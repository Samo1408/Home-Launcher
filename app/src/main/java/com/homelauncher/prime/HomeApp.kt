package com.homelauncher.prime

import android.app.Application
import com.homelauncher.prime.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HomeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { AppRepository.loadAll(this@HomeApp) } catch (_: Throwable) {}
        }
    }
}
