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
 * This file is created by fankes on 2026/8/30.
 */
package com.highcapable.anip.demo.ui.layout

import android.animation.ObjectAnimator
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.allViews
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMarginsRelative
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputLayout
import com.highcapable.anip.demo.R
import com.highcapable.anip.demo.ui.component.DemoPopupMenu
import com.highcapable.anip.demo.ui.component.SettingsDialog
import com.highcapable.anip.demo.ui.component.VerticalFastScroller
import com.highcapable.anip.demo.ui.vm.MainUiState
import com.highcapable.anip.demo.ui.vm.MainViewModel
import com.highcapable.anip.demo.ui.vm.SettingsDialogViewModel
import com.highcapable.anip.sdk.Anip
import com.highcapable.anip.sdk.entity.NotificationIcon
import com.highcapable.anip.sdk.type.IconCategory
import com.highcapable.anip.sdk.type.SystemVariant
import com.highcapable.betterandroid.ui.component.adapter.factory.bindAdapter
import com.highcapable.betterandroid.ui.component.adapter.recycler.cosmetic.RecyclerCosmetic
import com.highcapable.betterandroid.ui.component.adapter.recycler.factory.scrollToFirstPosition
import com.highcapable.betterandroid.ui.component.adapter.recycler.factory.smoothScrollToFirstPosition
import com.highcapable.betterandroid.ui.component.adapter.recycler.factory.wrapper
import com.highcapable.betterandroid.ui.extension.component.base.getThemeAttrsDrawable
import com.highcapable.betterandroid.ui.extension.component.base.toPx
import com.highcapable.betterandroid.ui.extension.graphics.AttrState
import com.highcapable.betterandroid.ui.extension.graphics.ColorStateList
import com.highcapable.betterandroid.ui.extension.view.hideIme
import com.highcapable.betterandroid.ui.extension.view.textColor
import com.highcapable.betterandroid.ui.extension.view.textToString
import com.highcapable.betterandroid.ui.extension.view.tooltipTextCompat
import com.highcapable.betterandroid.ui.extension.view.updateMargins
import com.highcapable.betterandroid.ui.extension.view.updatePadding
import com.highcapable.betterandroid.ui.extension.view.updateText
import com.highcapable.hikage.annotation.Hikagable
import com.highcapable.hikage.core.base.Hikagable
import com.highcapable.hikage.core.base.HikageView
import com.highcapable.hikage.core.builder.HikageBuilder
import com.highcapable.hikage.core.layout.LayoutParams
import com.highcapable.hikage.extension.betterandroid.ui.component.adapter.onBindItemView
import com.highcapable.hikage.runtime.lifecycle.setState
import com.highcapable.hikage.widget.android.widget.FrameLayout
import com.highcapable.hikage.widget.android.widget.ImageView
import com.highcapable.hikage.widget.android.widget.LinearLayout
import com.highcapable.hikage.widget.android.widget.TextView
import com.highcapable.hikage.widget.androidx.appcompat.widget.AppCompatImageButton
import com.highcapable.hikage.widget.androidx.recyclerview.widget.RecyclerView
import com.highcapable.hikage.widget.com.google.android.material.appbar.MaterialToolbar
import com.highcapable.hikage.widget.com.google.android.material.textfield.TextInputEditText
import com.highcapable.hikage.widget.com.google.android.material.textfield.TextInputLayout
import android.R as Android_R
import com.google.android.material.R as Material_R

class MainLayout(viewModelProvider: () -> MainViewModel, settingsDialogViewModelProvider: () -> SettingsDialogViewModel) : HikageBuilder {

    private companion object {
        const val ACTION_REFRESH = 1
        const val ACTION_ICON_CATEGORY = 2
        const val ACTION_SYSTEM_VARIANT = 3
        const val ACTION_SETTINGS = 4
        const val TOOLBAR_ACTION_WIDTH_DP = 44
        const val CONTROL_SIZE_DP = 48
        const val SORT_BUTTON_SIZE_DP = 24
        const val LIST_HORIZONTAL_PADDING_DP = 12
        const val GRID_ITEM_MIN_WIDTH_DP = 280
        const val GRID_SPACING_DP = 10
        const val REFRESH_ROTATION_DURATION = 800L
    }

