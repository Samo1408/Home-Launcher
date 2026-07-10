package com.homelauncher.prime.ui

import com.homelauncher.prime.data.AppItem

sealed class DrawerEntry {
    data class App(val app: AppItem) : DrawerEntry()
    data class Folder(val name: String, val apps: List<AppItem>) : DrawerEntry()
}
