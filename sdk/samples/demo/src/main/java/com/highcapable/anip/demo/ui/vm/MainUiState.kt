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
package com.highcapable.anip.demo.ui.vm

import com.highcapable.anip.sdk.Anip
import com.highcapable.anip.sdk.entity.NotificationIcon
import com.highcapable.anip.sdk.entity.NotificationIconSnapshot
import com.highcapable.anip.sdk.type.IconCategory
import com.highcapable.anip.sdk.type.SystemVariant

data class MainUiState(
    val sourceSettings: SettingsUiState.SourceSettings = SettingsUiState.SourceSettings(),
    val iconCategory: IconCategory = IconCategory.ALL,
    val systemVariant: SystemVariant? = null,
    val query: String = "",
    val sortField: SortField = SortField.NAME,
    val sortDescending: Boolean = false,
    val snapshot: NotificationIconSnapshot? = null,
    val icons: List<NotificationIcon> = emptyList(),
    val fetchStatus: Anip.FetchResult.Status? = null,
    val isRefreshing: Boolean = false,
    val scrollRequest: ScrollRequest? = null
) {

    enum class SortField { NAME, ADDED }

    data class ScrollRequest(val id: Long, val smooth: Boolean)
}