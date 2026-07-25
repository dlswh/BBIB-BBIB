package com.s14p31s207.android.feature.control.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.s14p31s207.android.app.navigation.NavMenu
import com.s14p31s207.android.app.navigation.SidebarNav
import com.s14p31s207.android.data.comm.tcp.Direction
import com.s14p31s207.android.feature.control.ui.component.ControlSection
import com.s14p31s207.android.feature.control.ui.component.HomeSection
import com.s14p31s207.android.feature.control.ui.component.JogSection
import com.s14p31s207.android.feature.control.ui.component.TopBar
import com.s14p31s207.android.ui.theme.AndroidTheme
import java.text.DecimalFormat

enum class ConnectionTab { WIFI, BT }

private val RightPanelWidth = 332.dp
private val ControlPanelGap = 40.dp
private val RightPanelGap = 6.dp
private val HomeJogSectionGap = 27.dp

data class BluetoothDeviceOption(
    val name: String,
    val address: String
) {
    val label: String get() = "$name ($address)"
}

data class WifiDeviceOption(
    val name: String,
    val ip: String
) {
    val label: String get() = "$name ($ip)"
}

@Composable
fun ControlScreen(
    modifier: Modifier = Modifier,
    selectedTab: ConnectionTab = ConnectionTab.WIFI,
    ipcOptions: List<WifiDeviceOption> = emptyList(),
    selectedIpc: String = "",
    btDevices: List<BluetoothDeviceOption> = emptyList(),
    selectedBtDeviceAddress: String? = null,
    onIpcLongPressed: (String) -> Unit = {},
    deviceToDelete: String? = null,
    onDeleteConfirm: () -> Unit = {},
    onDeleteCancel: () -> Unit = {},
    isConnected: Boolean = false,
    isConnecting: Boolean = false,
    isSwitchingToBluetooth: Boolean = false,
    reconnectingMessage: String? = null,
    relativeMovement: Double? = null,
    absolutePosition: Double? = null,
    estopButtonText: String = "ESTOP",
    isEstopButtonEnabled: Boolean = true,
    isErrorResetEnabled: Boolean = false,
    isErrorResetPending: Boolean = false,
    isControlEnabled: Boolean = true,
    isWheelStopEnabled: Boolean = false,
    isJogStopEnabled: Boolean = false,
    selectedNav: NavMenu = NavMenu.CONTROL,
    onNavSelected: (NavMenu) -> Unit = {},
    onTabSelected: (ConnectionTab) -> Unit = {},
    onIpcSelected: (String) -> Unit = {},
    onBtDeviceSelected: (String) -> Unit = {},
    onConnect: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    onSpeedChanged: (String) -> Unit = {},
    onAbsolutePositionChanged: (String) -> Unit = {},
    onAbsoluteMove: (position: Double, speed: Double) -> Unit = { _, _ -> },
    onRelativeDistanceChanged: (String) -> Unit = {},
    onRelativeMove: (direction: Direction, distance: Double, speed: Double) -> Unit = { _, _, _ -> },
    onCcw: () -> Unit = {},
    onCw: () -> Unit = {},
    onMoveHome: () -> Unit = {},
    onResetHome: () -> Unit = {},
    onHomeLeft: () -> Unit = {},
    onHomeRight: () -> Unit = {},
    onStop: () -> Unit = {},
    onJogStop: () -> Unit = {},
    onEstop: () -> Unit = {},
    onErrorReset: () -> Unit = {},
    wifiRssi: Int? = null,
    onLogout: () -> Unit = {},
    username: String = "",
) {
    var localSelectedIpc by rememberSaveable(selectedIpc) { mutableStateOf(selectedIpc) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedBtDevice = btDevices.firstOrNull { it.address == selectedBtDeviceAddress }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        SidebarNav(
            selectedNav = selectedNav,
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
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
        ) {
            TopBar(
                selectedTab = selectedTab,
                isConnected = isConnected,
                isConnecting = isConnecting,
                isSwitchingToBluetooth = isSwitchingToBluetooth,
                reconnectingMessage = reconnectingMessage,
                selectedIpc = localSelectedIpc,
                ipcOptions = ipcOptions,
                btDevices = btDevices,
                selectedBtDevice = selectedBtDevice,
                dropdownExpanded = dropdownExpanded,
                onTabSelected = { tab ->
                    localSelectedIpc = ""
                    dropdownExpanded = false
                    onTabSelected(tab)
                },
                onDropdownClick = { dropdownExpanded = true },
                onDropdownDismiss = { dropdownExpanded = false },
                onIpcSelected = { option ->
                    localSelectedIpc = option
                    dropdownExpanded = false
                    onIpcSelected(option)
                },
                onBtDeviceSelected = { address ->
                    dropdownExpanded = false
                    onBtDeviceSelected(address)
                },
                onIpcLongPressed = onIpcLongPressed,
                onConnect = onConnect,
                onDisconnect = {
                    localSelectedIpc = ""
                    onDisconnect()
                },
                wifiRssi = wifiRssi,
            )
            if (deviceToDelete != null) {
                AlertDialog(
                    onDismissRequest = onDeleteCancel,
                    title = { Text("기기 삭제") },
                    text = { Text("'$deviceToDelete'를 삭제하시겠습니까?\nWiFi와 Bluetooth 정보가 모두 삭제됩니다.") },
                    confirmButton = {
                        TextButton(onClick = onDeleteConfirm) {
                            Text("삭제", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDeleteCancel) { Text("취소") }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "현재 위치 : ${formatDistanceCm(absolutePosition)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "상대 이동 거리 : ${formatSignedDistanceCm(relativeMovement)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(24.dp))
                Button(
                    onClick = onErrorReset,
                    enabled = isErrorResetEnabled,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .width(160.dp)
                        .height(56.dp)
                ) {
                    Text(
                        text = if (isErrorResetPending) "RESETTING..." else "ERROR RESET",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onEstop,
                    enabled = isEstopButtonEnabled,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .width(160.dp)
                        .height(56.dp)
                ) {
                    Text(
                        estopButtonText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                ControlSection(
                    modifier = Modifier.weight(1f),
                    commandEnabled = isControlEnabled,
                    stopEnabled = isWheelStopEnabled,
                    onSpeedChanged = onSpeedChanged,
                    onAbsolutePositionChanged = onAbsolutePositionChanged,
                    onAbsoluteMove = onAbsoluteMove,
                    onRelativeDistanceChanged = onRelativeDistanceChanged,
                    onRelativeMove = onRelativeMove,
                    onCcw = onCcw,
                    onCw = onCw,
                    onStop = onStop,
                )

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = ControlPanelGap / 2),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Column(
                    modifier = Modifier.width(RightPanelWidth),
                    verticalArrangement = Arrangement.spacedBy(RightPanelGap),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "HOME",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    HomeSection(
                        enabled = isControlEnabled,
                        onMoveHome = onMoveHome,
                        onResetHome = onResetHome
                    )
                    Spacer(modifier = Modifier.height(HomeJogSectionGap))
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(HomeJogSectionGap))
                    Text(
                        text = "JOG",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    JogSection(
                        modifier = Modifier,
                        directionEnabled = isControlEnabled,
                        jogStopEnabled = isJogStopEnabled,
                        onHomeLeft = onHomeLeft,
                        onHomeRight = onHomeRight,
                        onJogStop = onJogStop,
                    )
                }
            }
        }
    }
}

private fun formatDistanceCm(value: Double?): String {
    return value?.let { "${distanceFormat.format(it)}cm" } ?: "-cm"
}

private fun formatSignedDistanceCm(value: Double?): String {
    return value?.let {
        val sign = if (it > 0) "+" else ""
        "$sign${distanceFormat.format(it)}cm"
    } ?: "-cm"
}

private val distanceFormat = DecimalFormat("0.##")

@Preview(showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ControlScreenPreview() {
    AndroidTheme {
        Surface {
            ControlScreen(
                absolutePosition = 20.0,
                relativeMovement = 30.0,
                ipcOptions = listOf(
                    WifiDeviceOption("IPC-01", "192.168.0.1"),
                    WifiDeviceOption("IPC-02", "192.168.0.2"),
                ),
                btDevices = listOf(
                    BluetoothDeviceOption("Motor Controller", "00:11:22:33:44:55"),
                    BluetoothDeviceOption("Wheel Unit", "AA:BB:CC:DD:EE:FF")
                )
            )
        }
    }
}
