package com.s14p31s207.android.data.repository

import android.util.Log
import com.google.gson.Gson
import com.s14p31s207.android.data.comm.bluetooth.BluetoothConnectionManager
import com.s14p31s207.android.data.comm.bluetooth.BluetoothConnectionStatus
import com.s14p31s207.android.data.comm.tcp.CommandDto
import com.s14p31s207.android.data.comm.tcp.HeartbeatDto
import com.s14p31s207.android.data.comm.tcp.ResponseStatus
import com.s14p31s207.android.data.comm.tcp.ResultStatus
import com.s14p31s207.android.data.comm.tcp.ServerMessage
import com.s14p31s207.android.data.comm.tcp.ServerMessageDto
import com.s14p31s207.android.data.comm.tcp.TcpChannel
import com.s14p31s207.android.data.db.CommandRequestDao
import com.s14p31s207.android.data.db.CommandRequestEntity
import com.s14p31s207.android.data.db.IpcDeviceDao
import com.s14p31s207.android.domain.model.LogEntry
import com.s14p31s207.android.domain.repository.CommandRepository
import com.s14p31s207.android.domain.repository.ConnectRepository
import com.s14p31s207.android.domain.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject


class CommandRepositoryImpl @Inject constructor(
    private val tcpChannel: TcpChannel,
    private val connectRepository: ConnectRepository,
    private val btManager: BluetoothConnectionManager,
    private val gson: Gson,
    private val commandRequestDao: CommandRequestDao,
    private val logRepository: LogRepository,
    private val ipcDeviceDao: IpcDeviceDao
) : CommandRepository {

    var isWifiMode: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)  // 추가
    private var heartbeatJob: Job? = null

    override val messages: SharedFlow<ServerMessage> = merge(
        tcpChannel.incomingMessages,
        btManager.incomingMessages
    ).map(::parseMessage)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    init {
        scope.launch {
            messages.collect { message ->
                val logEntry = message.toLogEntryOrNull()  // suspend 호출
                if (logEntry != null) logRepository.insert(logEntry)
            }
        }

        // 연결 상태 관찰 → heartbeat 자동 제어
        scope.launch {
            combine(
                connectRepository.isConnected,
                btManager.connectionState.map { it.status == BluetoothConnectionStatus.CONNECTED }
            ) { wifiConnected, btConnected ->
                wifiConnected || btConnected
            }.collect { connected ->
                if (connected) startHeartbeat() else stopHeartbeat()
            }
        }
    }
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                if (isWifiMode) {
                    tcpChannel.sendHeartbeat()
                } else {
                    btManager.sendMessage(gson.toJson(HeartbeatDto()))
                }
                delay(1_000L)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private suspend fun currentIpcId(): String? =
        if (isWifiMode) {
            connectRepository.currentIpcId.value.also {
                Log.d("CommandRepo", "currentIpcId (wifi): $it")
            }
        } else {
            val mac = btManager.connectionState.value.connectedDeviceAddress
            Log.d("CommandRepo", "currentIpcId (bt) mac: $mac")
            val name = mac?.let { ipcDeviceDao.getByMacAddress(it)?.name }
            Log.d("CommandRepo", "currentIpcId (bt) name: $name")
            name
        }

    override suspend fun send(command: CommandDto): Result<Unit> {
        runCatching {
            commandRequestDao.upsert(
                CommandRequestEntity(
                    id = command.id,
                    cmd = command.cmd,
                    timestamp = command.timestamp,
                    dataJson = gson.toJson(command.data)
                )
            )
        }.onFailure { error ->
            Log.w("CommandRepository", "Failed to store command request ${command.id}", error)
        }

        return if (isWifiMode) {
            tcpChannel.send(command)
        } else {
            btManager.send(command)
        }
    }

    private fun parseMessage(raw: String): ServerMessage {
        return try {
            val dto = gson.fromJson(raw, ServerMessageDto::class.java)
            fun Any?.toSafeDouble() = this?.toString()?.toDoubleOrNull()

            when (dto.type.trim().uppercase()) {
                "RESPONSE" -> ServerMessage.Response(
                    id = dto.id,
                    timestamp = dto.timestamp,
                    requestId = dto.data["requestId"]?.toString(),
                    cmd = dto.data["cmd"]?.toString() ?: "",
                    status = ResponseStatus.entries.find { it.name == dto.data["status"] }
                        ?: ResponseStatus.REJECTED
                )

                "STATE", "STATUS" -> ServerMessage.State(
                    id = dto.id,
                    timestamp = dto.timestamp,
                    status = dto.data["status"]?.toString() ?: "",
                    position = dto.data["position"].toSafeDouble(),
                    speed = dto.data["speed"].toSafeDouble()
                )
                "RESULT" -> ServerMessage.Result(
                    id = dto.id,
                    timestamp = dto.timestamp,
                    requestId = dto.data["requestId"]?.toString(),
                    cmd = dto.data["cmd"]?.toString() ?: "",
                    status = ResultStatus.entries.find { it.name == dto.data["status"] }
                        ?: ResultStatus.FAILED,
                    position = dto.data["position"].toSafeDouble(),
                    speed = dto.data["speed"].toSafeDouble()
                )

                "ERROR" -> ServerMessage.Error(
                    id = dto.id,
                    timestamp = dto.timestamp,
                    //errorCode = dto.data["errorCode"]?.toString()?.toIntOrNull() ?: 0,
                    errorCode = dto.data["errorCode"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0,
                    errorName = dto.data["errorName"]?.toString() ?: "",
                    category = dto.data["category"]?.toString() ?: "",
                    severity = dto.data["severity"]?.toString() ?: "",
                    context = dto.data["context"]?.toString() ?: "",
                    solution = dto.data["solution"]?.toString() ?: "",
                    requestId = dto.data["requestId"]?.toString()
                )

                "CONNECT" -> ServerMessage.Connect(
                    id = dto.id,
                    channel = dto.data["channel"]?.toString() ?: "",
                    status = dto.data["status"]?.toString() ?: ""
                )

                else -> ServerMessage.Unknown(raw)
            }
        } catch (_: Exception) {
            ServerMessage.Unknown(raw)
        }
    }

    private fun Any?.toSafeDouble(): Double? = this?.toString()?.toDoubleOrNull()
    private suspend fun ServerMessage.toLogEntryOrNull(): LogEntry? {
        val ipcId = currentIpcId() ?: return null

        return when (this) {
            is ServerMessage.Response -> LogEntry.Response(
                id = id, ipcId = ipcId,
                timestamp = timestamp,
                requestId = requestId.orEmpty(), cmd = cmd, status = status,
                message = if (status == ResponseStatus.ACCEPTED) "Request accepted" else "Request rejected"
            )
            is ServerMessage.Result -> LogEntry.Result(
                id = id, ipcId = ipcId,
                timestamp = timestamp,
                requestId = requestId.orEmpty(), cmd = cmd, status = status,
                message = if (status == ResultStatus.COMPLETED) "Completed" else "Failed"
            )
            is ServerMessage.Error -> LogEntry.Error(
                id = id, ipcId = ipcId,
                timestamp = timestamp,
                errorCode = errorCode, errorName = errorName, category = category,
                severity = severity, context = context, solution = solution,
                requestId = requestId
            )
            is ServerMessage.State -> LogEntry.State(
                id = id, ipcId = ipcId,
                timestamp = timestamp,
                status = status, position = position, speed = speed
            )
            else -> null
        }
    }
}
