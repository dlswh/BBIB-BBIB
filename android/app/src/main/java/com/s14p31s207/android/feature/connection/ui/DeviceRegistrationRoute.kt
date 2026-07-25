package com.s14p31s207.android.feature.connection.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.s14p31s207.android.app.navigation.NavMenu
import com.s14p31s207.android.feature.auth.session.SessionState
import com.s14p31s207.android.feature.connection.vm.DeviceRegistrationViewModel

@Composable
fun DeviceRegistrationRoute(
    viewModel: DeviceRegistrationViewModel = hiltViewModel(),
    onNavSelected: (NavMenu) -> Unit,
    onLogout: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val canSave = uiState.name.isNotBlank() &&
            ((uiState.confirmedIp != null && uiState.confirmedPort != null) ||
                    uiState.pendingMacAddress != null)

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(
                message = "저장되었습니다.",
                duration = SnackbarDuration.Short
            )
            viewModel.clearSaveSuccess()
            onSaved()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    DeviceRegistrationScreen(
        uiState = uiState,
        scanState = scanState,
        snackbarHostState = snackbarHostState,
        canSave = canSave,
        onNavSelected = onNavSelected,
        onNameChanged = viewModel::onNameChanged,
        onIpChanged = viewModel::onIpChanged,
        onPortChanged = viewModel::onPortChanged,
        onTestWifi = viewModel::testWifiConnection,
        onSkipWifi = viewModel::skipWifi,
        onStartBluetoothScan = viewModel::startDiscovery,
        onStopBluetoothScan = viewModel::stopDiscovery,
        onPairDevice = viewModel::pairDevice,
        onClearPairingResult = viewModel::clearPairingResult,
        onSelectDevice = viewModel::onSelectDevice,       // 짧은 탭 선택
        onPairingSuccess = viewModel::onPairingSuccess,   // 페어링 완료 후 선택
        onSkipBluetooth = viewModel::skipBluetooth,
        onLogout = onLogout,
        username = SessionState.name,
        onSave = viewModel::save
    )
}