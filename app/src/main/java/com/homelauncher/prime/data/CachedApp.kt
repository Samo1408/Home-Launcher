package com.homelauncher.prime.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cached_apps", indices = [Index("package_name"), Index("user_serial")])
data class CachedApp(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "component_name") val componentName: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "user_serial") val userSerial: Long,
    @ColumnInfo(name = "is_work") val isWork: Boolean = false,
    @ColumnInfo(name = "user_label") val userLabel: String = "Personal",
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
)
