package com.quadram.bubbleservice

interface MoveListener {
    fun move(x: Float, y: Float)
    fun onUp()
    fun onDown()
}