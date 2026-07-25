package com.s14p31s207.android.domain.model

import com.s14p31s207.android.data.comm.tcp.ResponseStatus
import com.s14p31s207.android.data.comm.tcp.ResultStatus
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class LogType { RESPONSE, RESULT, ERROR, STATE }
sealed class LogEntry {
    abstract val id: String
    abstract val timestamp: String
    abstract val type: LogType
    abstract val ipcId: String

    data class Response(
        override val id: String,
        override val timestamp: String,
        override val ipcId: String,
        val requestId: String,
        val cmd: String,
        val status: ResponseStatus,
        val message: String,
    ) : LogEntry() {
        override val type = LogType.RESPONSE
    }

    data class Result(
        override val id: String,
        override val timestamp: String,
        override val ipcId: String,
        val requestId: String,
        val cmd: String,
        val status: ResultStatus,
        val message: String,
        val position: Double? = null,
        val speed: Double? = null,
    ) : LogEntry() {
        override val type = LogType.RESULT
    }

    data class Error(
        override val id: String,
        override val timestamp: String,
        override val ipcId: String,
        val errorCode: Int,
        val errorName: String,
        val category: String,
        val severity: String,
        val context: String,
        val solution: String,
        val requestId: String? = null,
    ) : LogEntry() {
        override val type = LogType.ERROR
    }

    data class State(
        override val id: String,
        override val timestamp: String,
        override val ipcId: String,
        val status: String,
        val position: Double?,
        val speed: Double?,
    ) : LogEntry() {
        override val type = LogType.STATE
    }
}

fun LogEntry.displayTime(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val zoneId = ZoneId.systemDefault()

    return runCatching {
        Instant.parse(timestamp)
            .atZone(zoneId)
            .format(formatter)
    }.recoverCatching {
        OffsetDateTime.parse(timestamp)
            .atZoneSameInstant(zoneId)
            .format(formatter)
    }.getOrElse {
        timestamp.replace("T", " ").take(19)
    }
}

fun LogEntry.displayText(): String = when (this) {
    is LogEntry.Response -> "$cmd $message"
    is LogEntry.Result -> "$cmd $message"
    is LogEntry.State -> "status=$status  pos=$position  speed=$speed"
    is LogEntry.Error -> "$errorCode  $errorName  ${requestId ?: ""}  ${solution}"
}