    private val viewModel by lazy(viewModelProvider)
    private val settingsDialogViewModel by lazy(settingsDialogViewModelProvider)

    private var refreshAnimator: ObjectAnimator? = null

    override fun build() = Hikagable {
        LinearLayout(
            lparams = LayoutParams(matchParent = true),
            init = {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(MaterialColors.getColor(this, Material_R.attr.colorSurfaceContainerLow))

                val dialog = SettingsDialog(
                    context = context,
                    viewModelProvider = { settingsDialogViewModel }
                )
                setState(settingsDialogViewModel.uiState) { state -> dialog.render(state) }
            }
        ) {
            MaterialToolbar(
                lparams = LayoutParams(widthMatchParent = true)
            ) {
                title = stringResource(R.string.app_name)
                updatePaddingRelative(end = 10.dp)

                configureToolbar()
                setState(viewModel.uiState) { state ->
                    updateRefreshAnimation(state.isRefreshing)
                }
            }

            LinearLayout(
                lparams = LayoutParams(widthMatchParent = true) {
                    topMargin = 5.dp
                    updateMargins(horizontal = 12.dp)
                },
                init = {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
            ) {
                TextInputLayout(
                    lparams = LayoutParams(widthMatchParent = true, height = CONTROL_SIZE_DP.dp),
                    init = {
                        val activeBorderColor = MaterialColors.getColor(this, Material_R.attr.colorPrimary)
                        val borderColor = MaterialColors.getColor(this, Material_R.attr.colorOutlineVariant)

                        boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                        boxBackgroundColor = Color.TRANSPARENT
                        endIconContentDescription = stringResource(R.string.clear_search)
                        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                        isHintEnabled = false

                        setBoxStrokeColorStateList(
                            ColorStateList(
                                AttrState.FOCUSED to activeBorderColor,
                                AttrState.NORMAL to borderColor
                            )
                        )
                        setBoxCornerRadii(
                            10.dp.toFloat(),
                            10.dp.toFloat(),
                            10.dp.toFloat(),
                            10.dp.toFloat()
                        )
                    }
                ) {
                    TextInputEditText(
                        lparams = LayoutParams(matchParent = true)
                    ) {
                        compoundDrawablePadding = 4.dp
                        gravity = Gravity.CENTER_VERTICAL or Gravity.START
                        hint = stringResource(R.string.search_hint)
                        isSingleLine = true

                        setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0)
                        TextViewCompat.setCompoundDrawableTintList(
                            this, ColorStateList(
                                AttrState.NORMAL to MaterialColors.getColor(this, Material_R.attr.colorOnSurfaceVariant)
                            )
                        )
                        updatePadding(vertical = 0)
                        updatePaddingRelative(start = 12.dp)

                        doAfterTextChanged { viewModel.updateQuery(it?.toString().orEmpty()) }
                        setOnEditorActionListener { _, actionId, _ ->
                            if (actionId != EditorInfo.IME_ACTION_DONE) return@setOnEditorActionListener false

                            hideIme()
                            clearFocus()
                            true
                        }

                        setState(viewModel.uiState) { state ->
                            if (textToString() != state.query) {
                                updateText(state.query)
                                setSelection(length())
                            }
                        }
                    }
                }
            }

            LinearLayout(
                lparams = LayoutParams(widthMatchParent = true) {
                    updateMargins(horizontal = 16.dp, vertical = 12.dp)
                },
                init = {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
            ) {
                LinearLayout(
                    lparams = LayoutParams(width = 0) {
                        weight = 1f
                    },
                    init = {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                ) {
                    TextView {
                        ellipsize = TextUtils.TruncateAt.END
                        maxLines = 1
                        textColor = MaterialColors.getColor(this, Material_R.attr.colorOnSurfaceVariant)
                        textSize = 13f

                        setState(viewModel.uiState) { state ->
                            if (state.icons.isEmpty()) setText(
                                if (state.query.trim().isEmpty())
                                    R.string.no_icon_resources
                                else R.string.no_matching_icon_resources
                            ) else text = pluralStringResource(
                                R.plurals.icon_resources_count,
                                state.icons.size,
                                state.icons.size
                            )
                        }
                    }
                    ImageView(
                        lparams = LayoutParams(width = 16.dp, height = 16.dp) {
                            updateMarginsRelative(start = 5.dp)
                        }
                    ) {
                        isVisible = false
                        imageTintList = ColorStateList(
                            AttrState.NORMAL to MaterialColors.getColor(this, Material_R.attr.colorPrimary)
                        )
                        setImageResource(R.drawable.ic_check)

                        setState(viewModel.uiState) { state ->
                            state.fetchStatus?.let { status ->
                                val isFailed = status == Anip.FetchResult.Status.FAILED
                                val textResource = when (status) {
                                    Anip.FetchResult.Status.SUCCESS -> R.string.fetch_status_updated
                                    Anip.FetchResult.Status.UP_TO_DATE -> R.string.fetch_status_up_to_date
                                    Anip.FetchResult.Status.FAILED -> R.string.fetch_status_failed
                                }
                                val iconResource = if (isFailed) R.drawable.ic_close else R.drawable.ic_check
                                val colorAttribute = if (isFailed) Material_R.attr.colorError else Material_R.attr.colorPrimary

                                isVisible = true
                                contentDescription = stringResource(textResource)
                                imageTintList = ColorStateList(
                                    AttrState.NORMAL to MaterialColors.getColor(this, colorAttribute)
                                )
                                setImageResource(iconResource)
                            } ?: run { isVisible = false }
                        }
                    }
                }

                @Hikagable
                fun ActionButton(
                    id: String? = null,
                    icon: Int,
                    description: Int,
                    tooltip: Int,
                    marginStart: Int = 0,
                    init: HikageView<AppCompatImageButton> = {}
                ) = AppCompatImageButton(
                    id = id,
                    lparams = LayoutParams(width = SORT_BUTTON_SIZE_DP.dp, height = SORT_BUTTON_SIZE_DP.dp) {
                        updateMarginsRelative(start = marginStart)
                    }
                ) {
                    val iconColor = MaterialColors.getColor(this, Material_R.attr.colorPrimary)

                    background = context.getThemeAttrsDrawable(Android_R.attr.selectableItemBackgroundBorderless)
                    contentDescription = stringResource(description)
                    tooltipTextCompat = stringResource(tooltip)

                    minimumHeight = 0
                    minimumWidth = 0

                    imageTintList = ColorStateList(AttrState.NORMAL to iconColor)
                    scaleType = ImageView.ScaleType.FIT_CENTER

                    setImageResource(icon)
                    setPadding(4.dp)

                    init()
                }

                ActionButton(
                    icon = R.drawable.ic_sort_ascending,
                    description = R.string.sort_direction,
                    tooltip = R.string.sort_direction
                ) {
                    setOnClickListener { viewModel.toggleSortDirection() }
                    setState(viewModel.uiState) { state ->
                        val directionText = if (state.sortDescending) R.string.sort_descending else R.string.sort_ascending
                        val directionIcon = if (state.sortDescending)
                            R.drawable.ic_sort_descending
                        else R.drawable.ic_sort_ascending

                        contentDescription = stringResource(directionText)
                        setImageResource(directionIcon)
                    }
                }
                ActionButton(
                    icon = R.drawable.ic_sort_two,
                    description = R.string.sort_method,
                    tooltip = R.string.sort_method,
                    marginStart = 6.dp
                ) {
                    setOnClickListener { anchor ->
                        val state = viewModel.uiState.value
                        DemoPopupMenu(
                            context = context,
                            items = listOf(
                                R.string.sort_by_name to MainUiState.SortField.NAME,
                                R.string.sort_by_added to MainUiState.SortField.ADDED
                            ),
                            selectedItem = state.sortField,
                            onItemSelected = viewModel::updateSortField
                        ).show(anchor)
                    }
                    setState(viewModel.uiState) { state ->
                        contentDescription = stringResource(
                            if (state.sortField == MainUiState.SortField.NAME) R.string.sort_by_name else R.string.sort_by_added
                        )
                    }
                }
            }

            FrameLayout(
                lparams = LayoutParams(widthMatchParent = true, height = 0) {
                    weight = 1f
                },
                init = {
                    updatePadding(horizontal = LIST_HORIZONTAL_PADDING_DP.dp)
                }
            ) {
                RecyclerView(
                    lparams = LayoutParams(matchParent = true)
                ) {
                    isVerticalFadingEdgeEnabled = true
                    setFadingEdgeLength(9.dp)

                    updatePadding(bottom = 3.dp)
                    VerticalFastScroller(
                        recyclerView = this,
                        thumbDrawableRes = R.drawable.fast_scroll_vertical_thumb,
                        trackDrawableRes = R.drawable.fast_scroll_vertical_track
                    )

                    bindIconAdapter()
                    var submittedIcons: List<NotificationIcon>? = null
                    setState(viewModel.uiState) { state ->
                        if (submittedIcons !== state.icons) {
                            submittedIcons = state.icons
                            val iconList = this
                            adapter?.wrapper?.submitList(state.icons) {
                                state.scrollRequest?.let { request ->
                                    if (request.smooth) iconList.smoothScrollToFirstPosition()
                                    else iconList.scrollToFirstPosition()
                                    viewModel.consumeScrollRequest(request.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun RecyclerView.bindIconAdapter() = bindAdapter<NotificationIcon>(
        cosmetic = createIconListCosmetic()
    ) {
        onBindDiffer(
            areItemsTheSame = { oldItem, newItem -> oldItem.packageName == newItem.packageName },
            areContentsTheSame = { oldItem, newItem -> oldItem === newItem }
        )
        onBindItemView(IconItemLayout.build()) { hikage, icon, _ ->
            hikage.get<MaterialCardView>("item_preview").setCardBackgroundColor(icon.color ?: IconItemLayout.DEFAULT_PREVIEW_COLOR)
            hikage.get<TextView>("item_label").text = icon.label
            hikage.get<TextView>("item_package").text = icon.packageName
            hikage.get<AppCompatImageButton>("item_copy_package").setOnClickListener {
                viewModel.copyPackageName(icon.packageName)
            }
            hikage.get<TextView>("item_overlay").isVisible = icon.overlay
            hikage.get<TextView>("item_contributors").apply {
                isVisible = icon.contributors.isNotEmpty()
                text = context.getString(R.string.contributors, icon.contributors.joinToString())
            }
            hikage.get<ImageView>("item_icon").setImageBitmap(viewModel.uiState.value.snapshot?.getBitmap(icon))
        }
        onItemViewClick { _, icon, _ -> viewModel.showTestNotification(icon) }
    }

    private fun RecyclerView.createIconListCosmetic(): RecyclerCosmetic<*, *> {
        val availableWidthDp = resources.configuration.screenWidthDp - LIST_HORIZONTAL_PADDING_DP * 2
        val spanCount = (availableWidthDp / GRID_ITEM_MIN_WIDTH_DP).coerceAtLeast(1)
        val spacing = GRID_SPACING_DP.toPx(context)

        return RecyclerCosmetic.fromGridVertical(context, spanCount, spacing, spacing)
    }

    private fun MaterialToolbar.configureToolbar() {
        val iconColor = MaterialColors.getColor(this, Material_R.attr.colorOnSurface)

        addAction(ACTION_REFRESH, 0, R.string.refresh, R.drawable.ic_refresh, iconColor)
        addAction(ACTION_ICON_CATEGORY, 1, R.string.icon_category, R.drawable.ic_filter_list, iconColor)
        addAction(ACTION_SYSTEM_VARIANT, 2, R.string.system_variant, R.drawable.ic_android, iconColor)
        addAction(ACTION_SETTINGS, 3, R.string.settings, R.drawable.ic_settings, iconColor)

        setOnMenuItemClickListener { item ->
            when (item.itemId) {
                ACTION_REFRESH -> viewModel.refresh()
                ACTION_ICON_CATEGORY -> showIconCategoryMenu()
                ACTION_SYSTEM_VARIANT -> showSystemVariantMenu()
                ACTION_SETTINGS -> viewModel.uiState.value.let {
                    settingsDialogViewModel.open(it.sourceSettings)
                }
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        post {
            val actionWidth = TOOLBAR_ACTION_WIDTH_DP.toPx(context)
            listOf(ACTION_REFRESH, ACTION_ICON_CATEGORY, ACTION_SYSTEM_VARIANT, ACTION_SETTINGS)
                .mapNotNull { findActionView(it) }
                .forEach {
                    it.minimumWidth = actionWidth
                    it.updateLayoutParams { width = actionWidth }
                }
        }
    }

    private fun MaterialToolbar.addAction(id: Int, order: Int, title: Int, icon: Int, iconColor: Int) =
        menu.add(Menu.NONE, id, order, title).apply {
            setIcon(icon)
            this.icon?.setTint(iconColor)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

    private fun MaterialToolbar.showIconCategoryMenu() {
        val anchor = findActionView(ACTION_ICON_CATEGORY) ?: return
        val state = viewModel.uiState.value

        DemoPopupMenu(
            context = context,
            items = listOf(
                R.string.icon_category_all to IconCategory.ALL,
                R.string.icon_category_app to IconCategory.APP,
                R.string.icon_category_game to IconCategory.GAME,
                R.string.icon_category_system to IconCategory.SYSTEM
            ),
            selectedItem = state.iconCategory,
            onItemSelected = viewModel::selectIconCategory
        ).show(anchor)
    }

    private fun MaterialToolbar.showSystemVariantMenu() {
        val anchor = findActionView(ACTION_SYSTEM_VARIANT) ?: return
        val state = viewModel.uiState.value

        DemoPopupMenu(
            context = context,
            items = listOf(
                R.string.system_variant_unset to null,
                R.string.system_variant_common to SystemVariant.COMMON,
                R.string.system_variant_mios to SystemVariant.MIOS,
                R.string.system_variant_coloros to SystemVariant.COLOROS
            ),
            selectedItem = state.systemVariant,
            onItemSelected = viewModel::selectSystemVariant
        ).show(anchor)
    }

    private fun MaterialToolbar.updateRefreshAnimation(isRefreshing: Boolean) {
        menu.findItem(ACTION_REFRESH)?.isEnabled = !isRefreshing
        if (!isRefreshing) {
            refreshAnimator?.cancel()
            refreshAnimator = null
            findActionView(ACTION_REFRESH)?.rotation = 0f
            return
        }

        post {
            if (!viewModel.uiState.value.isRefreshing || refreshAnimator?.isRunning == true) return@post

            val refreshButton = findActionView(ACTION_REFRESH) ?: return@post
            refreshButton.rotation = 0f
            refreshAnimator = ObjectAnimator.ofFloat(refreshButton, View.ROTATION, 0f, 360f).apply {
                duration = REFRESH_ROTATION_DURATION
                interpolator = LinearInterpolator()
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }
    }

    private fun MaterialToolbar.findActionView(itemId: Int) = menu.findItem(itemId)?.let { item ->
        allViews.firstOrNull {
            it.contentDescription?.toString() == item.title?.toString()
        }
    }
}