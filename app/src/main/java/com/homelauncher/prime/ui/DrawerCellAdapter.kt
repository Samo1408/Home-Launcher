package com.homelauncher.prime.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.homelauncher.prime.R
import com.homelauncher.prime.data.AppItem
import com.homelauncher.prime.util.FontManager
import com.homelauncher.prime.util.IconCache

class DrawerCellAdapter(
    private val apps: List<AppItem>
) : RecyclerView.Adapter<DrawerCellAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = apps[pos]
        IconCache.load(h.itemView.context, item, h.icon)
        h.label.text = item.label
        val tf = FontManager.getCurrentTypeface(h.itemView.context)
        if (tf != android.graphics.Typeface.DEFAULT) h.label.typeface = tf
        h.itemView.setOnClickListener { AppGridAdapter.launch(h.itemView.context, item, it) }
    }
}

data class SelectionState(
    val selected: Set<String> = emptySet(),
    val active: Boolean = false
) {
    val count get() = selected.size
    fun toggle(id: String) = copy(selected = if (id in selected) selected - id else selected + id, active = true)
    fun exit() = SelectionState()
    fun isSelected(id: String) = id in selected
}
