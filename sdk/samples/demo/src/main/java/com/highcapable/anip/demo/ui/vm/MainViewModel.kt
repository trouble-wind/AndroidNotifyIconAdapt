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
import androidx.lifecycle.viewModelScope
import com.highcapable.anip.sdk.Anip
import com.highcapable.anip.sdk.entity.NotificationIcon
import com.highcapable.anip.sdk.entity.NotificationIconSnapshot
import com.highcapable.anip.sdk.type.IconCategory
import com.highcapable.anip.sdk.type.SystemVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    private val mutableUiState = MutableStateFlow(MainUiState())
    private val mutableUiEffects = MutableSharedFlow<MainUiEffect>(extraBufferCapacity = 1)

    private var renderJob: Job? = null
    private var renderRevision = 0L
    private var nextScrollRequestId = 0L

    val uiState = mutableUiState.asStateFlow()
    val uiEffects = mutableUiEffects.asSharedFlow()

    fun refresh() {
        val state = mutableUiState.value
        if (!state.isRefreshing) mutableUiEffects.tryEmit(MainUiEffect.Refresh(state.sourceSettings))
    }

    fun selectIconCategory(iconCategory: IconCategory) {
        if (mutableUiState.value.iconCategory != iconCategory)
            mutableUiEffects.tryEmit(MainUiEffect.ChangeIconCategory(iconCategory))
    }

    fun selectSystemVariant(systemVariant: SystemVariant?) {
        val state = mutableUiState.value
        if (state.systemVariant == systemVariant) return

        mutableUiState.update { it.copy(systemVariant = systemVariant) }
        mutableUiEffects.tryEmit(MainUiEffect.ChangeSystemVariant(systemVariant, state.sourceSettings))
    }

    fun showTestNotification(icon: NotificationIcon) {
        val bitmap = mutableUiState.value.snapshot?.getBitmap(icon) ?: return
        mutableUiEffects.tryEmit(MainUiEffect.ShowTestNotification(icon, bitmap))
    }

    fun copyPackageName(packageName: String) {
        mutableUiEffects.tryEmit(MainUiEffect.CopyPackageName(packageName))
    }

    fun updateSettings(sourceSettings: SettingsUiState.SourceSettings) {
        mutableUiState.update {
            if (it.sourceSettings == sourceSettings) it else it.copy(sourceSettings = sourceSettings)
        }
    }

    fun updateQuery(query: String) {
        val state = mutableUiState.value
        if (state.query != query) scheduleRender(state.copy(query = query))
    }

    fun toggleSortDirection() {
        val state = mutableUiState.value
        scheduleRender(
            state = state.copy(sortDescending = !state.sortDescending),
            scrollRequest = createScrollRequest(smooth = false)
        )
    }

    fun updateSortField(sortField: MainUiState.SortField) {
        val state = mutableUiState.value
        if (state.sortField != sortField) scheduleRender(
            state = state.copy(sortField = sortField),
            scrollRequest = createScrollRequest(smooth = false)
        )
    }

    fun updateIconCategory(iconCategory: IconCategory, snapshot: NotificationIconSnapshot) {
        if (mutableUiState.value.iconCategory == iconCategory) return

        scheduleRender(
            state = mutableUiState.value.copy(iconCategory = iconCategory, snapshot = snapshot),
            scrollRequest = createScrollRequest(smooth = false)
        )
    }

    fun beginFetch(sourceSettings: SettingsUiState.SourceSettings) {
        mutableUiState.update { it.copy(sourceSettings = sourceSettings, isRefreshing = true) }
    }

    fun updateSnapshot(snapshot: NotificationIconSnapshot) {
        scheduleRender(mutableUiState.value.copy(snapshot = snapshot))
    }

    fun finishFetch(
        snapshot: NotificationIconSnapshot?,
        status: Anip.FetchResult.Status,
        smoothScrollToTop: Boolean
    ) {
        val currentState = mutableUiState.value
        val state = currentState.copy(
            snapshot = snapshot ?: currentState.snapshot,
            fetchStatus = status,
            isRefreshing = false
        )
        if (snapshot == null) mutableUiState.value = state
        else scheduleRender(
            state = state,
            scrollRequest = if (smoothScrollToTop) createScrollRequest(smooth = true) else null
        )
    }

    fun consumeScrollRequest(id: Long) {
        mutableUiState.update {
            if (it.scrollRequest?.id == id) it.copy(scrollRequest = null) else it
        }
    }

    private fun createScrollRequest(smooth: Boolean) = MainUiState.ScrollRequest(++nextScrollRequestId, smooth)

    private fun scheduleRender(state: MainUiState, scrollRequest: MainUiState.ScrollRequest? = null) {
        val revision = ++renderRevision
        mutableUiState.value = state
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val icons = withContext(Dispatchers.Default) { render(state) }
            if (revision != renderRevision) return@launch

            mutableUiState.update {
                it.copy(icons = icons, scrollRequest = scrollRequest ?: it.scrollRequest)
            }
        }
    }

    private fun render(state: MainUiState): List<NotificationIcon> {
        val snapshot = state.snapshot ?: return emptyList()
        val sourceIcons = snapshot.icons
        val query = state.query.trim()
        val filteredIcons = if (query.isEmpty()) sourceIcons else sourceIcons.filter { icon ->
            sequenceOf(icon.packageName, icon.label)
                .plus(icon.availableLabels.values.asSequence())
                .plus(icon.contributors.asSequence())
                .any { value -> value.contains(query, ignoreCase = true) }
        }
        val nameComparator = if (state.sortDescending)
            compareByDescending<NotificationIcon> { it.label }.thenByDescending { it.packageName }
        else compareBy<NotificationIcon> { it.label }.thenBy { it.packageName }

        return when (state.sortField) {
            MainUiState.SortField.NAME -> filteredIcons.sortedWith(nameComparator)
            MainUiState.SortField.ADDED -> if (state.sortDescending) filteredIcons.reversed() else filteredIcons
        }
    }
}