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

    override val iconClose: Drawable?
        get() = applicationContext.resources.getDrawable(R.drawable.ic_close)
    override val iconHideLeft: Drawable?
        get() = applicationContext.resources.getDrawable(R.drawable.ic_left)
    override val iconHideRight: Drawable?
        get() = applicationContext.resources.getDrawable(R.drawable.ic_right)
    override val iconOpen: Drawable?
        get() = applicationContext.resources.getDrawable(R.drawable.ic_open)
    override val movementStyle: FloatingStyle
        get() = FloatingStyle.STICKED_TO_SIDES

    override val callback: OnFloatingClickListener
        get() = object: OnFloatingClickListener {
            override fun OnItemClick(item: FloatingItem) {
                when (item.id) {
                    "close" -> {  toggleMenu() }
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