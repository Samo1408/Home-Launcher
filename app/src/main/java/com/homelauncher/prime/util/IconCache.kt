package com.homelauncher.prime.util

import android.content.Context
import android.widget.ImageView
import com.homelauncher.prime.data.AppItem
import kotlinx.coroutines.*

object IconCache {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var factory: LauncherIconFactory? = null

    fun init(context: Context) {
        if (factory == null) factory = LauncherIconFactory(context.applicationContext)
    }

    fun load(context: Context, item: AppItem, target: ImageView) {
        init(context)
        val f = factory ?: run { target.setImageResource(android.R.drawable.sym_def_app_icon); return }
        val info = item.launcherInfo
        if (info != null) {
            val bitmap = f.getIcon(info)
            target.setImageBitmap(bitmap)
        } else {
            scope.launch {
                try {
                    val drawable = context.packageManager.getApplicationIcon(item.packageName)
                    target.post { target.setImageDrawable(drawable) }
                } catch (_: Throwable) {
                    target.post { target.setImageResource(android.R.drawable.sym_def_app_icon) }
                }
            }
        }
    }

    fun clear() = factory?.clearCache()
}
