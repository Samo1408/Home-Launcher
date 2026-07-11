package com.homelauncher.prime.data

import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.UserHandle

data class AppItem(
    val packageName: String,
    val componentName: String,
    val label: String,
    val user: UserHandle,
    val userSerial: Long,
    val isWork: Boolean,
    val userLabel: String,
) {
    @Transient var launcherInfo: LauncherActivityInfo? = null
    @Transient var cachedIcon: Drawable? = null
    val id: String get() = "$packageName/$componentName@$userSerial"

    fun loadLabel(pm: PackageManager): String {
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: Throwable) { label }
    }
}
