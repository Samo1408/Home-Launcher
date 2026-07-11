package com.homelauncher.prime.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

object AppRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var db: AppDatabase? = null
    private fun db(ctx: Context) = db ?: AppDatabase.getInstance(ctx).also { db = it }

    private val _appsFlow = MutableStateFlow<List<AppItem>>(emptyList())
    val appsFlow: Flow<List<AppItem>> = _appsFlow

    @Volatile var cachedApps: List<AppItem> = emptyList()
        private set
    @Volatile var isLoading = false
        private set
    @Volatile var dirty = false
        private set

    private var receiverRegistered = false
    private var initDone = false

    fun initialize(context: Context) {
        if (initDone) return
        initDone = true
        registerPackageListener(context)
        scope.launch {
            val roomApps = db(context).appDao().getAllList()
            if (roomApps.isNotEmpty()) {
                val items = roomApps.map { it.toAppItem() }
                cachedApps = items
                _appsFlow.value = items
            }
            syncWithSystem(context)
        }
    }

    suspend fun refresh(context: Context) { dirty = true; syncWithSystem(context) }

    fun onPackageChanged(context: Context, packageName: String) {
        scope.launch {
            try {
                val dao = db(context).appDao()
                val launcher = context.getSystemService(LauncherApps::class.java) ?: return@launch
                val um = context.getSystemService(UserManager::class.java) ?: return@launch
                val myUser = Process.myUserHandle()
                dao.deleteByPackage(packageName)
                val newApps = mutableListOf<CachedApp>()
                for (user in um.userProfiles) {
                    val isWork = user != myUser
                    val serial = um.getSerialNumberForUser(user)
                    val userLabel = if (isWork) "Work" else "Personal"
                    try {
                        for (info in launcher.getActivityList(packageName, user)) {
                            newApps += CachedApp(
                                id = "${info.applicationInfo.packageName}/${info.componentName.className}@$serial",
                                packageName = info.applicationInfo.packageName,
                                componentName = info.componentName.className,
                                label = info.label?.toString() ?: info.applicationInfo.packageName,
                                userSerial = serial, isWork = isWork, userLabel = userLabel
                            )
                        }
                    } catch (_: Throwable) {}
                }
                if (newApps.isNotEmpty()) dao.insertAll(newApps)
                cachedApps = dao.getAllList().map { it.toAppItem() }
                _appsFlow.value = cachedApps
            } catch (_: Throwable) {}
        }
    }

    private suspend fun syncWithSystem(context: Context) {
        isLoading = true
        try {
            val launcher = context.getSystemService(LauncherApps::class.java) ?: return
            val um = context.getSystemService(UserManager::class.java) ?: return
            val myUser = Process.myUserHandle()
            val dao = db(context).appDao()
            if (dirty) dao.deleteAll()
            val count = dao.count()
            if (count > 0 && !dirty) {
                cachedApps = dao.getAllList().map { it.toAppItem() }
                _appsFlow.value = cachedApps
                isLoading = false
                return
            }
            val allApps = mutableListOf<CachedApp>()
            for (user in um.userProfiles) {
                val isWork = user != myUser
                val serial = um.getSerialNumberForUser(user)
                val userLabel = if (isWork) "Work" else "Personal"
                for (info in launcher.getActivityList(null, user)) {
                    allApps += CachedApp(
                        id = "${info.applicationInfo.packageName}/${info.componentName.className}@$serial",
                        packageName = info.applicationInfo.packageName,
                        componentName = info.componentName.className,
                        label = info.label?.toString() ?: info.applicationInfo.packageName,
                        userSerial = serial, isWork = isWork, userLabel = userLabel
                    )
                }
            }
            if (allApps.isNotEmpty()) dao.insertAll(allApps)
            cachedApps = dao.getAllList().map { it.toAppItem() }
            _appsFlow.value = cachedApps
            dirty = false
        } finally { isLoading = false }
    }

    private fun registerPackageListener(context: Context) {
        if (receiverRegistered) return
        receiverRegistered = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.applicationContext.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val pkg = intent?.data?.schemeSpecificPart ?: return
                ctx ?: return
                onPackageChanged(ctx, pkg)
            }
        }, filter)
    }

    fun groupByUser(apps: List<AppItem>): Map<String, List<AppItem>> = apps.groupBy { it.userLabel }

    fun cached() = cachedApps
    fun loadAll(context: Context): List<AppItem> {
        runBlocking { syncWithSystem(context) }
        return cachedApps
    }
    fun invalidate() { dirty = true; cachedApps = emptyList() }
    fun loadFast(context: Context): List<AppItem> { initialize(context); return cachedApps }
}

private fun CachedApp.toAppItem(): AppItem = AppItem(
    packageName = packageName, componentName = componentName, label = label,
    user = Process.myUserHandle(), userSerial = userSerial, isWork = isWork, userLabel = userLabel
)
