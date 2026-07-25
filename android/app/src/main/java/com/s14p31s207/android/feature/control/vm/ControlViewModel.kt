package com.s14p31s207.android.feature.control.vm

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.s14p31s207.android.data.comm.bluetooth.BluetoothBondedDevice
import com.s14p31s207.android.data.comm.bluetooth.BluetoothBondedDeviceProvider
import com.s14p31s207.android.data.comm.tcp.Channel
import com.s14p31s207.android.data.comm.tcp.CommandDto
import com.s14p31s207.android.data.comm.tcp.CommandFactory
import com.s14p31s207.android.data.comm.tcp.Direction
import com.s14p31s207.android.data.comm.tcp.ProtocolCommands
import com.s14p31s207.android.data.comm.tcp.ResponseStatus
import com.s14p31s207.android.data.comm.tcp.ServerMessage
import com.s14p31s207.android.data.db.IpcDevice
import com.s14p31s207.android.data.db.IpcDeviceDao
import com.s14p31s207.android.data.repository.CommandRepositoryImpl
import com.s14p31s207.android.domain.repository.CommandRepository
import com.s14p31s207.android.domain.repository.CommandSequenceRepository
import com.s14p31s207.android.domain.repository.ConnectionChannel
import com.s14p31s207.android.domain.repository.DeviceConnectionRepository
import com.s14p31s207.android.domain.repository.DeviceConnectionState
import com.s14p31s207.android.feature.control.error.ControlErrorManager
import com.s14p31s207.android.feature.control.state.ControlMode
import com.s14p31s207.android.feature.control.state.toControlMode
import com.s14p31s207.android.feature.control.ui.BluetoothDeviceOption
import com.s14p31s207.android.feature.control.ui.ConnectionTab
import com.s14p31s207.android.util.ConnectionPerfTrace
import com.s14p31s207.android.util.PerfLogger
import com.s14p31s207.android.feature.control.ui.WifiDeviceOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle, // SavedStateHandle 주입
    private val ipcDeviceDao: IpcDeviceDao,
    private val deviceConnectionRepository: DeviceConnectionRepository,
    private val bondedDeviceProvider: BluetoothBondedDeviceProvider,
    private val commandRepository: CommandRepository,
    private val commandSequenceRepository: CommandSequenceRepository,
    private val controlErrorManager: ControlErrorManager,
) : AndroidViewModel(application) {

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val KEY_SELECTED_MAC = "selected_mac"
        private const val KEY_SELECTED_IP = "selected_ip"
    }

    private enum class PendingEstopAction {
        ACTIVATE,
        RESET,
    }

    val activeBlockingError: StateFlow<ServerMessage.Error?> =
        controlErrorManager.activeBlockingError
    val warningMessage: StateFlow<String?> = controlErrorManager.warningMessage
    val isResetPending: StateFlow<Boolean> = controlErrorManager.isResetPending
    val isEmergencyStop: StateFlow<Boolean> = controlErrorManager.isEmergencyStop

    private val _motorState = MutableStateFlow<ServerMessage.State?>(null)
    val motorState: StateFlow<ServerMessage.State?> = _motorState.asStateFlow()

    private val _lastResult = MutableStateFlow<ServerMessage.Result?>(null)
    val lastResult: StateFlow<ServerMessage.Result?> = _lastResult.asStateFlow()

    private val _isEstopPending = MutableStateFlow(false)
    val isEstopPending: StateFlow<Boolean> = _isEstopPending.asStateFlow()

    private val _pendingEstopAction = MutableStateFlow<PendingEstopAction?>(null)

    val selectedTab: StateFlow<ConnectionTab> =
        savedStateHandle.getStateFlow(KEY_SELECTED_TAB, ConnectionTab.WIFI)

    private val _selectedProfileMac = savedStateHandle.getStateFlow<String?>(KEY_SELECTED_MAC, null)
    private val _selectedProfileIp = savedStateHandle.getStateFlow<String?>(KEY_SELECTED_IP, null)

    val ipcProfiles = ipcDeviceDao.getAllDevices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val wifiIpcOptions: StateFlow<List<IpcDevice>> =
        ipcProfiles.map { devices ->
            devices.filter { it.ip != null && it.port != null }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bluetoothIpcOptions: StateFlow<List<IpcDevice>> =
        ipcProfiles.map { devices ->
            devices.filter { it.macAddress != null }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ControlViewModel.kt 최상단 프로퍼티 영역에 추가

    val wifiDeviceOptions: StateFlow<List<WifiDeviceOption>> = wifiIpcOptions.map { list ->
        list.map { WifiDeviceOption(name = it.name, ip = it.ip ?: "") }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bluetoothDeviceOptions: StateFlow<List<BluetoothDeviceOption>> =
        bluetoothIpcOptions.map { list ->
            list.map { BluetoothDeviceOption(name = it.name, address = it.macAddress ?: "") }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedProfile: StateFlow<IpcDevice?> = combine(
        ipcProfiles,
        selectedTab,
        _selectedProfileMac,
        _selectedProfileIp
    ) { profiles, tab, mac, ip ->
        when (tab) {
            ConnectionTab.WIFI -> profiles.find { it.ip == ip }
            ConnectionTab.BT -> profiles.find { it.macAddress == mac }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _showDisconnectDialog = MutableStateFlow<ConnectionTab?>(null)
    val showDisconnectDialog: StateFlow<ConnectionTab?> = _showDisconnectDialog.asStateFlow()

    val isDeviceConnected: StateFlow<Boolean> = deviceConnectionRepository.connectionState
        .map { it is DeviceConnectionState.Connected }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Connecting / Reconnecting / SwitchingToBluetooth 모두 "연결 중" 상태로 취급
    val isConnecting: StateFlow<Boolean> = deviceConnectionRepository.connectionState
        .map { state ->
            state is DeviceConnectionState.Connecting
                    || state is DeviceConnectionState.Reconnecting
                    || state is DeviceConnectionState.SwitchingToBluetooth
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _wifiRssi = MutableStateFlow<Int?>(null)
    val wifiRssi: StateFlow<Int?> = _wifiRssi.asStateFlow()

    private val wifiManager =
        application.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager

    // TopBar 아이콘/버튼 색상 전용 — BT 자동 전환 진행 중 여부
    val isSwitchingToBluetooth: StateFlow<Boolean> = deviceConnectionRepository.connectionState
        .map { it is DeviceConnectionState.SwitchingToBluetooth }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // TopBar 버튼 텍스트 및 스낵바 메시지용
    val reconnectingMessage: StateFlow<String?> = deviceConnectionRepository.connectionState
        .map { state ->
            when (state) {
                is DeviceConnectionState.Reconnecting ->
                    "WiFi 재연결 중 (${state.attempt}/${state.maxAttempts})"

                is DeviceConnectionState.SwitchingToBluetooth ->
                    "BT로 자동 전환 중..."

                else -> null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val controlMode: StateFlow<ControlMode> =
        combine(
            _motorState,
            isDeviceConnected
        ) { motorState, isConnected ->
            when {
                !isConnected -> ControlMode.DISCONNECTED
                else -> motorState?.status.toControlMode()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ControlMode.DISCONNECTED
        )

    val estopButtonText: StateFlow<String> =
        combine(controlMode, _isEstopPending) { mode, _ ->
            if (mode == ControlMode.E_STOPPED) "ESTOP RESET" else "ESTOP"
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "ESTOP"
        )

    val isEstopButtonEnabled: StateFlow<Boolean> =
        combine(
            _isEstopPending,
            isDeviceConnected,
            controlErrorManager.isEmergencyStop,
            controlErrorManager.isErrorAcknowledged,
        ) { isPending, isConnected, isEstop, acknowledged ->
            if (!isConnected || isPending) return@combine false
            // 비상정지 에러 상태: 확인(acknowledge) 후에만 ESTOP RESET 버튼 활성화
            if (isEstop) acknowledged
            else true
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isControlEnabled: StateFlow<Boolean> =
        combine(
            controlMode,
            controlErrorManager.activeBlockingError
        ) { mode, blockingError ->
            val isModeValid = mode == ControlMode.IDLE || mode == ControlMode.ERROR
            isModeValid && blockingError == null
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isWheelStopEnabled: StateFlow<Boolean> =
        controlMode.map { mode ->
            mode == ControlMode.MOVING
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isJogStopEnabled: StateFlow<Boolean> =
        controlMode.map { mode ->
            mode == ControlMode.JOGGING
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _deviceToDelete = MutableStateFlow<String?>(null)
    val deviceToDelete: StateFlow<String?> = _deviceToDelete.asStateFlow()

    fun onDeviceLongPressed(name: String) {
        _deviceToDelete.value = name
    }

    fun onDeleteConfirm() {
        val name = _deviceToDelete.value ?: return
        _deviceToDelete.value = null
        viewModelScope.launch {
            ipcDeviceDao.deleteByName(name)
        }
    }

    fun onDeleteCancel() {
        _deviceToDelete.value = null
    }

    val isErrorResetEnabled: StateFlow<Boolean> = combine(
        controlErrorManager.activeBlockingError,
        controlErrorManager.isErrorAcknowledged,
        controlErrorManager.isResetPending,
        controlErrorManager.isEmergencyStop,
    ) { activeError, acknowledged, resetPending, isEstop ->
        activeError != null && acknowledged && !resetPending && !isEstop
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _absolutePosition = MutableStateFlow<Double?>(null)
    val absolutePosition: StateFlow<Double?> = _absolutePosition.asStateFlow()

    private val _targetPosition = MutableStateFlow<Double?>(null)
    val targetPosition: StateFlow<Double?> = _targetPosition.asStateFlow()

    private val _relativeMovement = MutableStateFlow<Double?>(null)
    val relativeMovement: StateFlow<Double?> = _relativeMovement.asStateFlow()

    private val _bondedDevices = MutableStateFlow<List<BluetoothBondedDevice>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothBondedDevice>> = _bondedDevices.asStateFlow()

    private val _selectedBondedDeviceAddress = MutableStateFlow<String?>(null)
    val selectedBondedDeviceAddress: StateFlow<String?> = _selectedBondedDeviceAddress.asStateFlow()
    private val successfulBluetoothConnections = mutableSetOf<String>()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // one-shot 이벤트 — 재연결/BT전환 최종 실패 시 스낵바 메시지 전달
    private val _reconnectFailedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val reconnectFailedEvent: SharedFlow<String> = _reconnectFailedEvent.asSharedFlow()

    init {
        observeServerMessages()
        observeConnectionFailure()
        observeBluetoothFallbackSuccess()
        startRssiPolling()
    }

    private var rssiPollingJob: Job? = null

    private fun startRssiPolling() {
        viewModelScope.launch {
            combine(isDeviceConnected, selectedTab) { connected, tab ->
                connected && tab == ConnectionTab.WIFI
            }.collect { shouldPoll ->
                if (shouldPoll) {
                    rssiPollingJob?.cancel() // 새로 시작 전에 기존 Job 먼저 취소
                    rssiPollingJob = launch {
                        while (true) {
                            delay(3000L)
                            refreshWifiRssi()
                        }
                    }
                } else {
                    rssiPollingJob?.cancel()
                    rssiPollingJob = null
                    _wifiRssi.value = null
                }
            }
        }
    }

    private fun refreshWifiRssi() {
        try {
            @Suppress("DEPRECATION")
            val rssi = wifiManager.connectionInfo.rssi
            _wifiRssi.value = rssi.takeIf { it != -127 }
            Log.d("ControlVM", "WiFi RSSI 갱신: $rssi")
        } catch (e: Exception) {
            Log.e("ControlVM", "RSSI 갱신 실패: ${e.message}")
        }
    }

    private fun observeServerMessages() {
        viewModelScope.launch {
            commandRepository.messages.collect { message ->
                when (message) {
                    is ServerMessage.State -> {
                        _motorState.value = message
                        updateAbsolutePosition(message.position)
                        resolveEstopPending(message.status)
                    }

                    is ServerMessage.Result -> {
                        _lastResult.value = message
                        updateAbsolutePosition(message.position)
                    }

                    is ServerMessage.Response -> {
                        val isResetAccepted = message.status == ResponseStatus.ACCEPTED
                        val isResetCmd = message.cmd == ProtocolCommands.RESET_ERROR
                                || message.cmd == ProtocolCommands.EMERGENCY_STOP_RESET
                        if (isResetCmd && isResetAccepted) {
                            _motorState.update { currentState ->
                                currentState?.copy(status = "IDLE")
                            }
                            if (message.cmd == ProtocolCommands.EMERGENCY_STOP_RESET &&
                                _pendingEstopAction.value == PendingEstopAction.RESET
                            ) {
                                _isEstopPending.value = false
                                _pendingEstopAction.value = null
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }


    private fun observeConnectionFailure() {
        viewModelScope.launch {
            // 1. 초기 상태에 괄호()를 달아서 data class 문법에 맞춥니다.
            var prevState: DeviceConnectionState = DeviceConnectionState.Disconnected()

            deviceConnectionRepository.connectionState.collect { state ->
                when {
                    // 2. state가 Disconnected인지 확인하고, 동시에 state.isManual이 false일 때만 실행합니다.
                    prevState is DeviceConnectionState.Reconnecting
                            && state is DeviceConnectionState.Disconnected
                            && !state.isManual -> {

                        resetControlSessionState()
                        _reconnectFailedEvent.tryEmit("WiFi 재연결에 실패했습니다.")
                    }

                    prevState is DeviceConnectionState.SwitchingToBluetooth
                            && state is DeviceConnectionState.Disconnected
                            && !state.isManual -> {

                        resetControlSessionState()
                        _reconnectFailedEvent.tryEmit("WiFi 재연결 및 BT 자동 전환에 모두 실패했습니다.")
                    }

                    prevState is DeviceConnectionState.Connected
                            && (prevState as? DeviceConnectionState.Connected)?.channel == ConnectionChannel.BLUETOOTH
                            && state is DeviceConnectionState.Disconnected
                            && !state.isManual -> {
                        resetControlSessionState()
                        _reconnectFailedEvent.tryEmit("블루투스 통신이 끊어졌습니다.")
                    }
                }
                prevState = state
            }
        }
    }

    /**
     * BT 자동 전환 성공 시 처리
     *
     * SwitchingToBluetooth → Connected(BLUETOOTH) 전환 감지 시:
     * - UI 탭을 BT로 동기화
     * - CONNECT 명령 전송
     */
    private fun observeBluetoothFallbackSuccess() {
        viewModelScope.launch {
            var prevState: DeviceConnectionState = DeviceConnectionState.Disconnected()
            deviceConnectionRepository.connectionState.collect { state ->
                if (prevState is DeviceConnectionState.SwitchingToBluetooth
                    && state is DeviceConnectionState.Connected
                    && state.channel == ConnectionChannel.BLUETOOTH
                ) {
                    // UI 탭 BT로 동기화
                    savedStateHandle[KEY_SELECTED_TAB] = ConnectionTab.BT
                    savedStateHandle[KEY_SELECTED_MAC] = state.device.macAddress
                    (commandRepository as? CommandRepositoryImpl)?.isWifiMode = false

                    // 서버에 CONNECT 명령 전송
                    sendCommandWithId { id -> CommandFactory.connect(id, Channel.BLUETOOTH) }
                        .onFailure {
                            Log.d("ControlVM", "BT fallback CONNECT cmd failed: ${it.message}")
                        }
                }
                prevState = state
            }
        }
    }

    fun onTabSelected(tab: ConnectionTab) {
        if (selectedTab.value == tab) return

        val currentProfile =
            (deviceConnectionRepository.connectionState.value as? DeviceConnectionState.Connected)?.device

        if (isDeviceConnected.value && currentProfile != null) {
            val isValidForNewTab = when (tab) {
                ConnectionTab.WIFI -> currentProfile.ip != null && currentProfile.port != null
                ConnectionTab.BT -> currentProfile.macAddress != null
            }

            if (isValidForNewTab) {
                savedStateHandle[KEY_SELECTED_TAB] = tab
                (commandRepository as? CommandRepositoryImpl)?.isWifiMode =
                    (tab == ConnectionTab.WIFI)
                performSeamlessHandover(currentProfile, tab)
            } else {
                _showDisconnectDialog.value = tab
            }
        } else {
            savedStateHandle[KEY_SELECTED_TAB] = tab
            (commandRepository as? CommandRepositoryImpl)?.isWifiMode = (tab == ConnectionTab.WIFI)
            validateProfileForTab(tab)
        }
    }

    private fun performSeamlessHandover(profile: IpcDevice, newTab: ConnectionTab) {
        viewModelScope.launch {
            deviceConnectionRepository.disconnect()

            val newChannel =
                if (newTab == ConnectionTab.WIFI) ConnectionChannel.WIFI else ConnectionChannel.BLUETOOTH

            deviceConnectionRepository.connect(profile, newChannel)
                .onSuccess {
                    val tcpChannel =
                        if (newTab == ConnectionTab.WIFI) Channel.WIFI else Channel.BLUETOOTH
                    sendCommandWithId { id -> CommandFactory.connect(id, tcpChannel) }
                }
                .onFailure { error ->
                    _errorMessage.value = "채널 전환 연결 실패: ${error.message}"
                    resetControlSessionState()
                }
        }
    }

    fun onDisconnectConfirm() {
        val targetTab = _showDisconnectDialog.value ?: return
        viewModelScope.launch {
            deviceConnectionRepository.disconnect()
            resetControlSessionState()

            savedStateHandle[KEY_SELECTED_TAB] = targetTab
            savedStateHandle[KEY_SELECTED_MAC] = null
            savedStateHandle[KEY_SELECTED_IP] = null
            (commandRepository as? CommandRepositoryImpl)?.isWifiMode =
                (targetTab == ConnectionTab.WIFI)
            _selectedBondedDeviceAddress.value = null
            _showDisconnectDialog.value = null
        }
    }

    fun onDisconnectCancel() {
        _showDisconnectDialog.value = null
    }

    private fun validateProfileForTab(tab: ConnectionTab) {
        val profile = selectedProfile.value ?: return
        val isValid = when (tab) {
            ConnectionTab.WIFI -> profile.ip != null && profile.port != null
            ConnectionTab.BT -> profile.macAddress != null
        }
        if (!isValid) {
            savedStateHandle[KEY_SELECTED_MAC] = null
            savedStateHandle[KEY_SELECTED_IP] = null
            _selectedBondedDeviceAddress.value = null
        }
    }

    fun moveAbsolute(position: Double, speed: Double) {
        sendCommand(
            onSuccess = { setTargetPosition(position) }
        ) { id -> CommandFactory.moveAbsolute(id, position, speed) }
    }

    fun moveRelative(direction: Direction, distance: Double, speed: Double) {
        val currentPosition = _absolutePosition.value ?: 0.0
        val signedDistance = when (direction) {
            Direction.POS -> distance
            Direction.NEG -> -distance
        }
        val nextTargetPosition = currentPosition + signedDistance
        sendCommand(
            onSuccess = { setTargetPosition(nextTargetPosition) }
        ) { id -> CommandFactory.moveRelative(id, direction, distance, speed) }
    }

    fun jogStart(direction: Direction) {
        sendCommand(
            onSuccess = { clearTargetPosition() }
        ) { id -> CommandFactory.jogStart(id, direction) }
    }

    fun jogStop() {
        sendCommand(
            onSuccess = { clearTargetPosition() }
        ) { id -> CommandFactory.jogStop(id) }
    }

    fun moveHome() {
        sendCommand(
            onSuccess = { setTargetPosition(0.0) }
        ) { id -> CommandFactory.moveHome(id) }
    }

    fun resetHome() {
        sendCommand(
            onSuccess = { setTargetPosition(0.0) }
        ) { id -> CommandFactory.resetHome(id) }
    }

    fun stopWheel() {
        sendCommand { id -> CommandFactory.stop(id) }
    }

    fun onEstopClicked() {
        if (_isEstopPending.value) return

        val isEmergencyStopped = controlMode.value == ControlMode.E_STOPPED
        val pendingAction = if (isEmergencyStopped) {
            PendingEstopAction.RESET
        } else {
            PendingEstopAction.ACTIVATE
        }
        val buildCommand: (String) -> CommandDto = if (isEmergencyStopped) {
            CommandFactory::emergencyStopReset
        } else {
            CommandFactory::emergencyStop
        }

        _isEstopPending.value = true
        _pendingEstopAction.value = pendingAction

        viewModelScope.launch {
            sendCommandWithId(buildCommand)
                .onSuccess {
                    if (!isEmergencyStopped) {
                        clearTargetPosition()
                    }
                }
                .onFailure {
                    _isEstopPending.value = false
                    _pendingEstopAction.value = null
                    _errorMessage.value = "Failed to send command."
                }
        }
    }

    private fun sendCommand(
        onSuccess: (() -> Unit)? = null,
        buildCommand: (String) -> CommandDto
    ) {
        viewModelScope.launch {
            sendCommandWithId(buildCommand)
                .onSuccess { onSuccess?.invoke() }
                .onFailure {
                    _errorMessage.value = "명령 전송에 실패했습니다."
                }
        }
    }

    private suspend fun sendCommandWithId(
        buildCommand: (String) -> CommandDto
    ): Result<Unit> {
        return try {
            val id = commandSequenceRepository.nextId()
            commandRepository.send(buildCommand(id))
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    fun onProfileSelected(profile: IpcDevice) {
        profile.macAddress?.let { savedStateHandle[KEY_SELECTED_MAC] = it }
        profile.ip?.let { savedStateHandle[KEY_SELECTED_IP] = it }

        profile.macAddress?.let { address ->
            _selectedBondedDeviceAddress.value = address
        }
    }

    fun refreshBondedDevices() {
        bondedDeviceProvider.getBondedDevices()
            .onSuccess { devices ->
                _bondedDevices.value = devices
                _errorMessage.value = null
                val selectedAddress = _selectedBondedDeviceAddress.value
                if (selectedAddress == null || devices.none { it.address == selectedAddress }) {
                    _selectedBondedDeviceAddress.value = devices.firstOrNull()?.address
                }
            }
            .onFailure { error ->
                _bondedDevices.value = emptyList()
                _selectedBondedDeviceAddress.value = null
                _errorMessage.value = error.message ?: "블루투스 기기 목록을 불러오지 못했습니다."
            }
    }

    fun onBtDeviceSelected(address: String) {
        _selectedBondedDeviceAddress.value = address
    }

    fun onConnectClicked() {
        Log.d("ControlVM", "onConnectClicked called")
        val profile = selectedProfile.value

        if (profile == null) {
            _errorMessage.value = "연결할 IPC 기기를 선택해주세요."
            return
        }

        val channel = if (selectedTab.value == ConnectionTab.WIFI) {
            ConnectionChannel.WIFI
        } else {
            ConnectionChannel.BLUETOOTH
        }
        val perfCategory = resolveConnectionPerfCategory(profile, channel)
        val perfStartedAt = System.currentTimeMillis()
        val perfTrace = ConnectionPerfTrace(
            category = perfCategory,
            attemptId = buildConnectionAttemptId(perfCategory, perfStartedAt),
            startedAt = perfStartedAt
        )
        val target = when (channel) {
            ConnectionChannel.WIFI -> "${profile.ip}:${profile.port}"
            ConnectionChannel.BLUETOOTH -> profile.macAddress ?: "unknown"
        }

        viewModelScope.launch {
            _errorMessage.value = null
            resetControlSessionState()
            PerfLogger.d(
                "[${perfTrace.category}][START] attemptId=${perfTrace.attemptId} " +
                    "target=$target ts=${perfTrace.startedAt}"
            )

            deviceConnectionRepository.connect(profile, channel, perfTrace)
                .onSuccess {
                    Log.d("ControlVM", "connect success via $channel")
                    if (channel == ConnectionChannel.BLUETOOTH) {
                        profile.macAddress?.let { successfulBluetoothConnections.add(it) }
                    }
                    val tcpChannel =
                        if (channel == ConnectionChannel.WIFI) Channel.WIFI else Channel.BLUETOOTH
                    sendCommandWithId { id -> CommandFactory.connect(id, tcpChannel) }
                        .onFailure { Log.d("ControlVM", "CONNECT cmd failed: ${it.message}") }
                }
                .onFailure { error ->
                    Log.d("ControlVM", "connect failed: ${error.message}")
                    _errorMessage.value = error.message ?: "연결에 실패했습니다."
                }
        }
    }

    private fun resolveConnectionPerfCategory(
        profile: IpcDevice,
        channel: ConnectionChannel
    ): String {
        return when (channel) {
            ConnectionChannel.WIFI -> "CONN_WIFI"
            ConnectionChannel.BLUETOOTH -> {
                val macAddress = profile.macAddress
                if (macAddress != null && successfulBluetoothConnections.contains(macAddress)) {
                    "CONN_BT_RECONNECT"
                } else {
                    "CONN_BT_FIRST"
                }
            }
        }
    }

    private fun buildConnectionAttemptId(category: String, startedAt: Long): String {
        return category.lowercase() + "-$startedAt"
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearWarningMessage() {
        controlErrorManager.clearWarningMessage()
    }

    fun onErrorResetClicked() {
        viewModelScope.launch {
            controlErrorManager.requestErrorReset()
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "에러 리셋 명령 전송에 실패했습니다."
                }
        }
    }

    private fun resolveEstopPending(status: String) {
        when (_pendingEstopAction.value) {
            PendingEstopAction.ACTIVATE -> {
                if (status.toControlMode() == ControlMode.E_STOPPED) {
                    _isEstopPending.value = false
                    _pendingEstopAction.value = null
                }
            }

            PendingEstopAction.RESET -> {
                if (status.toControlMode() != ControlMode.E_STOPPED) {
                    _isEstopPending.value = false
                    _pendingEstopAction.value = null
                }
            }

            null -> Unit
        }
    }

    private fun updateAbsolutePosition(position: Double?) {
        if (position == null) return
        _absolutePosition.value = position
        recalculateRelativeMovement()
    }

    private fun setTargetPosition(position: Double) {
        _targetPosition.value = position
        recalculateRelativeMovement()
    }

    private fun clearTargetPosition() {
        _targetPosition.value = null
        _relativeMovement.value = null
    }

    private fun recalculateRelativeMovement() {
        val currentPosition = _absolutePosition.value
        val targetPosition = _targetPosition.value
        _relativeMovement.value =
            if (currentPosition != null && targetPosition != null) {
                targetPosition - currentPosition
            } else {
                null
            }
    }

    fun onDisconnectClicked() {
        viewModelScope.launch {
            resetControlSessionState()
            deviceConnectionRepository.disconnect()
        }
    }

    private fun resetControlSessionState() {
        controlErrorManager.clearSessionState()
        _motorState.value = null
        _lastResult.value = null
        _isEstopPending.value = false
        _pendingEstopAction.value = null
        _absolutePosition.value = null
        _targetPosition.value = null
        _relativeMovement.value = null
    }
}
