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
package com.highcapable.anip.sdk.internal.manifest.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * JSON representation of one package entry from an ANIP manifest.
 * @param label direct label text or an i18n label object.
 * @param target package whose icon resource should be inherited.
 * @param format direct resource format.
 * @param color optional notification color.
 * @param overlay optional overlay behavior.
 * @param contributors comma-separated contributor names.
 */
@Serializable
internal data class ManifestRule(
    val label: JsonElement? = null,
    val target: String? = null,
    val format: String? = null,
    val color: String? = null,
    val overlay: Boolean? = null,
    val contributors: String? = null
)