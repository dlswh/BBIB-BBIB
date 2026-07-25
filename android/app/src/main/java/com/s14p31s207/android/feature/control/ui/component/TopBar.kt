package com.s14p31s207.android.feature.control.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.s14p31s207.android.feature.control.ui.BluetoothDeviceOption
import com.s14p31s207.android.feature.control.ui.ConnectionTab
import com.s14p31s207.android.feature.control.ui.WifiDeviceOption

private val ActionButtonWidth = 160.dp
private val ActionButtonHeight = 56.dp
private val ActionButtonGap = 12.dp
private val TopBarGap = 20.dp

@Composable
fun TopBar(
    selectedTab: ConnectionTab,
    isConnected: Boolean,
    isConnecting: Boolean,
    isSwitchingToBluetooth: Boolean = false,
    reconnectingMessage: String? = null,
    selectedIpc: String,
    ipcOptions: List<WifiDeviceOption>,
    btDevices: List<BluetoothDeviceOption>,
    selectedBtDevice: BluetoothDeviceOption?,
    dropdownExpanded: Boolean,
    onTabSelected: (ConnectionTab) -> Unit,
    onDropdownClick: () -> Unit,
    onDropdownDismiss: () -> Unit,
    onIpcSelected: (String) -> Unit,
    onBtDeviceSelected: (String) -> Unit,
    onIpcLongPressed: (String) -> Unit = {},
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    wifiRssi: Int? = null,
) {
    val dropdownLabel = when (selectedTab) {
        ConnectionTab.BT -> selectedBtDevice?.label ?: "기기를 선택하시오"
        ConnectionTab.WIFI -> ipcOptions.find { it.name == selectedIpc }?.label ?: "기기를 선택하시오"
    }
    val connectEnabled = !isConnecting && when (selectedTab) {
        ConnectionTab.WIFI -> isConnected || selectedIpc.isNotBlank()
        ConnectionTab.BT -> isConnected || selectedBtDevice != null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .width(240.dp)
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabButton(
                    label = "WIFI",
                    selected = selectedTab == ConnectionTab.WIFI,
                    onClick = { onTabSelected(ConnectionTab.WIFI) },
                    modifier = Modifier.weight(1f)
                )
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                TabButton(
                    label = "BLUETOOTH",
                    selected = selectedTab == ConnectionTab.BT,
                    onClick = { onTabSelected(ConnectionTab.BT) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(TopBarGap))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedTab == ConnectionTab.WIFI && !isSwitchingToBluetooth && isConnected && wifiRssi != null) {
                    WifiSignalIcon(rssi = wifiRssi)
                    Text("${wifiRssi}dBm", style = MaterialTheme.typography.bodySmall)
                } else {
                    Icon(
                        imageVector = if (isSwitchingToBluetooth || selectedTab == ConnectionTab.BT) {
                            Icons.Default.Bluetooth
                        } else {
                            Icons.Default.Wifi
                        },
                        contentDescription = null,
                        tint = when {
                            isConnected -> Color(0xFF4CAF50)
                            isSwitchingToBluetooth -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = if (isConnected) "연결됨" else "미연결",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(TopBarGap))

            Box(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedButton(
                    onClick = onDropdownClick,
                    enabled = !isConnected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = ActionButtonGap)
                        .height(ActionButtonHeight),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = dropdownLabel,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = onDropdownDismiss
                ) {
                    when (selectedTab) {
                        ConnectionTab.WIFI -> {
                            if (ipcOptions.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "등록된 WIFI 프로필이 없습니다.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = onDropdownDismiss,
                                    enabled = false
                                )
                            } else {
                                ipcOptions.forEach { option ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { onIpcSelected(option.name) },
                                                onLongClick = { onIpcLongPressed(option.name) }
                                            )
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(
                                            option.label,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }

                        ConnectionTab.BT -> {
                            if (btDevices.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "블루투스 페어링된 기기가 없습니다.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = onDropdownDismiss,
                                    enabled = false
                                )
                            } else {
                                btDevices.forEach { device ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { onBtDeviceSelected(device.address) },
                                                onLongClick = { onIpcLongPressed(device.name) }
                                            )
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(
                                            device.label,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = if (isConnected) onDisconnect else onConnect,
            enabled = connectEnabled,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .width(ActionButtonWidth)
                .height(ActionButtonHeight)
        ) {
            Text(
                text = when {
                    reconnectingMessage != null -> reconnectingMessage
                    isConnecting -> "CONNECTING"
                    isConnected -> "DISCONNECT"
                    else -> "CONNECT"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun WifiSignalIcon(rssi: Int, modifier: Modifier = Modifier) {
    val level = when {
        rssi >= -55 -> 4
        rssi >= -66 -> 3
        rssi >= -77 -> 2
        rssi >= -88 -> 1
        else -> 0
    }
    val color = when (level) {
        4 -> Color(0xFF4CAF50)
        3 -> Color(0xFF8BC34A)
        2 -> Color(0xFFFFC107)
        1 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val inactive = Color(0xFFDDDDDD)

    Canvas(modifier = modifier.size(32.dp)) {
        val w = size.width
        val h = size.height
        val bars = 4
        val barWidth = w * 0.14f
        val gap = (w - barWidth * bars) / (bars + 1)

        for (i in 0 until bars) {
            val barHeight = h * (0.25f + 0.2f * i)
            val x = gap + i * (barWidth + gap)
            val y = h - barHeight
            drawRoundRect(
                color = if (i < level) color else inactive,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
            )
        }
    }
}
