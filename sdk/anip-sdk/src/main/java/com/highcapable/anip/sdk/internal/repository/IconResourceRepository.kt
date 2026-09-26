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
 * This file is created by fankes on 2026/8/29.
 */
package com.highcapable.anip.sdk.internal.repository

import android.content.Context
import android.graphics.Bitmap
import com.highcapable.anip.sdk.Anip
import com.highcapable.anip.sdk.config.AnipConfig
import com.highcapable.anip.sdk.config.RemoteSource
import com.highcapable.anip.sdk.entity.NotificationIcon
import com.highcapable.anip.sdk.entity.NotificationIconSnapshot
import com.highcapable.anip.sdk.internal.bundle.BundleStore
import com.highcapable.anip.sdk.internal.cache.BitmapCache
import com.highcapable.anip.sdk.internal.icon.IconCatalog
import com.highcapable.anip.sdk.internal.manifest.ResourceManifestParser
import com.highcapable.anip.sdk.internal.source.ResourceReleaseClient
import com.highcapable.anip.sdk.type.IconCategory
import com.highcapable.anip.sdk.type.SystemVariant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.IdentityHashMap

/**
 * Coordinates remote release discovery, persistent bundles, and the instance-scoped icon catalog.
 * @param context the context used to obtain app resources and the default cache directory.
 * @param config the configuration.
 */
internal class IconResourceRepository(context: Context, private val config: AnipConfig) {

    private companion object {

        const val DEFAULT_CACHE_DIR_NAME = "anip-icon-resources"

        const val MEMORY_CACHE_SIZE_BYTES = 16 * 1024 * 1024
        const val MAX_BUNDLE_SIZE_BYTES = 32L * 1024 * 1024
        const val MAX_EXTRACTED_SIZE_BYTES = 64L * 1024 * 1024
        const val MAX_ARCHIVE_ENTRIES = 10_000

        const val RETAINED_RELEASE_COUNT = 2
    }

    private val appContext = context.applicationContext

    private val bitmapCache = BitmapCache(MEMORY_CACHE_SIZE_BYTES)
    private val resourceManifestParser = ResourceManifestParser(bitmapCache)
    private val operationMutex = Mutex()

    @Volatile
    private var catalogState: CatalogState? = null

    /** One loaded catalog and its canonical source manifest URL. */
    private data class CatalogState(val manifestUrl: String, val catalog: IconCatalog)

    /** Timestamp persisted for the latest release of the currently configured resource repository. */
    val timestamp get() = createBundleStore(config.source, config.cacheDirectory).timestamp

    /**
     * Synchronizes the latest resource release and replaces the in-memory catalog after validation.
     *
     * An unchanged release reuses its extracted bundle. A failed refresh restores the latest valid disk catalog while
     * preserving the failure in the returned result.
     * @return [Anip.FetchResult]
     */
    suspend fun fetch() = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val source = config.source.also(RemoteSource::validate)
            val systemVariant = config.systemVariant
            val cacheDirectory = config.cacheDirectory
            val releaseClient = createReleaseClient(source)
            val bundleStore = createBundleStore(source, cacheDirectory)
            val previousTimestamp = bundleStore.timestamp

