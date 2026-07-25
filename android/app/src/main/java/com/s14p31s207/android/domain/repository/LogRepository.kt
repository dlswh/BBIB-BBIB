package com.s14p31s207.android.domain.repository

import com.s14p31s207.android.data.db.LogEntity
import com.s14p31s207.android.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getLogs(ipcId: String): Flow<List<LogEntry>>
    fun getLogsByPeriod(ipcId: String, start: String, end: String): Flow<List<LogEntry>>
    fun searchLogs(ipcId: String, start: String, end: String, query: String): Flow<List<LogEntry>>
    suspend fun insert(log: LogEntry): Unit
    suspend fun clear(ipcId: String): Unit
}