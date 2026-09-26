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
 * This file is created by fankes on 2026/8/31.
 */
package com.highcapable.anip.demo.ui.component

import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import com.highcapable.anip.demo.R
import com.highcapable.betterandroid.ui.component.adapter.factory.bindAdapter
import com.highcapable.betterandroid.ui.component.adapter.recycler.cosmetic.RecyclerCosmetic
import com.highcapable.betterandroid.ui.extension.component.base.toPx
import com.highcapable.betterandroid.ui.extension.view.updatePadding
import com.highcapable.hikage.core.layout.LayoutParams
import com.highcapable.hikage.extension.betterandroid.ui.component.adapter.onBindItemView
import com.highcapable.hikage.extension.setContentView
import com.highcapable.hikage.widget.android.widget.TextView
import com.highcapable.hikage.widget.androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.R as AppCompat_R

class DemoPopupMenu<T>(
    context: Context,
    items: List<Pair<Int, T>>,
    selectedItem: T,
    private val widthMatchedToAnchor: Boolean = false,
    onItemSelected: (T) -> Unit
) : PopupWindow(context, null, AppCompat_R.attr.popupMenuStyle) {

    private companion object {
        const val VERTICAL_OFFSET_DP = 4
        const val MENU_WIDTH_DP = 196
    }

    init {
        isFocusable = true
        isOutsideTouchable = true

        setContentView(context) {
            RecyclerView(
                lparams = LayoutParams(width = MENU_WIDTH_DP.dp)
            ) {
                itemAnimator = null
                overScrollMode = View.OVER_SCROLL_NEVER

                bindAdapter<Pair<Int, T>>(
                    cosmetic = RecyclerCosmetic.fromLinearVertical(context, rowSpacing = 4.toPx(context))
                ) {
                    onBindData { items }
                    onBindItemView(
                        Hikagable = {
                            TextView(
                                lparams = LayoutParams(widthMatchParent = true)
                            ) {
                                background = drawableResource(R.drawable.popup_menu_item_background)
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                                textSize = 16f

                                setTextColor(stateColorResource(R.color.popup_menu_item_text))
                                updatePadding(
                                    vertical = 8.dp,
                                    horizontal = 12.dp
                                )
                            }
                        }
                    ) { hikage, (title, item), _ ->
                        hikage.root<TextView>().apply {
                            isSelected = item == selectedItem
                            setText(title)
                        }
                    }
                    onItemViewClick { _, (_, item), _ ->
                        onItemSelected(item)
                        dismiss()
                    }
                }
            }
        }
    }

    fun show(anchor: View) {
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        val backgroundPadding = Rect()
        background?.getPadding(backgroundPadding)
        width = if (widthMatchedToAnchor)
            anchor.width
        else contentView.measuredWidth + backgroundPadding.left + backgroundPadding.right

        showAsDropDown(anchor, 0, VERTICAL_OFFSET_DP.toPx(anchor.context), Gravity.END)
    }
}