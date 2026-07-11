package com.homelauncher.prime.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.homelauncher.prime.R
import com.homelauncher.prime.data.AppItem
import com.homelauncher.prime.util.FontManager
import com.homelauncher.prime.util.IconCache
import android.util.TypedValue

class DesktopAdapter(
    private val apps: List<AppItem>,
    private val onClick: (AppItem, View) -> Unit,
    private val onLongClick: (AppItem, View) -> Unit,
    private val columns: Int = 4
) : RecyclerView.Adapter<DesktopAdapter.PageHolder>() {

    private val appsPerPage = columns * 4
    private val pages = apps.chunked(appsPerPage)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val recycler = RecyclerView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = GridLayoutManager(parent.context, columns)
            adapter = GridInnerAdapter(
                mutableListOf(),
                onClick,
                onLongClick
            )
        }
        return PageHolder(recycler)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        (holder.recyclerView.adapter as GridInnerAdapter).submit(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    inner class PageHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recyclerView: RecyclerView = view as RecyclerView
    }

    private class GridInnerAdapter(
        private val items: MutableList<AppItem>,
        private val onClick: (AppItem, View) -> Unit,
        private val onLongClick: (AppItem, View) -> Unit
    ) : RecyclerView.Adapter<GridInnerAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.icon)
            val label: TextView = v.findViewById(R.id.label)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            val ctx = h.itemView.context
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 52f, ctx.resources.displayMetrics).toInt()
            h.icon.layoutParams = h.icon.layoutParams.apply { width = px; height = px }
            IconCache.load(ctx, item, h.icon)
            h.label.text = item.label
            val tf = FontManager.getCurrentTypeface(ctx)
            if (tf != android.graphics.Typeface.DEFAULT) h.label.typeface = tf
            h.itemView.setOnClickListener { v -> onClick(item, v) }
            h.itemView.setOnLongClickListener { onLongClick(item, h.itemView); true }
        }

        fun submit(list: List<AppItem>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }
    }
}
