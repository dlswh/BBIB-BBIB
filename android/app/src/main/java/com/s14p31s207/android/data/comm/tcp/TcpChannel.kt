package com.s14p31s207.android.data.comm.tcp

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class TcpChannel {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private val gson = Gson()

    private val _incomingMessages = MutableSharedFlow<String>(replay = 0)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    suspend fun connect(ip: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            socket = Socket()
            socket?.connect(InetSocketAddress(ip, port), 5000)
            socket?.soTimeout = 3000
            writer = PrintWriter(socket!!.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 명령어 전송 (\n 붙여서 전송 → 서버가 줄바꿈으로 패킷 구분)
    suspend fun send(command: CommandDto): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val json = gson.toJson(command)
            Log.d("TcpChannel", "sending: $json")

            val activeWriter = writer
                ?: return@withContext Result.failure(
                    IllegalStateException("TCP socket is not connected.")
                )

            activeWriter.println(json)
            Log.d("TcpChannel", "sent successfully, hasError: ${activeWriter.checkError()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.d("TcpChannel", "send exception: ${e.message}")
            Result.failure(e)
        }
    }

    // 수신 루프 - 연결 직후 코루틴으로 실행, 연결이 끊길 때까지 계속 돌아감
    // ConnectRepositoryImpl에서 connect() 후 launch { tcpChannel.startReceiving() } 로 호출
    suspend fun startReceiving() = withContext(Dispatchers.IO) {
        Log.d("TcpChannel", "startReceiving started")
        try {
            while (isConnected()) {
                val line = reader?.readLine() ?: break
                Log.d("TcpChannel", "received: $line")
                _incomingMessages.emit(line)
            }
        } catch (e: Exception) {
            Log.d("TcpChannel", "startReceiving error: ${e.message}")
            e.printStackTrace()
        }
        Log.d("TcpChannel", "startReceiving ended")
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            writer?.close()
            reader?.close()
            socket?.close()
            socket = null
            writer = null
            reader = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isConnected(): Boolean =
        socket?.isConnected == true && socket?.isClosed == false

    suspend fun sendHeartbeat(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val json = gson.toJson(HeartbeatDto())
            Log.d("TcpChannel", "sending heartbeat: $json")

            val activeWriter = writer
                ?: return@withContext Result.failure(
                    IllegalStateException("TCP socket is not connected.")
                )

            activeWriter.println(json)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.d("TcpChannel", "sendHeartbeat exception: ${e.message}")
            Result.failure(e)
        }
    }
}
