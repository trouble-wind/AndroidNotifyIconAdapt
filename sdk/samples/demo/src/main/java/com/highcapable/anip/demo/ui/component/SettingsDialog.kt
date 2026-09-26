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
package com.highcapable.anip.demo.ui.component

import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.highcapable.anip.demo.R
import com.highcapable.anip.demo.ui.layout.SettingsDialogLayout
import com.highcapable.anip.demo.ui.vm.SettingsDialogViewModel
import com.highcapable.anip.demo.ui.vm.SettingsUiState
import com.highcapable.hikage.extension.setView

class SettingsDialog(context: Context, viewModelProvider: () -> SettingsDialogViewModel) {

    private val viewModel by lazy(viewModelProvider)
    private val content = SettingsDialogLayout { viewModel }.build().create(context)
    private val sourceValueInput = content.get<TextInputEditText>("settings_source_value_input")

    private val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.settings)
        .setView(content)
        .setPositiveButton(R.string.save, null)
        .setNegativeButton(R.string.cancel) { _, _ -> viewModel.dismiss() }
        .create()

    init {
        dialog.setOnCancelListener { viewModel.dismiss() }
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { viewModel.save() }
            sourceValueInput.post {
                sourceValueInput.selectAll()
                sourceValueInput.requestFocus()
            }
        }
    }

    fun render(state: SettingsUiState) {
        if (!state.isVisible) {
            if (dialog.isShowing) dialog.dismiss()
            return
        }

        if (!dialog.isShowing) dialog.show()
    }
}