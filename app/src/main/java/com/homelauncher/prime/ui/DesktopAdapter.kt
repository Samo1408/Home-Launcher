package com.homelauncher.prime.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.homelauncher.prime.data.AppItem

class DesktopAdapter(
    private val entries: List<Entry>,
    private val onClick: (AppItem, View) -> Unit,
    private val onLongClick: (AppItem, View) -> Unit
) : RecyclerView.Adapter<DesktopAdapter.PageHolder>() {

    sealed class Entry {
        data class Shortcut(val item: AppItem) : Entry()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val container = FrameLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return PageHolder(container)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        // Minimal stub – real implementation would render app grid
        holder.bindPlaceholder(position)
    }

    override fun getItemCount(): Int = entries.size

    inner class PageHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindPlaceholder(pos: Int) {
            (itemView as? FrameLayout)?.removeAllViews()
            val tv = TextView(itemView.context).apply {
                text = "Page $pos"
            }
            (itemView as FrameLayout).addView(tv)
        }
    }
}
