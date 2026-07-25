package com.s14p31s207.android.domain.repository

import com.s14p31s207.android.data.comm.tcp.WifiConnectionDto
import kotlinx.coroutines.flow.StateFlow

interface ConnectRepository {
    // 현재 연결 상태를 UI에서 실시간으로 관찰 가능
    val isConnected: StateFlow<Boolean>
    val currentIpcId: StateFlow<String?>
    // IPC에 연결 (IP와 Port가 필요)
    suspend fun connect(dto: WifiConnectionDto): Result<Unit>

    // 연결 해제
    suspend fun disconnect()
}