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

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.setPadding
import androidx.core.view.updateMargins
import androidx.core.view.updateMarginsRelative
import com.google.android.material.color.MaterialColors
import com.highcapable.anip.demo.R
import com.highcapable.betterandroid.ui.extension.component.base.getThemeAttrsDrawable
import com.highcapable.betterandroid.ui.extension.component.base.isUiInNightMode
import com.highcapable.betterandroid.ui.extension.graphics.AttrState
import com.highcapable.betterandroid.ui.extension.graphics.ColorStateList
import com.highcapable.betterandroid.ui.extension.graphics.toAlphaColor
import com.highcapable.betterandroid.ui.extension.view.textColor
import com.highcapable.betterandroid.ui.extension.view.tooltipTextCompat
import com.highcapable.betterandroid.ui.extension.view.updatePadding
import com.highcapable.betterandroid.ui.extension.view.updateTypeface
import com.highcapable.hikage.core.base.Hikagable
import com.highcapable.hikage.core.builder.HikageBuilder
import com.highcapable.hikage.core.layout.LayoutParams
import com.highcapable.hikage.widget.android.widget.ImageView
import com.highcapable.hikage.widget.android.widget.LinearLayout
import com.highcapable.hikage.widget.android.widget.TextView
import com.highcapable.hikage.widget.androidx.appcompat.widget.AppCompatImageButton
import com.highcapable.hikage.widget.com.google.android.material.card.MaterialCardView
import android.R as Android_R
import com.google.android.material.R as Material_R

object IconItemLayout : HikageBuilder {

    val DEFAULT_PREVIEW_COLOR = Color.rgb(88, 97, 116)

    override fun build() = Hikagable {
        MaterialCardView(
            lparams = LayoutParams(widthMatchParent = true),
            init = {
                cardElevation = 0f
                radius = 12.dp.toFloat()
                strokeWidth = 0
                val backgroundColorAttr = if (resources.configuration.isUiInNightMode)
                    Material_R.attr.colorSurfaceContainerHigh
                else Material_R.attr.colorSurface
                setCardBackgroundColor(MaterialColors.getColor(this, backgroundColorAttr))
            }
        ) {
            LinearLayout(
                lparams = LayoutParams(widthMatchParent = true),
                init = {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(16.dp)
                }
            ) {
                MaterialCardView(
                    id = "item_preview",
                    lparams = LayoutParams(width = 56.dp, height = 56.dp),
                    init = {
                        cardElevation = 0f
                        radius = 16.dp.toFloat()
                        strokeWidth = 0
                    }
                ) {
                    ImageView(
                        id = "item_icon",
                        lparams = LayoutParams(width = 38.dp, height = 38.dp) {
                            gravity = Gravity.CENTER
                        }
                    ) {
                        imageTintList = ColorStateList(AttrState.NORMAL to Color.WHITE)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                }
                LinearLayout(
                    lparams = LayoutParams(width = 0) {
                        weight = 1f
                        updateMarginsRelative(start = 14.dp)
                    },
                    init = {
                        orientation = LinearLayout.VERTICAL
                    }
                ) {
                    LinearLayout(
                        lparams = LayoutParams(widthMatchParent = true),
                        init = {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                    ) {
                        TextView(
                            id = "item_label",
                            lparams = LayoutParams(width = 0) {
                                weight = 1f
                            }
                        ) {
                            ellipsize = TextUtils.TruncateAt.END
                            maxLines = 1
                            textSize = 17f
                            updateTypeface(Typeface.BOLD)
                        }
                        TextView(
                            id = "item_overlay",
                            lparams = LayoutParams {
                                updateMarginsRelative(start = 8.dp)
                            }
                        ) {
                            background = createRoundedBackground(
                                MaterialColors.getColor(this, Material_R.attr.colorPrimaryContainer),
                                6.dp.toFloat()
                            )
                            updatePadding(horizontal = 6.dp, vertical = 3.dp)
                            textColor = MaterialColors.getColor(this, Material_R.attr.colorOnPrimaryContainer)
                            text = stringResource(R.string.overlay)
                            textSize = 11f
                        }
                    }
                    LinearLayout(
                        lparams = LayoutParams(widthMatchParent = true, height = 30.dp) {
                            updateMargins(top = 4.dp)
                        },
                        init = {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            background = createRoundedBackground(
                                MaterialColors.getColor(this, Material_R.attr.colorOnSurface).toAlphaColor(0.04f),
                                4.dp.toFloat()
                            )
                            clipToOutline = true
                        }
                    ) {
                        TextView(
                            id = "item_package",
                            lparams = LayoutParams(width = 0) {
                                weight = 1f
                            }
                        ) {
                            ellipsize = TextUtils.TruncateAt.END
                            maxLines = 1
                            updatePadding(horizontal = 6.dp, vertical = 3.dp)
                            textColor = MaterialColors.getColor(this, Material_R.attr.colorOnSurfaceVariant)
                            textSize = 12f
                            typeface = Typeface.MONOSPACE
                        }
                        AppCompatImageButton(
                            id = "item_copy_package",
                            lparams = LayoutParams(width = 32.dp, heightMatchParent = true)
                        ) {
                            background = context.getThemeAttrsDrawable(Android_R.attr.selectableItemBackground)
                            contentDescription = stringResource(R.string.copy_package_name)
                            tooltipTextCompat = stringResource(R.string.copy_package_name)
                            imageTintList = ColorStateList(
                                AttrState.NORMAL to MaterialColors.getColor(this, Material_R.attr.colorOnSurfaceVariant)
                            )
                            minimumHeight = 0
                            minimumWidth = 0
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageResource(R.drawable.ic_copy)
                            setPadding(7.dp)
                        }
                    }
                    TextView(
                        id = "item_contributors",
                        lparams = LayoutParams(widthMatchParent = true) {
                            updateMargins(top = 4.dp)
                        }
                    ) {
                        ellipsize = TextUtils.TruncateAt.END
                        maxLines = 1
                        textColor = MaterialColors.getColor(this, Material_R.attr.colorOnSurfaceVariant)
                        textSize = 12f
                    }
                }
            }
        }
    }

    private fun createRoundedBackground(color: Int, radius: Float) = GradientDrawable().apply {
        cornerRadius = radius
        setColor(color)
    }
}