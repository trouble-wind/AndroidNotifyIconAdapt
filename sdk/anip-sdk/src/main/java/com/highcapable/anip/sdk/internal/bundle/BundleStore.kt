/*
 * ANIP - Provides standardized monochrome icon resources for apps and vendor systems that do not conform to the Android standard notification design.
 * Copyright (C) 2019 HighCapable
 * https://github.com/BetterAndroid/android-notification-icon-project
 *
 * Apache License Version 2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is created by fankes on 2026/8/28.
 */
package com.highcapable.anip.sdk.internal.bundle

import com.highcapable.anip.sdk.internal.exception.BundleException
import com.highcapable.anip.sdk.internal.icon.IconCatalog
import com.highcapable.anip.sdk.internal.manifest.ResourceManifestParser
import com.highcapable.anip.sdk.internal.utils.copyToLimited
import com.highcapable.anip.sdk.type.SystemVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Owns persistent ANIP bundle installation, validation, fallback, and cache retention.
 * @param cacheDirectory cache directory shared by one logical resource source at a time.
 * @param sourceIdentity stable identity of the configured resource source.
 * @param resourceManifestParser the parser used to validate installed and cached bundles.
 * @param maxExtractedSizeBytes the maximum total uncompressed bundle size.
 * @param maxArchiveEntries the maximum number of ZIP entries accepted from a bundle.
 * @param retainedReleaseCount the number of recently used release directories to retain.
 */
