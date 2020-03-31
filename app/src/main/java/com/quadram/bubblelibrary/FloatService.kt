package com.quadram.bubblelibrary

import android.graphics.drawable.Drawable
import com.quadram.bubbleservice.*

class FloatService: FloatingWidgetService() {

    override val removeViewDrawable: Drawable?
        get() = null

    override val drawableStates: List<DrawableState>
        get() = listOf(DrawableState(applicationContext.resources.getDrawable(R.drawable.ic_floating_launcher), FloatingStates.DEFAULT))

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