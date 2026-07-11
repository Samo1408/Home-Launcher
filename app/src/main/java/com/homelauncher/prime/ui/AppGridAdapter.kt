package com.homelauncher.prime.ui

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.homelauncher.prime.R
import com.homelauncher.prime.data.AppItem
import com.homelauncher.prime.util.FontManager
import com.homelauncher.prime.util.IconCache

class AppGridAdapter(
    private val items: MutableList<AppItem>,
    private val onClick: (AppItem, View) -> Unit,
    private val onLongClick: (AppItem, View) -> Unit,
    private val iconSizeDp: Int = 56
) : RecyclerView.Adapter<AppGridAdapter.VH>() {

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
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconSizeDp.toFloat(), h.itemView.resources.displayMetrics).toInt()
        h.icon.layoutParams = h.icon.layoutParams.apply { width = px; height = px }
        IconCache.load(h.itemView.context, item, h.icon)
        h.label.text = item.label
        val tf = FontManager.getCurrentTypeface(h.itemView.context)
        if (tf != android.graphics.Typeface.DEFAULT) h.label.typeface = tf
        h.itemView.setOnClickListener { view -> pressAnim(view) { onClick(item, h.itemView) } }
        h.itemView.setOnLongClickListener { onLongClick(item, h.itemView); true }
        if (h.itemView.scaleX == 1f && h.itemView.tag == null) {
            h.itemView.tag = true; h.itemView.scaleX = 0.6f; h.itemView.scaleY = 0.6f; h.itemView.alpha = 0f
            h.itemView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(280).setInterpolator(OvershootInterpolator(0.6f)).start()
        }
    }

    private fun pressAnim(view: View, onEnd: () -> Unit) {
        view.animate().cancel()
        view.animate().scaleX(0.90f).scaleY(0.90f).setDuration(65).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(OvershootInterpolator(0.5f)).withEndAction { onEnd() }.start()
        }.start()
    }

    fun submit(list: List<AppItem>) { items.clear(); items.addAll(list); notifyDataSetChanged() }

    companion object {
        fun launch(ctx: Context, item: AppItem, source: View) {
            val la = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            la.startMainActivity(android.content.ComponentName(item.packageName, item.componentName), item.user, Rect().also { source.getGlobalVisibleRect(it) }, null)
        }
    }
}
