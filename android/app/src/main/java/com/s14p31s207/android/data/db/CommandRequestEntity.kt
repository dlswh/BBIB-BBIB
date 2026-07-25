package com.s14p31s207.android.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_requests")
data class CommandRequestEntity(
    @PrimaryKey
    val id: String,
    val cmd: String,
    val timestamp: String,
    @ColumnInfo(name = "data")
    val dataJson: String
)
