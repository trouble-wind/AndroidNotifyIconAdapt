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
package com.highcapable.anip.sdk.internal.cache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.LruCache
import androidx.core.graphics.createBitmap
import com.caverock.androidsvg.SVG
import com.highcapable.anip.sdk.Anip
import com.highcapable.anip.sdk.internal.exception.BundleException
import com.highcapable.anip.sdk.internal.icon.IconFormat
import com.highcapable.betterandroid.ui.extension.graphics.decodeToBitmapOrNull
import java.io.File

/**
 * Per-[Anip] LRU cache that lazily decodes PNG and SVG icon files into bitmaps.
 * @param maxSizeBytes the maximum bitmap memory retained by this cache.
 */
internal class BitmapCache(maxSizeBytes: Int) {

    private companion object {
        const val MAX_ICON_DIMENSION = 150
    }

    private val cache = object : LruCache<String, Bitmap>(maxSizeBytes) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    /**
     * Loads [file] in [format], reusing a cached bitmap when its file metadata is unchanged.
     * @return [Bitmap] or null.
     */
    @Synchronized
    fun load(file: File, format: IconFormat): Bitmap? {
        val key = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
        cache.get(key)?.let { return it }

        return runCatching {
            when (format) {
                IconFormat.PNG -> decodePng(file)
                IconFormat.SVG -> decodeSvg(file)
            }
        }.getOrNull()?.also { cache.put(key, it) }
    }

    /** Removes all instance-scoped cached bitmap references without recycling them. */
    @Synchronized
    fun clear() = cache.evictAll()

    /** Decodes a dimension-checked PNG file into a bitmap. */
    private fun decodePng(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        file.decodeToBitmapOrNull(bounds)

        if (bounds.outWidth !in 1..MAX_ICON_DIMENSION || bounds.outHeight !in 1..MAX_ICON_DIMENSION)
            throw BundleException("Invalid PNG icon dimensions: ${file.name}")

        return file.decodeToBitmapOrNull() ?: throw BundleException("Failed to decode PNG icon: ${file.name}")
    }

    /** Parses and renders a dimension-checked SVG file into a bitmap. */
    private fun decodeSvg(file: File): Bitmap {
        val picture = file.inputStream().buffered().use(SVG::getFromInputStream).renderToPicture()
        if (picture.width !in 1..MAX_ICON_DIMENSION || picture.height !in 1..MAX_ICON_DIMENSION)
            throw BundleException("Invalid SVG icon dimensions: ${file.name}")

        return createBitmap(picture.width, picture.height).apply {
            Canvas(this).drawPicture(picture)
        }
    }
}