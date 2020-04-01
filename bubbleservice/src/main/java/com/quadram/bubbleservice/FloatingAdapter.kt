package com.quadram.bubbleservice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class FloatingAdapter(
    private var list: MutableList<FloatingItem>,
    val callback: OnFloatingClickListener
): RecyclerView.Adapter<FloatingAdapter.Holder>() {

    inner class Holder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val textView = itemView.findViewById<TextView>(R.id.floating_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.floating_image_view)
        val linear: LinearLayout = itemView.findViewById<LinearLayout>(R.id.linear).apply {
            isClickable = true
            setOnClickListener {
                callback.OnItemClick(list[adapterPosition])
            }
        }
    }

    fun updateElements(newList: List<FloatingItem>) {
        val diff = DiffUtil.calculateDiff(UtilsDif(newList, list))
        list.clear()
        list.addAll(newList)
        diff.dispatchUpdatesTo(this)
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
        if (item.isVisibleText) {
            holder.textView.visibility = View.VISIBLE
        }
        holder.textView.text = item.name
        holder.imageView.setImageDrawable(item.drawable)
    }


    inner class UtilsDif(val newList: List<FloatingItem>, val oldList: List<FloatingItem>)
        : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return newList[newItemPosition].id == oldList[oldItemPosition].id
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return newList[newItemPosition].isVisible == oldList[oldItemPosition].isVisible
                    && newList[newItemPosition].drawable == oldList[oldItemPosition].drawable
        }

    }
}