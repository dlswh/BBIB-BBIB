package com.s14p31s207.android.feature.control.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.s14p31s207.android.app.navigation.NavMenu
import com.s14p31s207.android.data.comm.tcp.Direction
import com.s14p31s207.android.feature.auth.session.SessionState
import com.s14p31s207.android.feature.control.vm.ControlViewModel

@Composable
fun ControlRoute(
    viewModel: ControlViewModel = hiltViewModel(),
    onNavSelected: (NavMenu) -> Unit,
    onLogout: () -> Unit
) {
    // 1. 기존 원본 리스트 (장비 선택 콜백에서 원본 데이터를 찾을 때 유지)
    val wifiIpcOptions by viewModel.wifiIpcOptions.collectAsStateWithLifecycle()
    val bluetoothIpcOptions by viewModel.bluetoothIpcOptions.collectAsStateWithLifecycle()

    // 2. 🚀 성능 최적화: ViewModel에서 미리 가공해둔 UI 전용 리스트 구독
    val wifiDeviceOptions by viewModel.wifiDeviceOptions.collectAsStateWithLifecycle()
    val bluetoothDeviceOptions by viewModel.bluetoothDeviceOptions.collectAsStateWithLifecycle()

    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val warningMessage by viewModel.warningMessage.collectAsStateWithLifecycle()
    val isResetPending by viewModel.isResetPending.collectAsStateWithLifecycle()
    val isErrorResetEnabled by viewModel.isErrorResetEnabled.collectAsStateWithLifecycle()
    val absolutePosition by viewModel.absolutePosition.collectAsStateWithLifecycle()
    val relativeMovement by viewModel.relativeMovement.collectAsStateWithLifecycle()
    val estopButtonText by viewModel.estopButtonText.collectAsStateWithLifecycle()
    val isEstopButtonEnabled by viewModel.isEstopButtonEnabled.collectAsStateWithLifecycle()
    val isControlEnabled by viewModel.isControlEnabled.collectAsStateWithLifecycle()
    val isWheelStopEnabled by viewModel.isWheelStopEnabled.collectAsStateWithLifecycle()
    val isJogStopEnabled by viewModel.isJogStopEnabled.collectAsStateWithLifecycle()
    val wifiRssi by viewModel.wifiRssi.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val showDisconnectDialog by viewModel.showDisconnectDialog.collectAsStateWithLifecycle()

    val isConnected by viewModel.isDeviceConnected.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val isSwitchingToBluetooth by viewModel.isSwitchingToBluetooth.collectAsStateWithLifecycle()
    val reconnectingMessage by viewModel.reconnectingMessage.collectAsStateWithLifecycle()
    val deviceToDelete by viewModel.deviceToDelete.collectAsStateWithLifecycle()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    LaunchedEffect(warningMessage) {
        warningMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearWarningMessage()
        }
    }

    LaunchedEffect(reconnectingMessage) {
        reconnectingMessage?.let {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Indefinite)
        } ?: run {
            // null로 바뀐 이유가 성공인지 실패인지 모르므로
            // dismiss는 reconnectFailedEvent collector 또는 성공 경로에서만 처리
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reconnectFailedEvent.collect { message ->
            // 재연결 중 스낵바(Indefinite) 먼저 닫고 실패 메시지 표시
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    // 재연결 성공 시 (isConnected가 다시 true로) 스낵바 닫기
    LaunchedEffect(isConnected) {
        if (isConnected) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    showDisconnectDialog?.let { targetTab ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::onDisconnectCancel,
            title = { Text("연결 해제 확인") },
            text = { Text("선택한 기기는 ${targetTab.name} 정보를 가지고 있지 않습니다. 이동을 위해 현재 연결을 끊고 기기를 정지하시겠습니까?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::onDisconnectConfirm) {
                    Text("확인")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::onDisconnectCancel) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        ControlScreen(
            modifier = Modifier.padding(innerPadding),
            selectedTab = selectedTab,

            // 🚀 수정됨: UI가 재구성(Recomposition)될 때마다 무거운 반복문(map)을 돌리지 않고 즉시 주입
            ipcOptions = wifiDeviceOptions,
            btDevices = bluetoothDeviceOptions,

            selectedIpc = if (selectedTab == ConnectionTab.WIFI) selectedProfile?.name
                ?: "" else "",
            selectedBtDeviceAddress = if (selectedTab == ConnectionTab.BT) selectedProfile?.macAddress else null,
            deviceToDelete = deviceToDelete,
            onIpcLongPressed = viewModel::onDeviceLongPressed,
            onDeleteConfirm = viewModel::onDeleteConfirm,
            onDeleteCancel = viewModel::onDeleteCancel,
            absolutePosition = absolutePosition,
            relativeMovement = relativeMovement,
            estopButtonText = estopButtonText,
            isEstopButtonEnabled = isEstopButtonEnabled,
            isErrorResetEnabled = isErrorResetEnabled,
            isErrorResetPending = isResetPending,
            isControlEnabled = isControlEnabled,
            isWheelStopEnabled = isWheelStopEnabled,
            isJogStopEnabled = isJogStopEnabled,
            isConnected = isConnected,
            isConnecting = isConnecting,
            isSwitchingToBluetooth = isSwitchingToBluetooth,
            reconnectingMessage = reconnectingMessage,
            onNavSelected = onNavSelected,
            onTabSelected = viewModel::onTabSelected,

            // 기존 기능 유지: 선택했을 땐 원본 리스트(wifiIpcOptions 등)에서 찾아 통신에 활용
            onIpcSelected = { name ->
                wifiIpcOptions.find { it.name == name }?.let { viewModel.onProfileSelected(it) }
            },
            onBtDeviceSelected = { address ->
                bluetoothIpcOptions.find { it.macAddress == address }
                    ?.let { viewModel.onProfileSelected(it) }
            },
            onConnect = viewModel::onConnectClicked,
            onDisconnect = viewModel::onDisconnectClicked,
            onAbsoluteMove = { position, speed ->
                viewModel.moveAbsolute(position, speed)
            },
            onRelativeMove = { direction, distance, speed ->
                viewModel.moveRelative(direction, distance, speed)
            },
            onMoveHome = viewModel::moveHome,
            onResetHome = viewModel::resetHome,
            onHomeLeft = { viewModel.jogStart(Direction.NEG) },
            onHomeRight = { viewModel.jogStart(Direction.POS) },
            onStop = viewModel::stopWheel,
            onJogStop = viewModel::jogStop,
            onEstop = viewModel::onEstopClicked,
            onErrorReset = viewModel::onErrorResetClicked,
            wifiRssi = wifiRssi,
            onLogout = onLogout,
            username = SessionState.name,
        )
    }
}