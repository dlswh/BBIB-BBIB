package com.s14p31s207.android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IpcDeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: IpcDevice)

    @Query("SELECT * FROM ipc_devices ORDER BY name ASC")
    fun getAllDevices(): Flow<List<IpcDevice>>

    @Query("SELECT * FROM ipc_devices WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): IpcDevice?

    @Query("DELETE FROM ipc_devices WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT * FROM ipc_devices WHERE macAddress = :macAddress LIMIT 1")
    suspend fun getByMacAddress(macAddress: String): IpcDevice?
}
