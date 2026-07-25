package com.s14p31s207.android.domain.repository

import com.s14p31s207.android.data.db.IpcDevice
import com.s14p31s207.android.util.ConnectionPerfTrace
import kotlinx.coroutines.flow.StateFlow

enum class ConnectionChannel { WIFI, BLUETOOTH }

sealed interface DeviceConnectionState {
    data class Disconnected(val isManual: Boolean = false) : DeviceConnectionState
    object Connecting : DeviceConnectionState
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : DeviceConnectionState
    data class SwitchingToBluetooth(val device: IpcDevice) :
        DeviceConnectionState  // WiFi 재연결 전부 실패 시 BT 자동 전환 중

    data class Connected(val channel: ConnectionChannel, val device: IpcDevice) :
        DeviceConnectionState
}

interface DeviceConnectionRepository {
    val connectionState: StateFlow<DeviceConnectionState>

    suspend fun connect(
        device: IpcDevice,
        channel: ConnectionChannel,
        perfTrace: ConnectionPerfTrace? = null
    ): Result<Unit>
    suspend fun disconnect()
}
