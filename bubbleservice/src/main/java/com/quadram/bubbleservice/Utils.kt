package com.quadram.bubbleservice

import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.WindowManager

fun isAClick(
    clickThreshold: Int,
    startX: Float,
    endX: Float,
    startY: Float,
    endY: Float
): Boolean {
    val differenceX = Math.abs(startX - endX)
    val differenceY = Math.abs(startY - endY)
    return !(differenceX > clickThreshold || differenceY > clickThreshold)
}

fun getDisplayDimensions(context: Context): Point? {
    val wm =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val metrics = DisplayMetrics()
    display.getMetrics(metrics)
    val screenWidth = metrics.widthPixels
    var screenHeight = metrics.heightPixels

    // find out if status bar has already been subtracted from screenHeight
    display.getMetrics(metrics)
    val physicalHeight = metrics.heightPixels
    val statusBarHeight = getStatusBarHeight(context)
    val navigationBarHeight = getNavigationBarHeight(context)
    val heightDelta = physicalHeight - screenHeight
    if (heightDelta == 0 || heightDelta == navigationBarHeight) {
        screenHeight -= statusBarHeight
    }
    return Point(screenWidth, screenHeight)
}

fun getStatusBarHeight(context: Context): Int {
    val resources = context.resources
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
}

fun getNavigationBarHeight(context: Context): Int {
    val resources = context.resources
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
}

fun getWindowManager(context: Context): WindowManager {
    return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
}

fun getScreenSize(context: Context): Point? {
    val size = Point()
    getWindowManager(context).defaultDisplay.getSize(size)
    return size
}