            runCatching {
                val bundleRelease = releaseClient.fetchLatest()
                val iconCatalog = bundleStore.obtain(
                    bundleRelease = bundleRelease,
                    systemVariant = systemVariant,
                    forceRefresh = bundleRelease.timestamp != previousTimestamp
                ) { bundleFile ->
                    releaseClient.download(bundleRelease, bundleFile)
                }
                updateCatalog(source, iconCatalog)
                bundleStore.timestamp = bundleRelease.timestamp

                val isUpToDate = bundleRelease.timestamp == previousTimestamp
                val message = if (isUpToDate)
                    "ANIP resources are up to date."
                else "ANIP resources were updated."
                val status = if (isUpToDate) Anip.FetchResult.Status.UP_TO_DATE else Anip.FetchResult.Status.SUCCESS
                Anip.FetchResult(message, status)
            }.getOrElse {
                when (it) {
                    is CancellationException -> throw it
                    else -> {
                        updateCatalog(source, loadCachedCatalog(bundleStore, systemVariant))
                        Anip.FetchResult(
                            it.message ?: "Failed to fetch ANIP resources.",
                            Anip.FetchResult.Status.FAILED
                        )
                    }
                }
            }
        }
    }

    /**
     * Reloads the latest valid local catalog without network access or persistent cache mutations.
     *
     * A failed reload preserves the currently loaded catalog.
     * @return [Boolean] whether a valid cached catalog was loaded.
     */
    suspend fun reload() = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val source = config.source.also(RemoteSource::validate)
            val cacheDirectory = config.cacheDirectory
            val cachedCatalog = loadCachedCatalog(createBundleStore(source, cacheDirectory), config.systemVariant)
                ?: return@withLock false

            updateCatalog(source, cachedCatalog)
            true
        }
    }

    /**
     * Builds a complete snapshot from one catalog while preventing concurrent bundle replacement or cleanup.
     *
     * Icons are decoded sequentially on [Dispatchers.IO]. Icons sharing one resource file also share one bitmap even
     * if the instance cache evicts that bitmap before a later alias is visited.
     */
    suspend fun createSnapshot(category: IconCategory) = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val catalog = currentCatalog()
            val catalogIcons = catalog?.find(category).orEmpty()

            createSnapshot(catalogIcons)
        }
    }

    /** Returns the loaded icon for [packageName], or null when the catalog has no matching rule. */
    fun find(packageName: String) = currentCatalog()?.find(packageName)

    /** Returns loaded icons matching [category], or an empty list before a catalog is available. */
    fun find(category: IconCategory) = currentCatalog()?.find(category).orEmpty()

    /** Creates a release client from [source]. */
    private fun createReleaseClient(source: RemoteSource) = ResourceReleaseClient(
        source = source,
        maxBundleSizeBytes = MAX_BUNDLE_SIZE_BYTES
    )

    /** Creates a bundle store scoped to [source] and the currently configured cache root. */
    private fun createBundleStore(source: RemoteSource, cacheDirectory: File?) = run {
        source.validate()
        require(cacheDirectory?.isFile != true) { "ANIP cache directory must not be a file." }

        val cacheRoot = cacheDirectory ?: createDefaultCacheDirectory()
        BundleStore(
            cacheDirectory = cacheRoot,
            sourceIdentity = source.createIdentity(),
            resourceManifestParser = resourceManifestParser,
            maxExtractedSizeBytes = MAX_EXTRACTED_SIZE_BYTES,
            maxArchiveEntries = MAX_ARCHIVE_ENTRIES,
            retainedReleaseCount = RETAINED_RELEASE_COUNT
        )
    }

    /** Returns the application-private cache root used when no custom directory is configured. */
    private fun createDefaultCacheDirectory() = appContext.cacheDir.resolve(DEFAULT_CACHE_DIR_NAME)

    /** Returns the catalog only while the configured source still matches its origin. */
    private fun currentCatalog() = catalogState?.takeIf {
        it.manifestUrl == config.source.manifestUrl
    }?.catalog

    /** Atomically replaces the loaded catalog and drops LRU references when its logical source changes. */
    private fun updateCatalog(source: RemoteSource, catalog: IconCatalog?) {
        if (catalogState?.manifestUrl != source.manifestUrl) bitmapCache.clear()
        catalogState = catalog?.let { CatalogState(source.manifestUrl, it) }
    }

    /** Builds a snapshot by loading each distinct resource file at most once. */
    private suspend fun createSnapshot(catalogIcons: List<NotificationIcon>): NotificationIconSnapshot {
        val loadedIcons = mutableListOf<NotificationIcon>()
        val iconsByPackage = mutableMapOf<String, NotificationIcon>()
        val bitmapsByPackage = mutableMapOf<String, Bitmap>()
        val bitmapsByResource = mutableMapOf<File, Bitmap>()
        val uniqueBitmaps = IdentityHashMap<Bitmap, Unit>()
        var memorySizeBytes = 0L

        catalogIcons.forEach { icon ->
            val resourceFile = icon.bitmapResourceFile
            val bitmap = resourceFile?.let(bitmapsByResource::get) ?: icon.loadBitmap()?.also { loadedBitmap ->
                resourceFile?.let { bitmapsByResource[it] = loadedBitmap }
            } ?: return@forEach

            loadedIcons += icon
            iconsByPackage[icon.packageName] = icon
            bitmapsByPackage[icon.packageName] = bitmap
            if (uniqueBitmaps.put(bitmap, Unit) == null) memorySizeBytes += bitmap.allocationByteCount.toLong()
        }

        return NotificationIconSnapshot(
            icons = loadedIcons,
            memorySizeBytes = memorySizeBytes,
            iconsByPackage = iconsByPackage,
            bitmapsByPackage = bitmapsByPackage
        )
    }

    /** Loads the latest valid cached catalog while preserving coroutine cancellation. */
    private suspend fun loadCachedCatalog(bundleStore: BundleStore, systemVariant: SystemVariant?) = runCatching {
        withContext(Dispatchers.IO) { bundleStore.loadCached(systemVariant) }
    }.getOrElse {
        when (it) {
            is CancellationException -> throw it
            else -> null
        }
    }
}