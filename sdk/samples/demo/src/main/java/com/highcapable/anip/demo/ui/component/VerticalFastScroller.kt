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
package com.highcapable.anip.demo.ui.component

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.drawable.StateListDrawable
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.highcapable.betterandroid.ui.extension.component.base.getDrawableCompat
import com.highcapable.betterandroid.ui.extension.component.base.toPx
import com.highcapable.betterandroid.ui.extension.graphics.AttrState
import kotlin.math.max
import kotlin.math.roundToInt

class VerticalFastScroller(
    private val recyclerView: RecyclerView,
    @DrawableRes thumbDrawableRes: Int,
    @DrawableRes trackDrawableRes: Int
) : RecyclerView.ItemDecoration(), RecyclerView.OnItemTouchListener {

    private companion object {

        const val FULL_ALPHA = 255
        const val THUMB_OPACITY = 0.85f
        const val FADE_DURATION = 250L
        const val HIDE_DELAY = 1_500L
        const val HIDE_DELAY_AFTER_DRAGGING = 1_200L
        const val MINIMUM_TOUCH_TARGET_SIZE_DP = 48

        val PRESSED_STATE = intArrayOf(AttrState.PRESSED)
        val EMPTY_STATE = intArrayOf()
    }

    private val thumbDrawable = recyclerView.context.getDrawableCompat<StateListDrawable>(thumbDrawableRes)
    private val trackDrawable = recyclerView.context.getDrawableCompat(trackDrawableRes)

    private val thumbWidth = thumbDrawable.intrinsicWidth.coerceAtLeast(1)
    private val minimumThumbHeight = thumbDrawable.intrinsicHeight.coerceAtLeast(1)
    private val trackWidth = trackDrawable.intrinsicWidth.coerceAtLeast(1)
    private val minimumTouchTargetSize = MINIMUM_TOUCH_TARGET_SIZE_DP.toPx(recyclerView.context)
    private val hideRunnable = Runnable { hide() }

    private val fadeAnimator = ValueAnimator().apply {
        duration = FADE_DURATION
        addUpdateListener {
            setDrawableAlpha(it.animatedValue as Int)
            recyclerView.postInvalidateOnAnimation()
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            updateScrollPosition()
            if (needsScrollbar) show()
        }
    }

    private var drawableAlpha = 0
    private var targetAlpha = 0
    private var thumbHeight = 0
    private var thumbCenterY = 0
    private var needsScrollbar = false
    private var isDragging = false
    private var lastTargetPosition = RecyclerView.NO_POSITION

    init {
        setDrawableAlpha(0)
        recyclerView.addItemDecoration(this)
        recyclerView.addOnItemTouchListener(this)
        recyclerView.addOnScrollListener(scrollListener)
        recyclerView.post {
            updateScrollPosition()
            if (needsScrollbar) show()
        }
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        updateScrollPosition()
        if (!needsScrollbar || drawableAlpha == 0) return

        val isLayoutRtl = recyclerView.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val thumbLeft = if (isLayoutRtl) 0 else recyclerView.width - thumbWidth
        val trackLeft = if (isLayoutRtl) 
            (thumbWidth - trackWidth) / 2
        else recyclerView.width - (thumbWidth + trackWidth) / 2
        val thumbTop = thumbCenterY - thumbHeight / 2

        trackDrawable.setBounds(trackLeft, 0, trackLeft + trackWidth, recyclerView.height)
        thumbDrawable.setBounds(thumbLeft, thumbTop, thumbLeft + thumbWidth, thumbTop + thumbHeight)
        trackDrawable.draw(canvas)
        thumbDrawable.draw(canvas)
    }

    override fun onInterceptTouchEvent(recyclerView: RecyclerView, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && isPointInsideThumb(event.x, event.y)) {
            startDragging()
            return true
        }
        return isDragging
    }

    override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> if (isPointInsideThumb(event.x, event.y)) startDragging()
            MotionEvent.ACTION_MOVE -> if (isDragging) dragTo(event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (isDragging) stopDragging()
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit

    private fun updateScrollPosition() {
        val visibleLength = recyclerView.height
        val contentLength = recyclerView.computeVerticalScrollRange()
        val scrollableLength = contentLength - visibleLength
        needsScrollbar = visibleLength > 0 && scrollableLength > 0
        if (!needsScrollbar) return

        val proportionalHeight = (
            visibleLength.toLong() * visibleLength / contentLength
        ).toInt()
        thumbHeight = max(minimumThumbHeight, proportionalHeight).coerceAtMost(visibleLength)

        val thumbTravel = visibleLength - thumbHeight
        val scrollOffset = recyclerView.computeVerticalScrollOffset().coerceIn(0, scrollableLength)
        if (!isDragging) thumbCenterY = thumbHeight / 2 + (
            thumbTravel.toLong() * scrollOffset / scrollableLength
        ).toInt()
    }

    private fun isPointInsideThumb(x: Float, y: Float): Boolean {
        if (!needsScrollbar || drawableAlpha == 0) return false
        val touchTargetWidth = max(thumbWidth, minimumTouchTargetSize)
        val touchTargetHeight = max(thumbHeight, minimumTouchTargetSize)
        val isInsideX = if (recyclerView.layoutDirection == View.LAYOUT_DIRECTION_RTL)
            x <= touchTargetWidth
        else x >= recyclerView.width - touchTargetWidth

        return isInsideX &&
            y >= thumbCenterY - touchTargetHeight / 2 &&
            y <= thumbCenterY + touchTargetHeight / 2
    }

    private fun startDragging() {
        isDragging = true
        lastTargetPosition = RecyclerView.NO_POSITION
        recyclerView.stopScroll()
        thumbDrawable.state = PRESSED_STATE
        show()
    }

    private fun stopDragging() {
        isDragging = false
        lastTargetPosition = RecyclerView.NO_POSITION
        thumbDrawable.state = EMPTY_STATE
        scheduleHide(HIDE_DELAY_AFTER_DRAGGING)
    }

    private fun dragTo(y: Float) {
        val thumbTravel = recyclerView.height - thumbHeight
        if (thumbTravel <= 0) return

        val targetCenter = y.coerceIn(thumbHeight / 2f, recyclerView.height - thumbHeight / 2f)
        val itemCount = recyclerView.adapter?.itemCount ?: return
        if (itemCount <= 0) return

        thumbCenterY = targetCenter.roundToInt()
        recyclerView.postInvalidateOnAnimation()

        val progress = (targetCenter - thumbHeight / 2f) / thumbTravel
        val targetPosition = (progress * (itemCount - 1)).roundToInt()
        if (targetPosition == lastTargetPosition) return

        lastTargetPosition = targetPosition
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is LinearLayoutManager)
            layoutManager.scrollToPositionWithOffset(targetPosition, 0)
        else recyclerView.scrollToPosition(targetPosition)
    }

    private fun show() {
        recyclerView.removeCallbacks(hideRunnable)
        animateTo(FULL_ALPHA)
        if (!isDragging) scheduleHide(HIDE_DELAY)
    }

    private fun hide() = animateTo(0)

    private fun scheduleHide(delay: Long) {
        recyclerView.removeCallbacks(hideRunnable)
        recyclerView.postDelayed(hideRunnable, delay)
    }

    private fun animateTo(alpha: Int) {
        if (targetAlpha == alpha) return
        targetAlpha = alpha
        fadeAnimator.cancel()
        if (drawableAlpha == alpha) return
        fadeAnimator.setIntValues(drawableAlpha, alpha)
        fadeAnimator.start()
    }

    private fun setDrawableAlpha(alpha: Int) {
        drawableAlpha = alpha
        thumbDrawable.alpha = (alpha * THUMB_OPACITY).roundToInt()
        trackDrawable.alpha = alpha
    }
}