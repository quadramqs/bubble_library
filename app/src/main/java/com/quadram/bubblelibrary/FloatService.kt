package com.quadram.bubblelibrary

import android.graphics.drawable.Drawable
import com.quadram.bubbleservice.*

class FloatService: FloatingService() {

    override val timeToSetTransparent: Long
        get() = 2000

    override val timeToWaiting: Long
        get() = 3000

    override val removeViewDrawable: Drawable?
        get() = null

    override val drawableStates: List<DrawableState>
        get() = listOf(
            DrawableState(applicationContext.resources.getDrawable(R.drawable.ic_close), FloatingStates.OPEN),
            DrawableState(applicationContext.resources.getDrawable(R.drawable.ic_open), FloatingStates.CLOSE),
            DrawableState(applicationContext.resources.getDrawable(R.drawable.ic_right), FloatingStates.IDLE_RIGHT),
            DrawableState(applicationContext.resources.getDrawable(R.drawable.ic_left), FloatingStates.IDLE_LEFT),
            DrawableState(applicationContext.resources.getDrawable(R.drawable.ic_close_trans), FloatingStates.WAITING)
        )

    override val callback: OnFloatingClickListener
        get() = object: OnFloatingClickListener {
            override fun OnItemClick(item: FloatingItem) {
                when (item.id) {
                    "close" -> {  toggle() }
                    "delete" -> { stopSelf() }
                }
            }

        }

    override val items: List<FloatingItem>
        get() = mutableListOf(
            FloatingItem("Cerrar", "close", applicationContext.resources.getDrawable(android.R.drawable.ic_menu_close_clear_cancel)),
            FloatingItem("Eliminar", "delete", applicationContext.resources.getDrawable(android.R.drawable.ic_delete))
        )

}