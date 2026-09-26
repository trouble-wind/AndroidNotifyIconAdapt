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
package com.highcapable.anip.sdk.type

import com.highcapable.anip.sdk.internal.icon.RuleSource

/**
 * System resource variants supported by ANIP.
 * Vendor variants include common system resources automatically.
 */
enum class SystemVariant(internal val ruleSources: List<RuleSource>) {
    /** Common system resources only. */
    COMMON(listOf(RuleSource.SYSTEM_COMMON)),

    /** HyperOS and MIUI. */
    MIOS(listOf(RuleSource.SYSTEM_COMMON, RuleSource.SYSTEM_MIOS)),

    /** ColorOS. */
    COLOROS(listOf(RuleSource.SYSTEM_COMMON, RuleSource.SYSTEM_COLOROS))
}