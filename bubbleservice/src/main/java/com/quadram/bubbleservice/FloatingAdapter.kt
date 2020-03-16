package com.quadram.bubbleservice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FloatingAdapter(
    private val list: List<FloatingItem>,
    val callback: OnFloatingClickListener
): RecyclerView.Adapter<FloatingAdapter.Holder>() {

    inner class Holder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val textView = itemView.findViewById<TextView>(R.id.floating_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.floating_image_view)
        val linear = itemView.findViewById<LinearLayout>(R.id.linear).apply {
            isClickable = true
            setOnClickListener {
                callback.OnItemClick(list[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.floating_item, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = list[holder.adapterPosition]
        holder.textView.text = item.name
        holder.imageView.setImageDrawable(item.drawable)
    }
}