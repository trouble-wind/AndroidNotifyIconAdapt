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
 * This file is created by fankes on 2026/9/2.
 */
package com.highcapable.anip.sdk

import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Serves optional release metadata and a release bundle from loopback URLs.
 * @param bundleArchive the archive returned for the release attachment request.
 * @param releaseManifest optional static release manifest returned before the bundle.
 */
internal class TestDownloadServer(
    private val bundleArchive: ByteArray,
    private val releaseManifest: String? = null
) : Closeable {

    companion object {

        const val REPOSITORY = "test-owner/test-repository"
        const val TAG = "abc1234"
        const val ASSET_NAME = "anip-bundle-$TAG.zip"
        const val SOURCE_URL = "https://github.com/$REPOSITORY/releases/download/$TAG/$ASSET_NAME"
        const val MANIFEST_SOURCE_URL = "https://github.com/$REPOSITORY/releases/latest/download/anip-release.json"
        const val MANIFEST_REQUEST_TARGET = "/anip-release.json"
        const val DOWNLOAD_REQUEST_TARGET = "/resolved"
        const val RELATIVE_DOWNLOAD_REQUEST_TARGET = "/$ASSET_NAME"

        private const val REQUEST_TIMEOUT_MILLIS = 10_000
    }

    private val expectedRequestCount = if (releaseManifest == null) 1 else 2
    private val serverSocket = ServerSocket(0, expectedRequestCount, InetAddress.getByName("127.0.0.1")).apply {
        soTimeout = REQUEST_TIMEOUT_MILLIS
    }
    private val requests = mutableListOf<Pair<String, String?>>()

    @Volatile
    private var serverFailure: Throwable? = null

    private val serverThread = thread(name = "anip-test-download") {
        runCatching {
            repeat(expectedRequestCount) {
                serverSocket.accept().use(::respond)
            }
        }.onFailure {
            if (!serverSocket.isClosed) serverFailure = it
        }
    }

    /** Final URL returned by the resolver for this test. */
    val url = "http://127.0.0.1:${serverSocket.localPort}$DOWNLOAD_REQUEST_TARGET"

    /** Final static-manifest URL returned by the resolver for this test. */
    val manifestUrl = "http://127.0.0.1:${serverSocket.localPort}$MANIFEST_REQUEST_TARGET"

    /** Returns the request target received by this server after the download completes. */
    fun awaitRequestTargets(): List<String> {
        serverThread.join()
        serverFailure?.let { throw AssertionError("Test download server failed.", it) }
        return requests.map { it.first }
    }

    /** Returns whether any download request exposed an authorization header. */
    fun hasAuthorizationHeader() = requests.any { it.second != null }

    /** Stops the loopback server and releases its worker thread. */
    override fun close() {
        serverSocket.close()
        serverThread.join()
    }

    /** Records and responds to one HTTP request. */
    private fun respond(socket: Socket) {
        val reader = socket.getInputStream().bufferedReader()
        val requestTarget = reader.readLine().orEmpty().split(' ').getOrNull(1).orEmpty()
        val authorization = generateSequence(reader::readLine)
            .takeWhile(String::isNotEmpty)
            .lastOrNull { it.startsWith("Authorization:", ignoreCase = true) }
        requests += requestTarget to authorization

        val isManifestRequest = requestTarget == MANIFEST_REQUEST_TARGET
        val responseBody = if (isManifestRequest)
            checkNotNull(releaseManifest).toByteArray()
        else bundleArchive
        val mediaType = if (isManifestRequest) "application/json" else "application/octet-stream"
        val responseHeaders = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $mediaType\r\n")
            append("Content-Length: ${responseBody.size}\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray()

        socket.getOutputStream().use { output ->
            output.write(responseHeaders)
            output.write(responseBody)
        }
    }
}