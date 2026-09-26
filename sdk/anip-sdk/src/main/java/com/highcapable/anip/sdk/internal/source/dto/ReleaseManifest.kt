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
 * This file is created by fankes on 2026/9/4.
 */
package com.highcapable.anip.sdk.internal.source.dto

import kotlinx.serialization.Serializable

/**
 * Metadata published as the fixed `anip-release.json` attachment of the latest release.
 * @param schemaVersion manifest protocol version.
 * @param tag release tag and abbreviated source commit SHA.
 * @param timestamp source commit timestamp in epoch milliseconds.
 * @param assetName release bundle attachment name.
 * @param downloadUrl absolute or manifest-relative resource bundle URL.
 * @param size expected bundle size in bytes.
 * @param sha256 expected lowercase SHA-256 digest of the bundle.
 */
@Serializable
internal data class ReleaseManifest(
    val schemaVersion: Int = 0,
    val tag: String? = null,
    val timestamp: Long = 0,
    val assetName: String? = null,
    val downloadUrl: String? = null,
    val size: Long = -1,
    val sha256: String? = null
)