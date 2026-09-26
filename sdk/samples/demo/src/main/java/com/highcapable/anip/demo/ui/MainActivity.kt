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
 * This file is created by fankes on 2026/8/29.
 */
package com.highcapable.anip.demo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.anip.demo.R
import com.highcapable.anip.demo.ui.layout.MainLayout
import com.highcapable.anip.demo.ui.vm.MainUiEffect
import com.highcapable.anip.demo.ui.vm.MainViewModel
import com.highcapable.anip.demo.ui.vm.SettingsDialogUiEffect
import com.highcapable.anip.demo.ui.vm.SettingsDialogViewModel
import com.highcapable.anip.demo.ui.vm.SettingsUiState
import com.highcapable.anip.sdk.Anip
import com.highcapable.anip.sdk.config.AnipConfig
import com.highcapable.betterandroid.system.extension.component.clipboardManager
import com.highcapable.betterandroid.system.extension.component.copy
import com.highcapable.betterandroid.system.extension.utils.AndroidVersion
import com.highcapable.betterandroid.ui.component.activity.AppViewsActivity
import com.highcapable.betterandroid.ui.component.notification.factory.NotificationChannel
import com.highcapable.betterandroid.ui.component.notification.factory.createNotification
import com.highcapable.betterandroid.ui.extension.component.launch
import com.highcapable.betterandroid.ui.extension.view.hideIme
import com.highcapable.hikage.core.builder.lazyHikage
import com.highcapable.hikage.extension.setContentView
import com.highcapable.pangutext.android.factory.PanguTextFactory2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import android.R as Android_R

class MainActivity : AppViewsActivity() {

    private companion object {

        const val SETTINGS_PREFERENCES_NAME = "settings"
        const val SETTINGS_SOURCE_TEMPLATE_KEY = "source_template"
        const val SETTINGS_SOURCE_VALUE_KEY = "source_value"

        const val TEST_NOTIFICATION_CHANNEL_ID = "anip_test_notification"
    }

    private val viewModel by lazy { ViewModelProvider(this)[MainViewModel::class] }
    private val settingsDialogViewModel by lazy { ViewModelProvider(this)[SettingsDialogViewModel::class] }

    private val mainLayout by lazyHikage(
        MainLayout(
            viewModelProvider = { viewModel },
            settingsDialogViewModelProvider = { settingsDialogViewModel }
        )
    )

    private val anipConfig = AnipConfig()
    private val anip by lazy { Anip(this, anipConfig) }

    private val settingsPrefs by lazy { getSharedPreferences(SETTINGS_PREFERENCES_NAME, MODE_PRIVATE) }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        val notification = pendingNotification
        pendingNotification = null

