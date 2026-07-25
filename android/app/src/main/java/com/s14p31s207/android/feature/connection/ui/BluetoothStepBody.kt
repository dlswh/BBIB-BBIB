package com.s14p31s207.android.feature.connection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.s14p31s207.android.data.comm.bluetooth.BluetoothBondState
import com.s14p31s207.android.data.comm.bluetooth.BluetoothPairingResult
import com.s14p31s207.android.data.comm.bluetooth.BluetoothPermissionHelper
import com.s14p31s207.android.data.comm.bluetooth.BluetoothScanDevice
import com.s14p31s207.android.data.comm.bluetooth.BluetoothScanState

/**
 * Step 3에서 사용하는 Bluetooth 스캔/페어링 UI.
 *
 * - 이미 페어링된 기기(BONDED)는 스캔 없이도 목록 상단에 표시됨
 * - 짧게 탭 → 해당 기기를 선택(selectedMacAddress 업데이트) → SAVE 활성화
 * - 길게 누름 → 페어링 팝업(미페어링 기기를 페어링할 때)
 * - 페어링 성공 시 onPairingSuccess 호출 → ViewModel이 MAC 주소 저장
 */
@Composable
fun BluetoothStepBody(
    scanState: BluetoothScanState,
    isSaving: Boolean,
    selectedMacAddress: String?,           // 현재 선택된 MAC (ViewModel 상태)
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onPairDevice: (String) -> Boolean,
    onClearPairingResult: () -> Unit,
    onSelectDevice: (BluetoothScanDevice) -> Unit,   // 짧은 탭 → 기기 선택
    onPairingSuccess: (BluetoothPairingResult) -> Unit, // 페어링 성공 후 저장
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pairCandidateDevice by remember { mutableStateOf<BluetoothScanDevice?>(null) }
    var hasPermission by remember {
        mutableStateOf(BluetoothPermissionHelper.hasBluetoothPermissions(context))
    }

    // 이미 페어링된 기기를 상단으로, 나머지는 그 아래에 표시
    val sortedDevices = remember(scanState.devices) {
        scanState.devices.sortedWith(
            compareByDescending { it.bondState == BluetoothBondState.BONDED }
        )
    }

    val statusText = when {
        !hasPermission -> "권한 필요"
        scanState.errorMessage != null -> "오류 발생"
        scanState.isScanning -> "검색 중..."
        sortedDevices.isEmpty() -> "기기 없음"
        else -> "${sortedDevices.size}개 발견"
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // [상단 컨트롤 라인]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    if (scanState.isScanning) {
                        onStopScan()
                    } else {
                        hasPermission = BluetoothPermissionHelper.hasBluetoothPermissions(context)
                        if (hasPermission) onStartScan()
                    }
                },
                enabled = !scanState.isPairing && !isSaving,
                shape = RoundedCornerShape(4.dp),
                colors = if (scanState.isScanning)
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else
                    ButtonDefaults.buttonColors(),
                modifier = Modifier
                    .height(40.dp)
                    .width(100.dp)
            ) {
                Text(
                    text = if (scanState.isScanning) "STOP" else "SCAN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (scanState.errorMessage != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "탭하여 기기 선택",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // [메인 리스트]
        BluetoothDeviceList(
            devices = sortedDevices,
            isScanning = scanState.isScanning,
            selectedMacAddress = selectedMacAddress,
            onDeviceClick = { device -> onSelectDevice(device) },
            onPairOptionClick = { device -> pairCandidateDevice = device },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }

    // 페어링 확인 다이얼로그 (길게 눌렀을 때)
    pairCandidateDevice?.let { device ->
        AlertDialog(
            onDismissRequest = { pairCandidateDevice = null },
            title = { Text("Bluetooth") },
            text = {
                Text(
                    "\"${device.name ?: device.address}\" 기기와 페어링하시겠습니까?\n\n" +
                            "WiFi로 연결한 기기와 동일한 기기인지 확인 후 진행하세요."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onPairDevice(device.address)
                    pairCandidateDevice = null
                }) { Text("페어링") }
            },
            dismissButton = {
                TextButton(onClick = { pairCandidateDevice = null }) { Text("취소") }
            }
        )
    }

    // 페어링 진행 중
    if (scanState.isPairing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Bluetooth") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("페어링 중입니다...")
                }
            },
            confirmButton = {}
        )
    }

    // 페어링 결과
    scanState.pairingResult?.let { result ->
        AlertDialog(
            onDismissRequest = { onClearPairingResult() },
            title = { Text("Bluetooth") },
            text = { Text(result.message) },
            confirmButton = {
                if (result.isSuccess) {
                    TextButton(onClick = {
                        onClearPairingResult()
                        onPairingSuccess(result)   // 성공 → MAC 포함 저장
                    }) { Text("선택") }
                } else {
                    TextButton(onClick = { onClearPairingResult() }) { Text("확인") }
                }
            },
            dismissButton = {
                if (!result.isSuccess) {
                    TextButton(onClick = {
                        onClearPairingResult()
                        onPairDevice(result.address)
                    }) { Text("다시 시도") }
                }
            }
        )
    }
}

