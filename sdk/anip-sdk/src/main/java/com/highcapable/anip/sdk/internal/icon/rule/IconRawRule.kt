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
package com.highcapable.anip.sdk.internal.icon.rule

import com.highcapable.anip.sdk.internal.icon.IconFormat
import com.highcapable.anip.sdk.internal.icon.LocalizedLabel

/**
 * Parsed icon manifest rule before target inheritance is resolved.
 *
 * Nullable fields are intentionally preserved because a target rule may inherit them from another package.
 * @param label normalized direct or localized label.
 * @param target package whose resource and omitted properties should be inherited.
 * @param format direct resource format, absent for target rules.
 * @param color optional notification color override.
 * @param overlay optional overlay override.
 * @param contributors optional contributor override.
 */
internal data class IconRawRule(
    val label: LocalizedLabel?,
    val target: String?,
    val format: IconFormat?,
    val color: Int?,
    val overlay: Boolean?,
    val contributors: List<String>?
)