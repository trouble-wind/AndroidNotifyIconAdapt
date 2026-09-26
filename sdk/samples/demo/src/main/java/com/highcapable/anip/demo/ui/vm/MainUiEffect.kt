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

import android.graphics.Bitmap
import com.highcapable.anip.sdk.entity.NotificationIcon
import com.highcapable.anip.sdk.type.IconCategory
import com.highcapable.anip.sdk.type.SystemVariant

sealed interface MainUiEffect {

    data class Refresh(val sourceSettings: SettingsUiState.SourceSettings) : MainUiEffect

    data class ChangeIconCategory(val iconCategory: IconCategory) : MainUiEffect

    data class ChangeSystemVariant(
        val systemVariant: SystemVariant?,
        val sourceSettings: SettingsUiState.SourceSettings
    ) : MainUiEffect

    data class ShowTestNotification(val icon: NotificationIcon, val bitmap: Bitmap) : MainUiEffect

    data class CopyPackageName(val packageName: String) : MainUiEffect
}