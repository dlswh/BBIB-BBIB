package com.s14p31s207.android.data.repository

import com.s14p31s207.android.data.comm.bluetooth.BluetoothConnectionManager
import com.s14p31s207.android.data.comm.bluetooth.BluetoothConnectionStatus
import com.s14p31s207.android.data.comm.bluetooth.BluetoothConstants
import com.s14p31s207.android.data.comm.tcp.WifiConnectionDto
import com.s14p31s207.android.data.db.IpcDevice
import com.s14p31s207.android.domain.repository.ConnectRepository
import com.s14p31s207.android.domain.repository.ConnectionChannel
import com.s14p31s207.android.domain.repository.DeviceConnectionRepository
import com.s14p31s207.android.domain.repository.DeviceConnectionState
import com.s14p31s207.android.util.ConnectionPerfTrace
import com.s14p31s207.android.util.PerfLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class DeviceConnectionRepositoryImpl @Inject constructor(
    private val wifiRepository: ConnectRepository,
    private val btManager: BluetoothConnectionManager
) : DeviceConnectionRepository {

    private val _connectionState =
        MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.Disconnected())
    override val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    private var lastConnectedDevice: IpcDevice? = null
    private var lastConnectedChannel: ConnectionChannel? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reconnectJob: Job? = null

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 2
        private const val RECONNECT_DELAY_MS = 1000L
    }

    init {
        observeInternalStates()
    }

    private fun observeInternalStates() {
        scope.launch {
            wifiRepository.isConnected.collect { wifiConnected ->
                val currentState = _connectionState.value
                if (currentState is DeviceConnectionState.Connected &&
                    currentState.channel == ConnectionChannel.WIFI && !wifiConnected
                ) {
                    val device = lastConnectedDevice
                    val channel = lastConnectedChannel
                    if (device != null && channel == ConnectionChannel.WIFI) {
                        startReconnect(device, channel)
                    } else {
                        _connectionState.value = DeviceConnectionState.Disconnected()
                    }
                }
            }
        }

        scope.launch {
            btManager.connectionState.collect { btState ->
                val currentState = _connectionState.value
                if (currentState is DeviceConnectionState.Connected &&
                    currentState.channel == ConnectionChannel.BLUETOOTH &&
                    btState.status != BluetoothConnectionStatus.CONNECTED
                ) {
                    _connectionState.value = DeviceConnectionState.Disconnected()
                }
            }
        }
    }

    override suspend fun connect(
        device: IpcDevice,
        channel: ConnectionChannel,
        perfTrace: ConnectionPerfTrace?
    ): Result<Unit> {
        disconnect()
        _connectionState.value = DeviceConnectionState.Connecting
        val target = when (channel) {
            ConnectionChannel.WIFI -> "${device.ip}:${device.port}"
            ConnectionChannel.BLUETOOTH -> device.macAddress ?: "unknown"
        }

        return when (channel) {
            ConnectionChannel.WIFI -> {
                val ip = device.ip ?: return Result.failure(IllegalArgumentException("IP가 없습니다."))
                val port = device.port?.toIntOrNull() ?: return Result.failure(
                    IllegalArgumentException("포트가 올바르지 않습니다.")
                )

                wifiRepository.connect(WifiConnectionDto(ip, port, deviceName = device.name)).onSuccess {
                    lastConnectedDevice = device
                    lastConnectedChannel = channel
                    _connectionState.value = DeviceConnectionState.Connected(channel, device)
                    logConnectSuccess(perfTrace, target)
                }.onFailure {
                    _connectionState.value = DeviceConnectionState.Disconnected()
                    logConnectFailure(perfTrace, target, it.message)
                }
            }

            ConnectionChannel.BLUETOOTH -> {
                val mac = device.macAddress
                    ?: return Result.failure(IllegalArgumentException("MAC 주소가 없습니다."))

                btManager.connect(mac, BluetoothConstants.SERVICE_UUID).onSuccess {
                    lastConnectedDevice = device
                    lastConnectedChannel = channel
                    _connectionState.value = DeviceConnectionState.Connected(channel, device)
                    logConnectSuccess(perfTrace, target)
                }.onFailure {
                    _connectionState.value = DeviceConnectionState.Disconnected()
                    logConnectFailure(perfTrace, target, it.message)
                }
            }
        }
    }

    override suspend fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null

        lastConnectedDevice = null
        lastConnectedChannel = null
        // 수동 해제임을 명시
        _connectionState.value = DeviceConnectionState.Disconnected(isManual = true)

        wifiRepository.disconnect()
        btManager.disconnect()
    }

    private fun startReconnect(device: IpcDevice, channel: ConnectionChannel) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val ip = device.ip ?: return@launch
            val port = device.port?.toIntOrNull() ?: return@launch

            // WiFi 재연결 최대 N회 시도
            repeat(MAX_RECONNECT_ATTEMPTS) { attempt ->
                _connectionState.value = DeviceConnectionState.Reconnecting(
                    attempt = attempt + 1,
                    maxAttempts = MAX_RECONNECT_ATTEMPTS
                )

                delay(RECONNECT_DELAY_MS)

                // disconnect()가 호출됐으면 즉시 중단
                if (lastConnectedDevice == null) return@launch

                val result = wifiRepository.connect(WifiConnectionDto(ip, port, deviceName = device.name))
                if (result.isSuccess) {
                    _connectionState.value = DeviceConnectionState.Connected(channel, device)
                    return@launch
                }
            }

            // WiFi 재연결 전부 실패 → BT 자동 전환 시도
            // disconnect()가 호출됐으면 중단
            if (lastConnectedDevice == null) return@launch

            val mac = device.macAddress
            if (mac != null) {
                tryFallbackToBluetooth(device, mac)
            } else {
                // BT MAC이 없으면 그냥 연결 해제
                lastConnectedDevice = null
                lastConnectedChannel = null
                _connectionState.value = DeviceConnectionState.Disconnected()
            }
        }
    }

    private suspend fun tryFallbackToBluetooth(device: IpcDevice, mac: String) {
        _connectionState.value = DeviceConnectionState.SwitchingToBluetooth(device)

        // disconnect()가 호출됐으면 중단
        if (lastConnectedDevice == null) return

        // CancellationException은 반드시 재전파 — reconnectJob 취소 시 정상 종료되어야 함
        // 그 외 예외는 BT 전환 실패로 처리
        val result = try {
            btManager.connect(mac, BluetoothConstants.SERVICE_UUID)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

        result
            .onSuccess {
                lastConnectedChannel = ConnectionChannel.BLUETOOTH
                _connectionState.value =
                    DeviceConnectionState.Connected(ConnectionChannel.BLUETOOTH, device)
            }
            .onFailure {
                lastConnectedDevice = null
                lastConnectedChannel = null
                _connectionState.value = DeviceConnectionState.Disconnected()
            }
    }

    private fun logConnectSuccess(perfTrace: ConnectionPerfTrace?, target: String) {
        if (perfTrace == null) return

        val successTs = System.currentTimeMillis()
        val elapsedMs = successTs - perfTrace.startedAt
        PerfLogger.d(
            "[${perfTrace.category}][SUCCESS] attemptId=${perfTrace.attemptId} " +
                "target=$target ts=$successTs elapsedMs=$elapsedMs"
        )
    }

    private fun logConnectFailure(perfTrace: ConnectionPerfTrace?, target: String, error: String?) {
        if (perfTrace == null) return

        val failTs = System.currentTimeMillis()
        PerfLogger.d(
            "[${perfTrace.category}][FAIL] attemptId=${perfTrace.attemptId} " +
                "target=$target ts=$failTs error=${error ?: "unknown"}"
        )
    }
}
