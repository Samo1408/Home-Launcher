package com.homelauncher.prime.data

import android.content.pm.LauncherActivityInfo
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
    val id: String get() = "$packageName/$componentName@$userSerial"
}
