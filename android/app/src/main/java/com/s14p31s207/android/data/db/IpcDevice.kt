package com.s14p31s207.android.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ipc_devices")
data class IpcDevice(
    @PrimaryKey
    val name: String,
    val ip: String? = null,
    val port: String? = null,
    val macAddress: String? = null
)
