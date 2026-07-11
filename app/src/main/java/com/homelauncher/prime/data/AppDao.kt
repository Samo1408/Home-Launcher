package com.homelauncher.prime.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM cached_apps ORDER BY label COLLATE NOCASE ASC")
    fun getAll(): Flow<List<CachedApp>>

    @Query("SELECT * FROM cached_apps ORDER BY label COLLATE NOCASE ASC")
    suspend fun getAllList(): List<CachedApp>

    @Query("SELECT * FROM cached_apps WHERE is_work = :isWork ORDER BY label COLLATE NOCASE ASC")
    suspend fun getByWorkProfile(isWork: Boolean): List<CachedApp>

    @Query("SELECT * FROM cached_apps WHERE package_name = :pkg")
    suspend fun getByPackage(pkg: String): List<CachedApp>

    @Query("SELECT * FROM cached_apps WHERE user_serial = :serial ORDER BY label COLLATE NOCASE ASC")
    suspend fun getByUserSerial(serial: Long): List<CachedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<CachedApp>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: CachedApp)

    @Query("DELETE FROM cached_apps WHERE package_name = :pkg")
    suspend fun deleteByPackage(pkg: String)

    @Query("DELETE FROM cached_apps")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM cached_apps")
    suspend fun count(): Int
}
