package com.homelauncher.prime.util

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.LruCache

class LauncherIconFactory(private val context: Context) {

    private val density = context.resources.displayMetrics.density
    private val iconSizePx = (56f * density).toInt()
    private val memoryCache = LruCache<String, Bitmap>(128 * 1024 * 1024)

    fun getIcon(info: LauncherActivityInfo, sizePx: Int = iconSizePx): Bitmap {
        val key = cacheKey(info, sizePx)
        return memoryCache.get(key) ?: renderIcon(info, sizePx).also { memoryCache.put(key, it) }
    }

    fun getIconDrawable(info: LauncherActivityInfo, sizePx: Int = iconSizePx): Drawable {
        return BitmapDrawable(context.resources, getIcon(info, sizePx))
    }

    fun prefetch(apps: List<LauncherActivityInfo>, sizePx: Int = iconSizePx) {
        for (info in apps) {
            val key = cacheKey(info, sizePx)
            if (memoryCache.get(key) == null) {
                memoryCache.put(key, renderIcon(info, sizePx))
            }
        }
    }

    fun clearCache() = memoryCache.evictAll()
    fun trimToSize(maxSize: Int) = memoryCache.trimToSize(maxSize)

    private fun renderIcon(info: LauncherActivityInfo, sizePx: Int): Bitmap {
        val icon = info.getIcon(0) ?: return createPlaceholder(sizePx)
        return normalizeAndScale(icon, sizePx)
    }

    private fun normalizeAndScale(icon: Drawable, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
            val center = sizePx / 2f; val radius = sizePx * 0.4f
            val path = Path().apply { addCircle(center, center, radius, Path.Direction.CW) }
            canvas.clipPath(path)
            icon.background?.draw(canvas)
            icon.foreground?.draw(canvas)
        } else {
            icon.setBounds(0, 0, sizePx, sizePx); icon.draw(canvas)
        }
        return bitmap
    }

    private fun createPlaceholder(sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { color = 0xFFE0E0E0.toInt(); isAntiAlias = true }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx * 0.4f, paint)
        return bitmap
    }

    private fun cacheKey(info: LauncherActivityInfo, sizePx: Int): String =
        "${info.componentName.flattenToShortString()}@${Process.myUserHandle()}@$sizePx"
}
