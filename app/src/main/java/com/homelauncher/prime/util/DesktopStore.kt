package com.homelauncher.prime.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.json.JSONArray

object DesktopStore {

    private const val KEY = "desktop_shortcuts"
    private fun prefs(ctx: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    fun getShortcutIds(ctx: Context): Set<String> {
        val json = prefs(ctx).getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (_: Throwable) { emptySet() }
    }

    fun addShortcut(ctx: Context, appId: String) {
        val ids = getShortcutIds(ctx).toMutableSet().apply { add(appId) }
        prefs(ctx).edit().putString(KEY, JSONArray(ids.toList()).toString()).apply()
    }

    fun removeShortcut(ctx: Context, appId: String) {
        val ids = getShortcutIds(ctx).toMutableSet().apply { remove(appId) }
        prefs(ctx).edit().putString(KEY, JSONArray(ids.toList()).toString()).apply()
    }

    fun clearAll(ctx: Context) { prefs(ctx).edit().remove(KEY).apply() }
}
