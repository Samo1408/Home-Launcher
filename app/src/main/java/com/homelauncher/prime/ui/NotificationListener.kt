package com.homelauncher.prime.ui

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.content.SharedPreferences

/**
 * Listens for notifications and maintains per-package badge counts.
 * Apps (like HomeActivity) can read the badge counts from SharedPreferences.
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        private const val PREFS = "notification_badges"
        private const val KEY_COUNTS = "badge_counts"

        fun getBadgeCount(prefs: SharedPreferences, pkg: String): Int {
            val raw = prefs.getString(KEY_COUNTS, null) ?: return 0
            return try {
                org.json.JSONObject(raw).optInt(pkg, 0)
            } catch (_: Throwable) { 0 }
        }

        fun getAllBadges(prefs: SharedPreferences): Map<String, Int> {
            val raw = prefs.getString(KEY_COUNTS, null) ?: return emptyMap()
            return try {
                val obj = org.json.JSONObject(raw)
                val map = mutableMapOf<String, Int>()
                obj.keys().forEach { map[it] = obj.optInt(it, 0) }
                map
            } catch (_: Throwable) { emptyMap() }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        updateBadge(sbn.packageName, +1)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        updateBadge(sbn.packageName, -1)
    }

    private fun updateBadge(pkg: String, delta: Int) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val raw = prefs.getString(KEY_COUNTS, null)
        val obj = try { org.json.JSONObject(raw ?: "{}") } catch (_: Throwable) { org.json.JSONObject() }
        val cur = obj.optInt(pkg, 0)
        val next = (cur + delta).coerceAtLeast(0)
        if (next <= 0) obj.remove(pkg) else obj.put(pkg, next)
        prefs.edit().putString(KEY_COUNTS, obj.toString()).apply()
        // Broadcast so HomeActivity can refresh
        sendBroadcast(Intent("com.homelauncher.prime.NOTIFICATION_BADGE_CHANGED")
            .setPackage(packageName)
            .putExtra("pkg", pkg).putExtra("count", next))
    }

    override fun onListenerConnected() {
        // Clear and recalculate from current notifications
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE).edit()
        prefs.putString(KEY_COUNTS, org.json.JSONObject().toString()).apply()
        for (sbn in activeNotifications) {
            updateBadge(sbn.packageName, +1)
        }
    }
}
