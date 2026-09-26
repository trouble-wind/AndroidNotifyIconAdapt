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
package com.highcapable.anip.sdk.internal.icon

import java.util.Locale

/**
 * Normalized label declaration backed by either one default value or locale-specific translations.
 * @param defaultValue the non-localized label, or null for an i18n label map.
 * @param translations the locale-specific labels in manifest order.
 */
internal class LocalizedLabel(val defaultValue: String?, val translations: Map<Locale, String>) {

    /**
     * Resolves the best label for [locale].
     *
     * Resolution prefers an exact language tag, then language and country, then language, and finally the first
     * manifest translation. A non-localized [defaultValue] always wins.
     * @return [String]
     */
    fun resolve(locale: Locale = Locale.getDefault()): String {
        defaultValue?.let { return it }

        val entries = translations.entries
        entries.firstOrNull { it.key.toLanguageTag().equals(locale.toLanguageTag(), ignoreCase = true) }
            ?.value?.let { return it }

        entries.firstOrNull {
            it.key.language.equals(locale.language, ignoreCase = true) &&
                it.key.country.equals(locale.country, ignoreCase = true)
        }?.value?.let { return it }

        entries.firstOrNull {
            it.key.language.equals(locale.language, ignoreCase = true)
        }?.value?.let { return it }

        return translations.values.first()
    }
}