package com.quadram.bubblelibrary

import android.graphics.drawable.Drawable
import com.quadram.bubbleservice.FloatingItem
import com.quadram.bubbleservice.FloatingWidgetService
import com.quadram.bubbleservice.OnFloatingClickListener

class FloatService: FloatingWidgetService() {
    override fun getItemDrawable(): Drawable {
        return applicationContext.resources.getDrawable(R.mipmap.ic_launcher)
    }

    override fun getCallback(): OnFloatingClickListener {
        return object: OnFloatingClickListener {
            override fun OnItemClick(item: FloatingItem) {
                when (item.id) {
                    "close" -> {  toggle() }
                    "delete" -> { stopSelf() }
                }
            }

        }
    }

    override fun getItems(): List<FloatingItem> {
        val list = mutableListOf<FloatingItem>()
        list.add(FloatingItem("Cerrar", "close", applicationContext.resources.getDrawable(android.R.drawable.ic_menu_close_clear_cancel)))
        list.add(FloatingItem("Eliminar", "delete", applicationContext.resources.getDrawable(android.R.drawable.ic_delete)))
        return list
    }

}