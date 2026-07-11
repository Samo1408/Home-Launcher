package com.homelauncher.prime.util

import android.content.Context
import android.graphics.Typeface
import androidx.preference.PreferenceManager

object FontManager {

    private var currentTypeface: Typeface? = null

    fun getCurrentTypeface(context: Context): Typeface {
        return currentTypeface ?: Typeface.DEFAULT.also {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val fontPath = prefs.getString("font_path", null)
            if (fontPath != null) {
                try { currentTypeface = Typeface.createFromFile(fontPath) }
                catch (_: Throwable) { currentTypeface = Typeface.DEFAULT }
            } else currentTypeface = Typeface.DEFAULT
        }
    }

    fun setTypeface(typeface: Typeface) { currentTypeface = typeface }
    fun reset() { currentTypeface = Typeface.DEFAULT }
}
