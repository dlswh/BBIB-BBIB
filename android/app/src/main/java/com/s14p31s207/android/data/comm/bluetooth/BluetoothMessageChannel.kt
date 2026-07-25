package com.s14p31s207.android.data.comm.bluetooth

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class BluetoothMessageChannel(
    private val socket: BluetoothSocket,
    private val onMessageReceived: (String) -> Unit = {},
    private val onDisconnected: (String) -> Unit = {},
    private val onSendFailure: (Throwable) -> Unit = {}
) {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _incomingMessages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    private val reader = BufferedReader(
        InputStreamReader(socket.inputStream, StandardCharsets.UTF_8)
    )
    private val writer = OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8)

    private var listenJob: Job? = null

    private var heartbeatTimeoutJob: Job? = null

    @Volatile
    private var suppressDisconnectSignal = false

    @Volatile
    private var isClosed = false

    fun startListening() {
        listenJob?.cancel()
        suppressDisconnectSignal = false
        listenJob = ioScope.launch {
            try {
                resetHeartbeatTimeout()
                while (true) {
                    val line = reader.readLine() ?: break

                    resetHeartbeatTimeout()

                    _incomingMessages.emit(line)
                    onMessageReceived(line)
                }
                notifyDisconnected("Bluetooth connection closed.")
            } catch (_: CancellationException) {
                // The socket was intentionally closed.
            } catch (_: IOException) {
                notifyDisconnected("Bluetooth connection lost.")
            }
        }
    }

    // 타이머를 별도로 관리하여 무한 루프 부하 제거
    private fun resetHeartbeatTimeout() {
        heartbeatTimeoutJob?.cancel()
        heartbeatTimeoutJob = ioScope.launch {
            delay(3000L) // 3초간 새로운 메시지가 없으면
            notifyDisconnected("Bluetooth heartbeat timeout.")
            close() // 소켓 강제 종료
        }
    }

    suspend fun send(message: String): Result<String> = withContext(Dispatchers.IO) {
        val payload = message.ensureLineTerminated()

        return@withContext runCatching {
            writer.write(payload)
            writer.flush()
            payload
        }.onFailure(onSendFailure)
    }

    fun close() {
        suppressDisconnectSignal = true
        ioScope.cancel()
        closeResources()
    }

    fun release() {
        close()
    }

    private fun notifyDisconnected(message: String) {
        if (suppressDisconnectSignal) return
        closeResources()
        onDisconnected(message)
    }

    private fun closeResources() {
        if (isClosed) return // 이미 닫혔으면 무시하고 리턴
        isClosed = true

        listenJob?.cancel()
        listenJob = null

        runCatching { socket.close() }
        runCatching { reader.close() }
        runCatching { writer.close() }
    }

    private fun String.ensureLineTerminated(): String {
        return if (endsWith('\n')) this else "$this\n"
    }
}