// ── 기기 목록 ────────────────────────────────────────────────────────────────

@Composable
private fun BluetoothDeviceList(
    devices: List<BluetoothScanDevice>,
    isScanning: Boolean,
    selectedMacAddress: String?,
    onDeviceClick: (BluetoothScanDevice) -> Unit,
    onPairOptionClick: (BluetoothScanDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Box(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp)
        ) {
            if (devices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isScanning) "검색 중..." else "기기를 찾을 수 없습니다.\nSCAN 버튼을 눌러 주변 기기를 검색하세요.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(items = devices, key = { it.address }) { device ->
                    BluetoothDeviceRow(
                        device = device,
                        isSelected = device.address == selectedMacAddress,
                        onDeviceClick = onDeviceClick,
                        onPairOptionClick = onPairOptionClick
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        BluetoothListScrollIndicator(
            listState = listState,
            itemCount = devices.size,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 10.dp, bottom = 10.dp, end = 3.dp)
        )
    }
}

@Composable
private fun BluetoothDeviceRow(
    device: BluetoothScanDevice,
    isSelected: Boolean,
    onDeviceClick: (BluetoothScanDevice) -> Unit,
    onPairOptionClick: (BluetoothScanDevice) -> Unit
) {
    val displayName = device.name?.takeIf { it.isNotBlank() } ?: "알 수 없는 기기"
    val bondLabel = when (device.bondState) {
        BluetoothBondState.BONDED -> " (페어링됨)"
        BluetoothBondState.BONDING -> " (페어링 중)"
        BluetoothBondState.NONE -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(device.address) {
                    detectTapGestures(
                        onTap = {
                            when (device.bondState) {
                                BluetoothBondState.BONDED -> onDeviceClick(device)
                                BluetoothBondState.NONE -> onPairOptionClick(device)
                                BluetoothBondState.BONDING -> Unit
                            }
                        }
                    )
                }
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$displayName$bondLabel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BluetoothListScrollIndicator(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    if (itemCount <= 0) return
    val visibleCount = listState.layoutInfo.visibleItemsInfo.size
    if (visibleCount <= 0 || visibleCount >= itemCount) return

    val thumbHeightFraction = (visibleCount.toFloat() / itemCount.toFloat()).coerceIn(0.15f, 1f)
    val maxStartIndex = (itemCount - visibleCount).coerceAtLeast(1)
    val thumbOffsetFraction =
        (listState.firstVisibleItemIndex.toFloat() / maxStartIndex.toFloat()).coerceIn(0f, 1f)

    BoxWithConstraints(modifier = modifier) {
        val trackWidth = 4.dp
        val thumbHeight = (maxHeight * thumbHeightFraction).coerceAtLeast(24.dp)
        val thumbOffset = (maxHeight - thumbHeight) * thumbOffsetFraction

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(trackWidth)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = thumbOffset)
                .width(trackWidth)
                .height(thumbHeight)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp)
                )
        )
    }
}