        @Suppress("MissingPermission")
        if (isGranted && notification != null) postTestNotification(notification)
    }

    private var fetchJob: Job? = null
    private var loadRequestId = 0L
    private var pendingNotification: MainUiEffect.ShowTestNotification? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PanguTextFactory2.inject(this)
        restoreSettings()

        setContentView(mainLayout)
        observeUiEffects()
        loadSource(viewModel.uiState.value.sourceSettings)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) currentFocus?.let { focusedView ->
            val bounds = Rect()
            focusedView.getGlobalVisibleRect(bounds)
            if (!bounds.contains(event.rawX.toInt(), event.rawY.toInt())) {
                focusedView.clearFocus()
                focusedView.hideIme()
            }
        }

        return super.dispatchTouchEvent(event)
    }

    private fun observeUiEffects() {
        launch {
            viewModel.uiEffects.collect { effect ->
                when (effect) {
                    is MainUiEffect.Refresh -> loadSource(effect.sourceSettings)
                    is MainUiEffect.ChangeIconCategory -> viewModel.updateIconCategory(
                        effect.iconCategory,
                        anip.createSnapshot(effect.iconCategory)
                    )
                    is MainUiEffect.ChangeSystemVariant -> {
                        anipConfig.systemVariant = effect.systemVariant
                        loadSource(
                            selectedSourceSettings = effect.sourceSettings,
                            replaceSnapshot = true,
                            smoothScrollAfterFetch = true
                        )
                    }
                    is MainUiEffect.ShowTestNotification -> showTestNotificationDialog(effect)
                    is MainUiEffect.CopyPackageName -> clipboardManager.copy(effect.packageName)
                }
            }
        }
        launch {
            settingsDialogViewModel.uiEffects.collect { effect ->
                when (effect) {
                    is SettingsDialogUiEffect.ApplySettings -> {
                        viewModel.updateSettings(effect.sourceSettings)
                        applySettings(effect.sourceSettings)
                    }
                }
            }
        }
    }

    private fun showTestNotificationDialog(notification: MainUiEffect.ShowTestNotification) {
        MaterialAlertDialogBuilder(this)
            .setTitle(notification.icon.label)
            .setMessage(R.string.test_notification_prompt)
            .setPositiveButton(Android_R.string.ok) { _, _ -> requestTestNotification(notification) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun requestTestNotification(notification: MainUiEffect.ShowTestNotification) {
        if (AndroidVersion.isAtLeast(AndroidVersion.T) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotification = notification
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        postTestNotification(notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postTestNotification(notification: MainUiEffect.ShowTestNotification) {
        val icon = notification.icon
        val smallIcon = IconCompat.createWithBitmap(notification.bitmap)

        createNotification(
            channel = NotificationChannel(TEST_NOTIFICATION_CHANNEL_ID) {
                name = getString(R.string.test_notification_channel_name)
            }
        ) {
            AndroidVersion.require(AndroidVersion.M) {
                this.smallIcon = smallIcon
            }
            this.contentTitle = icon.label
            this.contentText = getString(R.string.test_notification_content)
            icon.color?.let { this.color = it }
        }.post(tag = icon.packageName)
    }

    private fun applySettings(sourceSettings: SettingsUiState.SourceSettings) {
        val source = sourceSettings.createSource()
        val sourceChanged = anipConfig.source != source
        settingsPrefs.edit {
            putString(SETTINGS_SOURCE_TEMPLATE_KEY, sourceSettings.template.name)
            putString(SETTINGS_SOURCE_VALUE_KEY, sourceSettings.value)
        }
        loadSource(sourceSettings, replaceSnapshot = sourceChanged)
    }

    private fun restoreSettings() {
        val sourceTemplateName = settingsPrefs.getString(SETTINGS_SOURCE_TEMPLATE_KEY, null)
        val sourceTemplate = SettingsUiState.SourceTemplate.entries.firstOrNull { it.name == sourceTemplateName }
            ?: SettingsUiState.SourceTemplate.GITHUB
        val sourceValue = settingsPrefs.getString(SETTINGS_SOURCE_VALUE_KEY, sourceTemplate.defaultValue)
            .orEmpty()
            .ifBlank { sourceTemplate.defaultValue }
        val restoredSourceSettings = SettingsUiState.SourceSettings(sourceTemplate, sourceValue)
            .takeIf { runCatching(it::createSource).isSuccess }
            ?: SettingsUiState.SourceSettings()

        anipConfig.source = restoredSourceSettings.createSource()
        anipConfig.systemVariant = viewModel.uiState.value.systemVariant
        viewModel.updateSettings(restoredSourceSettings)
    }

    private fun loadSource(
        selectedSourceSettings: SettingsUiState.SourceSettings,
        replaceSnapshot: Boolean = false,
        smoothScrollAfterFetch: Boolean = false
    ) {
        val requestId = ++loadRequestId
        val currentSnapshot = viewModel.uiState.value.snapshot
        anipConfig.source = selectedSourceSettings.createSource()

        fetchJob?.cancel()
        viewModel.beginFetch(selectedSourceSettings)
        fetchJob = launch {
            runCatching {
                if (requestId != loadRequestId) return@launch

                val shouldReload = currentSnapshot == null || replaceSnapshot
                val restoredSnapshot = if (shouldReload && anip.reload())
                    anip.createSnapshot(viewModel.uiState.value.iconCategory)
                else null
                if (requestId != loadRequestId) return@launch

                val shouldApplyRestoredSnapshotImmediately = currentSnapshot == null
                if (shouldApplyRestoredSnapshotImmediately) restoredSnapshot?.let(viewModel::updateSnapshot)

                val result = withContext(Dispatchers.IO) { anip.fetch() }
                if (requestId != loadRequestId) return@launch

                val fetchedSnapshot = if (result.status == Anip.FetchResult.Status.SUCCESS || shouldReload && restoredSnapshot == null)
                    anip.createSnapshot(viewModel.uiState.value.iconCategory)
                else null
                if (requestId != loadRequestId) return@launch

                val snapshot = fetchedSnapshot ?: restoredSnapshot.takeUnless { shouldApplyRestoredSnapshotImmediately }
                val shouldSmoothScroll = result.status == Anip.FetchResult.Status.SUCCESS ||
                    smoothScrollAfterFetch && result.status != Anip.FetchResult.Status.FAILED
                viewModel.finishFetch(
                    snapshot = snapshot,
                    status = result.status,
                    smoothScrollToTop = shouldSmoothScroll
                )
            }.onFailure {
                if (requestId != loadRequestId) return@onFailure

                viewModel.finishFetch(
                    snapshot = null,
                    status = Anip.FetchResult.Status.FAILED,
                    smoothScrollToTop = false
                )
            }
        }
    }
}