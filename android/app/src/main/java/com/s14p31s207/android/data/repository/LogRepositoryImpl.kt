package com.s14p31s207.android.data.repository

import com.s14p31s207.android.data.comm.tcp.ResponseStatus
import com.s14p31s207.android.data.comm.tcp.ResultStatus
import com.s14p31s207.android.data.db.LogDao
import com.s14p31s207.android.data.db.LogEntity
import com.s14p31s207.android.domain.model.*
import com.s14p31s207.android.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogRepositoryImpl(private val logDao: LogDao) : LogRepository {

    companion object {
        private const val MAX_LOGS = 500
    }

    override fun getLogs(ipcId: String): Flow<List<LogEntry>> =
        logDao.getAll(ipcId).map { entities -> entities.map { it.toLogEntry() } }

    override fun getLogsByPeriod(ipcId: String, start: String, end: String): Flow<List<LogEntry>> =
        logDao.getLogsByPeriod(ipcId, start, end).map { entities ->
            entities.map { it.toLogEntry() }
        }

    override fun searchLogs(ipcId: String, start: String, end: String, query: String): Flow<List<LogEntry>> =
        logDao.searchLogs(ipcId, start, end, query).map { entities ->
            entities.map { it.toLogEntry() }
        }

    override suspend fun insert(log: LogEntry) {
        logDao.insert(log.toEntity())
    }

    override suspend fun clear(ipcId: String) {
        logDao.clearAll(ipcId)
    }

    private fun LogEntity.toLogEntry(): LogEntry = when (type) {
        "RESPONSE" -> LogEntry.Response(
            id = id, ipcId = ipcId, timestamp = timestamp,
            requestId = requestId ?: "", cmd = cmd ?: "",
            status = if (status == "ACCEPTED") ResponseStatus.ACCEPTED else ResponseStatus.REJECTED,
            message = message ?: ""
        )
        "RESULT" -> LogEntry.Result(
            id = id, ipcId = ipcId, timestamp = timestamp,
            requestId = requestId ?: "", cmd = cmd ?: "",
            status = if (status == "COMPLETED") ResultStatus.COMPLETED else ResultStatus.FAILED,
            message = message ?: ""
        )
        "STATE" -> LogEntry.State(
            id = id, ipcId = ipcId, timestamp = timestamp,
            status = status.toString(), position = position, speed = speed
        )
        else -> LogEntry.Error(
            id = id, ipcId = ipcId, timestamp = timestamp,
            errorCode = errorCode ?: 0, errorName = errorName ?: "",
            category = category ?: "", severity = severity ?: "",
            context = context ?: "", solution = solution ?: "",
            requestId = requestId
        )
    }

    private fun LogEntry.toEntity(): LogEntity = when (this) {
        is LogEntry.Response -> LogEntity(
            id = id, ipcId = ipcId, type = "RESPONSE", timestamp = timestamp,
            cmd = cmd, status = status.name, message = message, requestId = requestId
        )
        is LogEntry.Result -> LogEntity(
            id = id, ipcId = ipcId, type = "RESULT", timestamp = timestamp,
            cmd = cmd, status = status.name, message = message, requestId = requestId
        )
        is LogEntry.Error -> LogEntity(
            id = id, ipcId = ipcId, type = "ERROR", timestamp = timestamp,
            errorCode = errorCode, errorName = errorName, category = category,
            severity = severity, context = context, solution = solution, requestId = requestId
        )
        is LogEntry.State -> LogEntity(
            id = id, ipcId = ipcId, type = "STATE", timestamp = timestamp,
            status = status, position = position, speed = speed
        )
    }
}