internal class BundleStore(
    private val cacheDirectory: File,
    private val sourceIdentity: String,
    private val resourceManifestParser: ResourceManifestParser,
    private val maxExtractedSizeBytes: Long,
    private val maxArchiveEntries: Int,
    private val retainedReleaseCount: Int
) {

    private companion object {
        const val SOURCE_FILE_NAME = "source"
        const val TIMESTAMP_FILE_NAME = "timestamp"
    }

    /**
     * Source commit timestamp of the last successfully installed release.
     *
     * The value is stored as decimal text in the source-bound cache and resolves to `0L` when absent or invalid.
     */
    var timestamp: Long
        get() = if (!isCurrentSource()) 0L else cacheDirectory.resolve(TIMESTAMP_FILE_NAME).runCatching {
            readText().trim().toLong()
        }.getOrDefault(0L)
        set(value) {
            prepareSource()

            val timestampFile = cacheDirectory.resolve(TIMESTAMP_FILE_NAME)
            val temporaryFile = File.createTempFile("timestamp-", ".tmp", cacheDirectory)
            try {
                temporaryFile.writeText(value.toString())

                if (timestampFile.exists() && !timestampFile.delete())
                    throw BundleException("Failed to replace the ANIP timestamp file.")
                if (!temporaryFile.renameTo(timestampFile))
                    throw BundleException("Failed to install the ANIP timestamp file.")
            } finally {
                temporaryFile.delete()
            }
        }

    /**
     * Returns a validated catalog for [bundleRelease], downloading and replacing its cache when required.
     *
     * Installation occurs through a staging directory. An existing release directory is restored if parsing the
     * replacement fails, so callers never observe a partially extracted bundle.
     * @param bundleRelease the validated release metadata.
     * @param systemVariant system resources to include during validation and parsing, or null to exclude them.
     * @param forceRefresh whether an existing directory for the same tag must be replaced.
     * @param downloadBundle the callback that streams the release archive into the provided file.
     * @return [IconCatalog]
     */
    suspend fun obtain(
        bundleRelease: BundleRelease,
        systemVariant: SystemVariant?,
        forceRefresh: Boolean,
        downloadBundle: suspend (File) -> Unit
    ) = withContext(Dispatchers.IO) {
        prepareSource()

        val releasesDirectory = cacheDirectory.resolve("releases").apply {
            if (!isDirectory && !mkdirs()) throw BundleException("Failed to create release cache directory: $absolutePath")
        }

        val releaseDirectory = releasesDirectory.resolve(bundleRelease.tag)
        if (releaseDirectory.exists() && !releaseDirectory.isDirectory && !releaseDirectory.delete())
            throw BundleException("Release cache path is not a directory: ${bundleRelease.tag}")

        if (!forceRefresh && releaseDirectory.isDirectory) {
            val cachedIconCatalog = runCatching {
                resourceManifestParser.parse(releaseDirectory, systemVariant)
            }.getOrNull()
            if (cachedIconCatalog != null) {
                releaseDirectory.setLastModified(System.currentTimeMillis())
                pruneReleases(releasesDirectory, releaseDirectory)

                return@withContext cachedIconCatalog
            }

            releaseDirectory.deleteRecursively()
        }

        val stagingDirectory = createStagingDirectory()
        try {
            val archiveFile = stagingDirectory.resolve(bundleRelease.assetName)
            val extractedDirectory = stagingDirectory.resolve("bundle").apply {
                if (!mkdirs()) throw BundleException("Failed to create extraction directory: $absolutePath")
            }

            downloadBundle(archiveFile)
            extract(archiveFile, extractedDirectory)
            resourceManifestParser.parse(extractedDirectory, systemVariant)

            val previousDirectory = stagingDirectory.resolve("previous")
            if (releaseDirectory.isDirectory && !releaseDirectory.renameTo(previousDirectory))
                throw BundleException("Failed to stage the previous release cache: ${bundleRelease.tag}")
            if (!extractedDirectory.renameTo(releaseDirectory)) {
                previousDirectory.renameTo(releaseDirectory)
                throw BundleException("Failed to install release cache: ${bundleRelease.tag}")
            }

            runCatching {
                resourceManifestParser.parse(releaseDirectory, systemVariant).also {
                    releaseDirectory.setLastModified(System.currentTimeMillis())
                    pruneReleases(releasesDirectory, releaseDirectory)
                }
            }.getOrElse {
                releaseDirectory.deleteRecursively()
                previousDirectory.renameTo(releaseDirectory)

                throw it
            }
        } finally {
            stagingDirectory.deleteRecursively()
        }
    }

    /**
     * Loads the most recently used valid cached catalog without performing network access.
     * @return [IconCatalog] or null.
     */
    fun loadCached(systemVariant: SystemVariant?): IconCatalog? {
        if (!isCurrentSource()) return null

        val releasesDirectory = cacheDirectory.resolve("releases")
        val candidates = releasesDirectory.listFiles { file -> file.isDirectory }
            ?.sortedByDescending(File::lastModified)
            .orEmpty()

        return candidates.firstNotNullOfOrNull { directory ->
            runCatching { resourceManifestParser.parse(directory, systemVariant) }.getOrNull()
        }
    }

    /** Ensures the configured cache path exists as a directory before writing bundle state. */
    private fun ensureCacheDirectory() {
        if (cacheDirectory.isFile)
            throw BundleException("Cache path is a file: ${cacheDirectory.absolutePath}")
        if (!cacheDirectory.isDirectory && !cacheDirectory.mkdirs())
            throw BundleException("Failed to create cache directory: ${cacheDirectory.absolutePath}")
    }

    /** Rebinds persistent cache state when the configured resource source changes. */
    private fun prepareSource() {
        ensureCacheDirectory()
        if (isCurrentSource()) return

        val releasesDirectory = cacheDirectory.resolve("releases")
        if (releasesDirectory.exists() && !releasesDirectory.deleteRecursively())
            throw BundleException("Failed to clear releases from the previous ANIP source.")

        val timestampFile = cacheDirectory.resolve(TIMESTAMP_FILE_NAME)
        if (timestampFile.exists() && !timestampFile.delete())
            throw BundleException("Failed to clear the timestamp from the previous ANIP source.")

        val sourceFile = cacheDirectory.resolve(SOURCE_FILE_NAME)
        val temporaryFile = File.createTempFile("source-", ".tmp", cacheDirectory)
        try {
            temporaryFile.writeText(sourceIdentity)

            if (sourceFile.exists() && !sourceFile.delete())
                throw BundleException("Failed to replace the ANIP source file.")
            if (!temporaryFile.renameTo(sourceFile))
                throw BundleException("Failed to install the ANIP source file.")
        } finally {
            temporaryFile.delete()
        }
    }

    /** Returns whether persistent cache state belongs to the configured resource source. */
    private fun isCurrentSource() = cacheDirectory.resolve(SOURCE_FILE_NAME).runCatching {
        readText().trim() == sourceIdentity
    }.getOrDefault(false)

    /** Creates a unique staging directory inside the cache for same-filesystem replacement. */
    private fun createStagingDirectory(): File {
        val temporaryFile = File.createTempFile("staging-", ".tmp", cacheDirectory)
        if (!temporaryFile.delete() || !temporaryFile.mkdir())
            throw BundleException("Failed to create staging directory: ${temporaryFile.absolutePath}")

        return temporaryFile
    }

    /** Extracts an archive while enforcing entry-count and total uncompressed-size limits. */
    private fun extract(archiveFile: File, targetDirectory: File) {
        val canonicalTarget = targetDirectory.canonicalFile
        var extractedSize = 0L

        ZipInputStream(archiveFile.inputStream().buffered()).use { archive ->
            generateSequence { archive.nextEntry }.forEachIndexed { index, entry ->
                if (index >= maxArchiveEntries) throw BundleException("Release archive contains too many entries.")

                val remainingSize = maxExtractedSizeBytes - extractedSize
                if (entry.size > remainingSize) throw BundleException("Release extracted content exceeds the size limit.")

                extractedSize += extractEntry(archive, entry, canonicalTarget, remainingSize)
                archive.closeEntry()
            }
        }
    }

    /**
     * Extracts one ZIP [entry] after rejecting malformed names and paths outside [targetDirectory].
     * @return [Long] the number of bytes written to disk for this entry.
     */
    private fun extractEntry(archive: ZipInputStream, entry: ZipEntry, targetDirectory: File, remainingSize: Long): Long {
        if (entry.name.isBlank() || entry.name.contains('\\')) throw BundleException("Invalid release archive entry: ${entry.name}")

        val outputFile = targetDirectory.resolve(entry.name).canonicalFile
        if (!outputFile.isInside(targetDirectory)) throw BundleException("Release archive entry escapes the target directory: ${entry.name}")

        if (entry.isDirectory) {
            if (!outputFile.isDirectory && !outputFile.mkdirs()) throw BundleException("Failed to create extracted directory: ${entry.name}")
            return 0
        }

        outputFile.parentFile?.let { parent ->
            if (!parent.isDirectory && !parent.mkdirs()) throw BundleException("Failed to create icon directory: ${entry.name}")
        }

        return outputFile.outputStream().buffered().use { output ->
            archive.copyToLimited(output, remainingSize)
        }
    }

    /** Removes old release directories while always retaining the currently selected release first. */
    private fun pruneReleases(releasesDirectory: File, currentReleaseDirectory: File) {
        val releaseOrder = compareByDescending<File> { it == currentReleaseDirectory }
            .thenByDescending(File::lastModified)

        releasesDirectory.listFiles { file -> file.isDirectory }
            ?.sortedWith(releaseOrder)
            ?.drop(retainedReleaseCount)
            ?.forEach(File::deleteRecursively)
    }

    /** Returns whether this canonical path is [directory] itself or one of its descendants. */
    private fun File.isInside(directory: File) = path == directory.path || path.startsWith(directory.path + File.separator)
}