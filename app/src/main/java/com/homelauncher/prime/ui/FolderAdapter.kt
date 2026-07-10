package com.homelauncher.prime.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.homelauncher.prime.R
import com.homelauncher.prime.data.AppItem
import com.homelauncher.prime.util.IconCache

/** Folder tile that opens a popup grid of its apps. */
class FolderAdapter(
    private val folders: List<Pair<String, List<AppItem>>>,
    private val onAppClick: (AppItem, View) -> Unit,
    private val onAppLongClick: (AppItem, View) -> Unit
) : RecyclerView.Adapter<FolderAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.folderTitle)
        val preview: GridLayout = v.findViewById(R.id.folderPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return VH(v)
    }

    override fun getItemCount() = folders.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val (name, apps) = folders[pos]
        val ctx: Context = h.itemView.context
        h.title.text = name
        val tf = com.homelauncher.prime.util.FontManager.getCurrentTypeface(ctx)
        if (tf != android.graphics.Typeface.DEFAULT) h.title.typeface = tf

        // Build 2x2 preview of icons, clipped within the folder circle
        h.preview.removeAllViews()
        apps.take(6).forEach { app ->
            val iv = ImageView(ctx)
            val lp = GridLayout.LayoutParams().apply {
                width = 0; height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
            iv.layoutParams = lp
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            IconCache.load(ctx, app, iv)
            h.preview.addView(iv)
        }

        h.itemView.setOnClickListener {
            FolderPopup.show(ctx, name, apps, onAppClick, onAppLongClick)
        }
        h.itemView.setOnLongClickListener {
            FolderPopup.show(ctx, name, apps, onAppClick, onAppLongClick); true
        }
    }
}
