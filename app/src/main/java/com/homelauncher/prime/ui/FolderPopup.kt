package com.homelauncher.prime.ui

import android.content.Context
import android.view.View
import com.homelauncher.prime.data.AppItem

object FolderPopup {
    fun show(
        context: Context,
        folderName: String,
        apps: List<AppItem>,
        onClick: (AppItem, View) -> Unit,
        onLongClick: (AppItem, View) -> Unit
    ) {
    }
}
