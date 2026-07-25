package com.s14p31s207.android.data.repository

import android.util.Log
import com.s14p31s207.android.data.comm.tcp.TcpChannel
import com.s14p31s207.android.data.comm.tcp.WifiConnectionDto
import com.s14p31s207.android.domain.repository.ConnectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectRepositoryImpl(
    private val tcpChannel: TcpChannel,
) : ConnectRepository {
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentIpcId = MutableStateFlow<String?>(null)  // 추가
    override val currentIpcId: StateFlow<String?> = _currentIpcId.asStateFlow()  // 추가

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var receiveJob: Job? = null

    override suspend fun connect(dto: WifiConnectionDto): Result<Unit> {
        val result = tcpChannel.connect(dto.ip, dto.port)
        if (result.isSuccess) {
            _currentIpcId.value = dto.deviceName  // 추가
            _isConnected.value = true
            receiveJob?.cancel()
            receiveJob = scope.launch {
                tcpChannel.startReceiving()
                _isConnected.value = false
                _currentIpcId.value = null  // 추가: 연결 끊기면 초기화
            }
        }
        return result
    }

    override suspend fun disconnect() {
        receiveJob?.cancel()
        tcpChannel.disconnect()
        _isConnected.value = false
        _currentIpcId.value = null  // 추가
    }
}