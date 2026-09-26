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
 * This file is created by fankes on 2026/9/3.
 */
package com.highcapable.anip.sdk.entity

import android.graphics.Bitmap

/**
 * Immutable in-memory snapshot of a selected ANIP icon catalog.
 *
 * Every icon in [icons] has a successfully loaded bitmap. The snapshot strongly retains those bitmaps until the
 * snapshot itself becomes unreachable.
 * @param icons icons whose bitmaps were loaded successfully.
 * @param memorySizeBytes bitmap allocation size with shared bitmap instances counted once.
 */
class NotificationIconSnapshot internal constructor(
    val icons: List<NotificationIcon>,
    val memorySizeBytes: Long,
    private val iconsByPackage: Map<String, NotificationIcon>,
    private val bitmapsByPackage: Map<String, Bitmap>
) {

    /**
     * Returns the snapshot icon associated with [packageName], or null when unavailable.
     * @param packageName the package name whose icon is requested.
     * @return [NotificationIcon] or null.
     */
    fun getIcon(packageName: String) = iconsByPackage[packageName]

    /**
     * Returns the snapshot bitmap associated with [packageName], or null when unavailable.
     * @param packageName the package name whose bitmap is requested.
     * @return [Bitmap] or null.
     */
    fun getBitmap(packageName: String) = bitmapsByPackage[packageName]

    /**
     * Returns the snapshot bitmap associated with [icon], or null when unavailable.
     * @param icon the icon whose bitmap is requested.
     * @return [Bitmap] or null.
     */
    fun getBitmap(icon: NotificationIcon) = bitmapsByPackage[icon.packageName]
}