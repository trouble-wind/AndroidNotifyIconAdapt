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
package com.highcapable.anip.demo.ui.layout

import android.text.InputType
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.highcapable.anip.demo.R
import com.highcapable.anip.demo.ui.component.DemoPopupMenu
import com.highcapable.anip.demo.ui.vm.SettingsDialogViewModel
import com.highcapable.anip.demo.ui.vm.SettingsUiState
import com.highcapable.betterandroid.ui.extension.view.textToString
import com.highcapable.betterandroid.ui.extension.view.updatePadding
import com.highcapable.betterandroid.ui.extension.view.updateText
import com.highcapable.hikage.core.base.Hikagable
import com.highcapable.hikage.core.builder.HikageBuilder
import com.highcapable.hikage.core.layout.LayoutParams
import com.highcapable.hikage.runtime.lifecycle.setState
import com.highcapable.hikage.widget.android.widget.LinearLayout
import com.highcapable.hikage.widget.com.google.android.material.textfield.TextInputEditText
import com.highcapable.hikage.widget.com.google.android.material.textfield.TextInputLayout
import com.google.android.material.R as Material_R

class SettingsDialogLayout(viewModelProvider: () -> SettingsDialogViewModel) : HikageBuilder {

    private val viewModel by lazy(viewModelProvider)
    private val sourceTemplateItems = SettingsUiState.SourceTemplate.entries.map { it.labelResource to it }

    override fun build() = Hikagable {
        LinearLayout(
            lparams = LayoutParams(widthMatchParent = true),
            init = {
                orientation = LinearLayout.VERTICAL
                updatePadding(horizontal = 24.dp)
                updatePadding(top = 16.dp)
            }
        ) {
            TextInputLayout(
                lparams = LayoutParams(widthMatchParent = true),
                init = {
                    hint = stringResource(R.string.settings_source_template)
                    endIconContentDescription = stringResource(R.string.settings_source_template)
                    endIconMode = TextInputLayout.END_ICON_CUSTOM

                    setEndIconDrawable(Material_R.drawable.mtrl_dropdown_arrow)
                    setEndIconOnClickListener {
                        (editText as? TextInputEditText)?.showSourceTemplateMenu()
                    }
                }
            ) {
                TextInputEditText(
                    lparams = LayoutParams(widthMatchParent = true)
                ) {
                    inputType = InputType.TYPE_NULL
                    isSingleLine = true

                    setOnClickListener { showSourceTemplateMenu() }
                    setOnTouchListener { view, event ->
                        if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener false

                        view.performClick()
                        true
                    }
                    setState(viewModel.uiState) { state ->
                        updateValue(stringResource(state.sourceSettings.template.labelResource))
                    }
                }
            }
            TextInputLayout(
                lparams = LayoutParams(widthMatchParent = true) {
                    updateMargins(top = 8.dp)
                },
                init = {
                    endIconContentDescription = stringResource(R.string.restore_default_source)
                    endIconMode = TextInputLayout.END_ICON_CUSTOM

                    setEndIconDrawable(R.drawable.ic_restore)
                    setEndIconOnClickListener { viewModel.restoreDefaultSourceValue() }

                    setState(viewModel.uiState) { state ->
                        hint = stringResource(state.sourceSettings.template.inputHintResource)
                        isEndIconVisible = state.sourceSettings.template == SettingsUiState.SourceTemplate.GITHUB
                        error = if (state.hasSourceError)
                            stringResource(R.string.settings_source_invalid)
                        else null
                    }
                }
            ) {
                TextInputEditText(
                    id = "settings_source_value_input",
                    lparams = LayoutParams(widthMatchParent = true)
                ) {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                    isSingleLine = true

                    doAfterTextChanged { viewModel.updateSourceValue(it?.toString().orEmpty()) }
                    setState(viewModel.uiState) { state -> updateValue(state.sourceSettings.value) }
                }
            }
        }
    }

    private val SettingsUiState.SourceTemplate.labelResource get() = when (this) {
        SettingsUiState.SourceTemplate.GITHUB -> R.string.settings_source_github
        SettingsUiState.SourceTemplate.GITLAB -> R.string.settings_source_gitlab
        SettingsUiState.SourceTemplate.STATIC -> R.string.settings_source_static
        SettingsUiState.SourceTemplate.CUSTOM -> R.string.settings_source_custom
    }

    private val SettingsUiState.SourceTemplate.inputHintResource get() = when (this) {
        SettingsUiState.SourceTemplate.GITHUB -> R.string.settings_source_github_repository
        SettingsUiState.SourceTemplate.GITLAB -> R.string.settings_source_gitlab_project
        SettingsUiState.SourceTemplate.STATIC -> R.string.settings_source_static_base_url
        SettingsUiState.SourceTemplate.CUSTOM -> R.string.settings_source_custom_manifest_url
    }

    private fun TextInputEditText.showSourceTemplateMenu() {
        requestFocus()
        post {
            DemoPopupMenu(
                context = context,
                items = sourceTemplateItems,
                selectedItem = viewModel.uiState.value.sourceSettings.template,
                widthMatchedToAnchor = true,
                onItemSelected = viewModel::selectSourceTemplate
            ).show(this)
        }
    }

    private fun TextInputEditText.updateValue(value: String) {
        if (textToString() != value) updateText(value)
    }
}