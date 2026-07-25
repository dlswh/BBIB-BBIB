package com.s14p31s207.android.feature.connection.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s14p31s207.android.data.comm.bluetooth.BluetoothConnectionManager
import com.s14p31s207.android.data.comm.bluetooth.BluetoothPairingResult
import com.s14p31s207.android.data.comm.bluetooth.BluetoothScanDevice
import com.s14p31s207.android.data.comm.bluetooth.BluetoothScanState
import com.s14p31s207.android.data.comm.bluetooth.BluetoothScanner
import com.s14p31s207.android.data.comm.tcp.TcpConnectionTester
import com.s14p31s207.android.data.db.AppDatabase
import com.s14p31s207.android.data.db.IpcDevice
import com.s14p31s207.android.util.PairingPerfTrace
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RegistrationStep { NAME, WIFI, BLUETOOTH, DONE }

data class DeviceRegistrationUiState(
    val name: String = "",
    val ip: String = "",
    val port: String = "",
    val wifiTestState: WifiTestState = WifiTestState.IDLE,
    val confirmedIp: String? = null,
    val confirmedPort: String? = null,
    val pendingMacAddress: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

enum class WifiTestState { IDLE, TESTING, SUCCESS, FAILURE }

@HiltViewModel
class DeviceRegistrationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: BluetoothScanner,
    private val connectionManager: BluetoothConnectionManager,
    private val tcpTester: TcpConnectionTester
) : ViewModel() {

    private val dao = AppDatabase.getInstance(context).ipcDeviceDao()

    private val _uiState = MutableStateFlow(DeviceRegistrationUiState())
    val uiState: StateFlow<DeviceRegistrationUiState> = _uiState.asStateFlow()

    val scanState: StateFlow<BluetoothScanState> = scanner.scanState

    init {
        // ViewModel 생성 시 이미 페어링된(BONDED) 기기를 바로 목록에 표시
        loadBondedDevices()
    }

    // ── 1. 기기 이름 관리 ──────────────────────────────────────────────

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    // ── 2. WiFi 섹션 로직 ───────────────────────────────────────────────

    fun onIpChanged(ip: String) {
        _uiState.update { it.copy(ip = ip, wifiTestState = WifiTestState.IDLE, confirmedIp = null) }
    }

    fun onPortChanged(port: String) {
        _uiState.update {
            it.copy(
                port = port,
                wifiTestState = WifiTestState.IDLE,
                confirmedPort = null
            )
        }
    }

    fun testWifiConnection() {
        val currentIp = _uiState.value.ip.trim()
        val currentPort = _uiState.value.port.trim().toIntOrNull()

        if (currentIp.isBlank() || currentPort == null) {
            _uiState.update { it.copy(errorMessage = "IP와 포트를 올바르게 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(wifiTestState = WifiTestState.TESTING, errorMessage = null) }

            val result = tcpTester.test(currentIp, currentPort)

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        wifiTestState = WifiTestState.SUCCESS,
                        confirmedIp = currentIp,
                        confirmedPort = currentPort.toString()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        wifiTestState = WifiTestState.FAILURE,
                        confirmedIp = null,
                        confirmedPort = null,
                        errorMessage = "연결에 실패했습니다: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun skipWifi() {
        _uiState.update {
            it.copy(
                ip = "",
                port = "",
                wifiTestState = WifiTestState.IDLE,
                confirmedIp = null,
                confirmedPort = null
            )
        }
    }

    // ── 3. Bluetooth 섹션 로직 ──────────────────────────────────────────

    /**
     * ViewModel 초기화 시 호출.
     * BluetoothScanner가 getBondedDevices()를 제공한다면 scanState에 merge하거나,
     * scanner.loadBondedDevices()를 직접 호출해 초기 목록을 채운다.
     */
    private fun loadBondedDevices() {
        scanner.loadBondedDevices()
    }

    fun startDiscovery() = scanner.startScan()

    fun stopDiscovery() = scanner.stopScan()

    fun pairDevice(address: String): Boolean {
        val startedAt = System.currentTimeMillis()
        return scanner.pairDevice(
            address = address,
            perfTrace = PairingPerfTrace(
                attemptId = "pair-$startedAt",
                startedAt = startedAt
            )
        )
    }

    fun clearPairingResult() = scanner.clearPairingResult()

    /**
     * 이미 페어링된 기기를 목록에서 짧게 탭해 선택할 때 호출.
     * pendingMacAddress를 업데이트해 SAVE 버튼을 활성화한다.
     */
    fun onSelectDevice(device: BluetoothScanDevice) {
        // 이미 선택된 기기를 다시 탭하면 선택 해제
        val newMac =
            if (_uiState.value.pendingMacAddress == device.address) null else device.address
        _uiState.update { it.copy(pendingMacAddress = newMac) }
    }

    /**
     * 페어링 성공 결과를 받아 MAC 주소를 저장하고 해당 기기를 자동 선택.
     */
    fun onPairingSuccess(pairingResult: BluetoothPairingResult) {
        if (pairingResult.isSuccess) {
            _uiState.update { it.copy(pendingMacAddress = pairingResult.address) }
        }
    }

    fun skipBluetooth() {
        scanner.clearPairingResult()
        _uiState.update { it.copy(pendingMacAddress = null) }
    }

    // ── 4. 최종 저장 로직 ────────────────────────────────────────────────

    fun save() {
        val state = _uiState.value

        val hasWifi = state.confirmedIp != null && state.confirmedPort != null
        val hasBt = state.pendingMacAddress != null

        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "기기 이름을 입력해주세요.") }
            return
        }

        if (!hasWifi && !hasBt) {
            _uiState.update { it.copy(errorMessage = "WiFi 테스트를 완료하거나 Bluetooth 기기를 선택해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val existing = dao.getByName(state.name.trim())

                dao.upsert(
                    IpcDevice(
                        name = state.name.trim(),
                        ip = state.confirmedIp ?: existing?.ip,
                        port = state.confirmedPort ?: existing?.port,
                        macAddress = state.pendingMacAddress ?: existing?.macAddress
                    )
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "저장에 실패했습니다: ${e.message}")
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun clearSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    override fun onCleared() {
        scanner.release()
        super.onCleared()
    }
}