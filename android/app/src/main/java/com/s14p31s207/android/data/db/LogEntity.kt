package com.s14p31s207.android.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// LogEntity.kt
@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey
    val id: String,
    val ipcId: String,
    val type: String,        // "RESPONSE" | "RESULT" | "ERROR"
    val timestamp: String,
    val cmd: String? = null,
    val status: String? = null,  // "ACCEPTED"/"REJECTED" | "COMPLETED"/"FAILED"
    val position: Double? = null,
    val speed: Double? = null,
    val message: String? = null,
    val requestId: String? = null,
    // ERROR 전용
    val errorCode: Int? = null,
    val errorName: String? = null,
    val category: String? = null,
    val severity: String? = null,
    val context: String? = null,
    val solution: String? = null,
)