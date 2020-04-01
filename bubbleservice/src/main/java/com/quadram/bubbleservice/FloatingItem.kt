package com.quadram.bubbleservice

import android.graphics.drawable.Drawable

data class FloatingItem(
    val name: String,
    val id: String,
    val drawable: Drawable,
    val isVisible: Boolean = true,
    val isVisibleText: Boolean = false
)