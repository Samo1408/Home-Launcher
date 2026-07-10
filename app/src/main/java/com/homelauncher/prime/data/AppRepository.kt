package com.homelauncher.prime.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AppRepository {
    private const val CACHE_FILE = "apps_cache.json"
    private const val CACHE_META = "apps_cache_meta.json"

    @Volatile private var cache: List<AppItem>? = null
    @Volatile var dirty: Boolean = true
        private set

    private var receiverRegistered = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun cached(): List<AppItem>? = cache

    fun invalidate() { dirty = true; cache = null }

    fun loadFast(context: Context): List<AppItem> {
        val file = File(context.filesDir, CACHE_FILE)
        if (file.exists() && cache == null) {
            cache = deserialize(file)
            dirty = false
        }
        if (cache == null || dirty) {
            scope.launch { loadAll(context) }
        }
        return cache ?: emptyList()
    }

    fun loadAll(context: Context): List<AppItem> {
        val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        val myUser = Process.myUserHandle()
        val out = ArrayList<AppItem>(256)

        for (user in um.userProfiles) {
            val isWork = user != myUser
            val serial = um.getSerialNumberForUser(user)
            val userLabel = if (isWork) "user $serial" else "Personal"
            for (info in launcher.getActivityList(null, user)) {
                out += AppItem(
                    packageName = info.applicationInfo.packageName,
                    componentName = info.componentName.className,
                    label = info.label?.toString() ?: info.applicationInfo.packageName,
                    user = user,
                    userSerial = serial,
                    isWork = isWork,
                    userLabel = userLabel
                ).also { it.launcherInfo = info }
            }
        }
        out.sortWith(compareBy { it.label.lowercase() })
        cache = out
        dirty = false
        scope.launch { serialize(context, out) }
        return out
    }

    fun registerPackageListener(context: Context) {
        if (receiverRegistered) return
        receiverRegistered = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                invalidate()
            }
        }, filter)
    }

    fun groupByUser(apps: List<AppItem>): Map<String, List<AppItem>> =
        apps.groupBy { it.userLabel }

    private fun serialize(ctx: Context, list: List<AppItem>) {
        try {
            val arr = JSONArray()
            for (a in list) {
                arr.put(JSONObject().apply {
                    put("pkg", a.packageName)
                    put("cls", a.componentName)
                    put("label", a.label)
                    put("serial", a.userSerial)
                    put("work", a.isWork)
                    put("ulabel", a.userLabel)
                })
            }
            File(ctx.filesDir, CACHE_FILE).writeText(arr.toString())
            val meta = JSONObject().apply {
                put("count", list.size)
                put("time", System.currentTimeMillis())
            }
            File(ctx.filesDir, CACHE_META).writeText(meta.toString())
        } catch (_: Throwable) {}
    }

    private fun deserialize(file: File): List<AppItem>? = try {
        val arr = JSONArray(file.readText())
        val out = ArrayList<AppItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            out += AppItem(
                packageName = obj.getString("pkg"),
                componentName = obj.getString("cls"),
                label = obj.getString("label"),
                user = Process.myUserHandle(),
                userSerial = obj.getLong("serial"),
                isWork = obj.getBoolean("work"),
                userLabel = obj.getString("ulabel")
            )
        }
        out
    } catch (_: Throwable) { null }
}
