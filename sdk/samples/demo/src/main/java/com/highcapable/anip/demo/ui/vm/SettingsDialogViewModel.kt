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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsDialogViewModel : ViewModel() {

    private val mutableUiState = MutableStateFlow(SettingsUiState())
    private val mutableUiEffects = MutableSharedFlow<SettingsDialogUiEffect>(extraBufferCapacity = 1)

    val uiState = mutableUiState.asStateFlow()
    val uiEffects = mutableUiEffects.asSharedFlow()

    fun open(sourceSettings: SettingsUiState.SourceSettings) {
        mutableUiState.value = SettingsUiState(
            isVisible = true,
            sourceSettings = sourceSettings
        )
    }

    fun dismiss() {
        mutableUiState.update {
            if (it.isVisible) it.copy(isVisible = false) else it
        }
    }

    fun selectSourceTemplate(sourceTemplate: SettingsUiState.SourceTemplate) {
        mutableUiState.update {
            if (it.sourceSettings.template == sourceTemplate && !it.hasSourceError) it
            else it.copy(
                sourceSettings = SettingsUiState.SourceSettings(sourceTemplate, sourceTemplate.defaultValue),
                hasSourceError = false
            )
        }
    }

    fun updateSourceValue(value: String) {
        mutableUiState.update {
            if (it.sourceSettings.value == value && !it.hasSourceError) it
            else it.copy(
                sourceSettings = it.sourceSettings.copy(value = value),
                hasSourceError = false
            )
        }
    }

    fun restoreDefaultSourceValue() {
        val template = mutableUiState.value.sourceSettings.template
        updateSourceValue(template.defaultValue)
    }

    fun save() {
        val state = mutableUiState.value
        val sourceSettings = state.sourceSettings.copy(value = state.sourceSettings.value.trim())
        val hasSourceError = runCatching(sourceSettings::createSource).isFailure

        if (hasSourceError) {
            mutableUiState.value = state.copy(hasSourceError = true)
            return
        }

        mutableUiState.value = state.copy(
            isVisible = false,
            sourceSettings = sourceSettings,
            hasSourceError = false
        )
        mutableUiEffects.tryEmit(SettingsDialogUiEffect.ApplySettings(sourceSettings))
    }
}