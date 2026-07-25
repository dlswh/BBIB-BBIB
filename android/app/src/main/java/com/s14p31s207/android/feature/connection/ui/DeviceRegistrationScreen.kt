package com.s14p31s207.android.feature.connection.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s14p31s207.android.app.navigation.NavMenu
import com.s14p31s207.android.app.navigation.SidebarNav
import com.s14p31s207.android.data.comm.bluetooth.BluetoothPairingResult
import com.s14p31s207.android.data.comm.bluetooth.BluetoothScanDevice
import com.s14p31s207.android.data.comm.bluetooth.BluetoothScanState
import com.s14p31s207.android.feature.connection.vm.DeviceRegistrationUiState
import com.s14p31s207.android.feature.connection.vm.WifiTestState

@Composable
fun DeviceRegistrationScreen(
    uiState: DeviceRegistrationUiState,
    scanState: BluetoothScanState,
    snackbarHostState: SnackbarHostState,
    canSave: Boolean,
    onNavSelected: (NavMenu) -> Unit,
    onNameChanged: (String) -> Unit,
    onIpChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onTestWifi: () -> Unit,
    onSkipWifi: () -> Unit,
    onStartBluetoothScan: () -> Unit,
    onStopBluetoothScan: () -> Unit,
    onPairDevice: (String) -> Boolean,
    onClearPairingResult: () -> Unit,
    onSelectDevice: (BluetoothScanDevice) -> Unit,   // 짧은 탭 선택
    onPairingSuccess: (BluetoothPairingResult) -> Unit, // 페어링 후 선택
    onSkipBluetooth: () -> Unit,
    onLogout: () -> Unit = {},
    username: String = "",
    onSave: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SidebarNav(
                selectedNav = NavMenu.CONNECTION,
                onNavSelected = onNavSelected,
                onLogout = onLogout,
                username = username,
                modifier = Modifier.fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. 헤더: 이름 설정 영역
                Column {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "NAME",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(100.dp)
                        )
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = onNameChanged,
                            placeholder = { Text("Enter device name...") },
                            singleLine = true,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.width(400.dp)
                        )
                    }
                }

                // 2. 메인: WiFi / Bluetooth 영역
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    SimpleSection(title = "WIFI CONNECTION", modifier = Modifier.weight(1f)) {
                        WifiContent(
                            uiState = uiState,
                            onIpChanged = onIpChanged,
                            onPortChanged = onPortChanged,
                            onTestWifi = onTestWifi,
                            onSkipWifi = onSkipWifi
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    SimpleSection(title = "BLUETOOTH PAIRING", modifier = Modifier.weight(1f)) {
                        BluetoothContent(
                            scanState = scanState,
                            isSaving = uiState.isSaving,
                            selectedMacAddress = uiState.pendingMacAddress,
                            onStartScan = onStartBluetoothScan,
                            onStopScan = onStopBluetoothScan,
                            onPairDevice = onPairDevice,
                            onClearPairingResult = onClearPairingResult,
                            onSelectDevice = onSelectDevice,
                            onPairingSuccess = onPairingSuccess,
                            onSkip = onSkipBluetooth
                        )
                    }
                }

                // 3. 하단: 저장 버튼
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onSave,
                            enabled = canSave && !uiState.isSaving,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .width(160.dp)
                                .height(56.dp)
                        ) {
                            Text(
                                text = if (uiState.isSaving) "SAVING..." else "SAVE",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        content()
    }
}

@Composable
private fun WifiContent(
    uiState: DeviceRegistrationUiState,
    onIpChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onTestWifi: () -> Unit,
    onSkipWifi: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SimpleLabelField(label = "IP ADDRESS", value = uiState.ip, onValueChange = onIpChanged)
        SimpleLabelField(label = "PORT", value = uiState.port, onValueChange = onPortChanged)

        Spacer(modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onSkipWifi,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("SKIP")
            }
            Button(
                onClick = onTestWifi,
                enabled = uiState.wifiTestState != WifiTestState.TESTING && uiState.ip.isNotBlank(),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(if (uiState.wifiTestState == WifiTestState.TESTING) "TESTING..." else "CONNECT TEST")
            }
        }
    }
}

@Composable
private fun BluetoothContent(
    scanState: BluetoothScanState,
    isSaving: Boolean,
    selectedMacAddress: String?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onPairDevice: (String) -> Boolean,
    onClearPairingResult: () -> Unit,
    onSelectDevice: (BluetoothScanDevice) -> Unit,
    onPairingSuccess: (BluetoothPairingResult) -> Unit,
    onSkip: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            BluetoothStepBody(
                scanState = scanState,
                isSaving = isSaving,
                selectedMacAddress = selectedMacAddress,
                onStartScan = onStartScan,
                onStopScan = onStopScan,
                onPairDevice = onPairDevice,
                onClearPairingResult = onClearPairingResult,
                onSelectDevice = onSelectDevice,
                onPairingSuccess = onPairingSuccess,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onSkip,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            // 선택된 기기가 있으면 UNPAIR, 없으면 SKIP
            Text(if (selectedMacAddress != null) "UNPAIR" else "SKIP")
        }
    }
}

@Composable
private fun SimpleLabelField(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
    }
}