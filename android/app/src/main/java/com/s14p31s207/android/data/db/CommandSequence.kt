package com.s14p31s207.android.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_sequences")
data class CommandSequence(
    @PrimaryKey
    val prefix: String,
    @ColumnInfo(name = "last_number")
    val lastNumber: Long = 0L
)
