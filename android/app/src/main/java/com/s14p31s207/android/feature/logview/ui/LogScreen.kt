package com.s14p31s207.android.feature.logview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s14p31s207.android.app.navigation.NavMenu
import com.s14p31s207.android.app.navigation.SidebarNav
import com.s14p31s207.android.data.comm.tcp.ResponseStatus
import com.s14p31s207.android.data.comm.tcp.ResultStatus
import com.s14p31s207.android.domain.model.LogEntry
import com.s14p31s207.android.domain.model.LogType
import com.s14p31s207.android.domain.model.displayText
import com.s14p31s207.android.domain.model.displayTime
import com.s14p31s207.android.ui.theme.AndroidTheme
import java.time.LocalDate
import android.widget.Toast

// ── Visual Transformations ────────────────────────────────────────────────────

class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(8)
        val result = buildString {
            digits.forEachIndexed { i, c ->
                if (i == 4 || i == 6) append('-')
                append(c)
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 4 -> offset
                offset <= 6 -> offset + 1
                else -> offset + 2
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 4 -> offset
                offset <= 7 -> offset - 1
                else -> offset - 2
            }
        }
        return TransformedText(AnnotatedString(result), offsetMapping)
    }
}

class TimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(4)
        val result = buildString {
            digits.forEachIndexed { i, c ->
                if (i == 2) append(':')
                append(c)
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 2) offset else offset + 1
            override fun transformedToOriginal(offset: Int): Int =
                if (offset <= 2) offset else offset - 1
        }
        return TransformedText(AnnotatedString(result), offsetMapping)
    }
}

// ── 타임스탬프 조합 ────────────────────────────────────────────────────────────

// "20260428" + "1300" → "2026-04-28T13:00:00"
fun buildTimestamp(date: String, time: String): String? {
    if (date.length < 8) return null
    val t = if (time.length < 4) "00:00:00"
    else "${time.substring(0, 2)}:${time.substring(2, 4)}:00"
    return "${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}T$t"
}

// ── LogScreen ─────────────────────────────────────────────────────────────────

@Composable
fun LogScreen(
    logs: List<LogEntry>,
    selectedNav: NavMenu,
    onNavSelected: (NavMenu) -> Unit,
    onSearch: (String?, String?, String) -> Unit,
    modifier: Modifier = Modifier,
    username: String = "",
    onLogout: () -> Unit = {},
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var activeType by remember { mutableStateOf<LogType?>(null) }

    var startDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var startDateError by remember { mutableStateOf(false) }
    var startTimeError by remember { mutableStateOf(false) }
    var endDateError by remember { mutableStateOf(false) }
    var endTimeError by remember { mutableStateOf(false) }
    var appliedStart by remember { mutableStateOf<String?>(null) }
    var appliedEnd by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()


    val filtered = remember(logs, appliedQuery, activeType, appliedStart, appliedEnd) {
        logs.filter { log ->
            val typeOk = activeType == null || log.type == activeType
            val searchOk = appliedQuery.isBlank() ||
                    log.displayText().contains(appliedQuery, ignoreCase = true)
            val timeOk = when {
                appliedStart == null && appliedEnd == null -> true
                appliedStart != null && appliedEnd == null -> log.timestamp >= appliedStart!!
                appliedStart == null && appliedEnd != null -> log.timestamp <= appliedEnd!!
                else -> log.timestamp in appliedStart!!..appliedEnd!!
            }
            typeOk && searchOk && timeOk
        }
    }
    LaunchedEffect(filtered.size) {
        if (filtered.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

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
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 타이틀 + CLEAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOG",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // 기간 검색
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateTimeInput(
                    date = startDate,
                    time = startTime,
                    dateError = startDateError,
                    timeError = startTimeError,
                    onDateChange = {
                        startDate = it
                        if (it.length >= 8) startDateError = false
                    },
                    onTimeChange = {
                        startTime = it
                        if (it.length >= 4) startTimeError = false
                    },
                )
                Text("~", style = MaterialTheme.typography.bodyMedium)
                DateTimeInput(
                    date = endDate,
                    time = endTime,
                    dateError = endDateError,
                    timeError = endTimeError,
                    onDateChange = {
                        endDate = it
                        if (it.length >= 8) endDateError = false
                    },
                    onTimeChange = {
                        endTime = it
                        if (it.length >= 4) endTimeError = false
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        startDateError = startDate.length < 8
                        startTimeError = startTime.length < 4
                        endDateError = endDate.length < 8
                        endTimeError = endTime.length < 4
                        if (startDateError || startTimeError || endDateError || endTimeError) {
                            Toast.makeText(context, "정확한 기간을 입력해주세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        appliedStart = buildTimestamp(startDate, startTime)
                        appliedEnd = buildTimestamp(endDate, endTime)
                        onSearch(appliedStart, appliedEnd, query)
                    },
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text("ENTER", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 키워드 검색
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("search logs...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        appliedQuery = query
                        onSearch(appliedStart, appliedEnd, query)
                    },
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text("ENTER", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 타입 필터 칩
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LogTypeChip(
                    label = "ALL",
                    selected = activeType == null,
                    onClick = { activeType = null }
                )
                LogType.entries.forEach { type ->
                    LogTypeChip(
                        label = type.name,
                        selected = activeType == type,
                        color = type.color(),
                        onClick = { activeType = if (activeType == type) null else type }
                    )
                }
            }

            // 로그 리스트
            Surface(
                modifier = Modifier
                    .height(500.dp)  // weight 대신 고정 높이
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                        .height(500.dp)  // weight 대신 고정 높이
                        .fillMaxWidth()
                    ) {
                        items(filtered, key = { it.id }) { log ->
                            LogEntryRow(log)
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "no logs match",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Text(
                        text = "showing ${filtered.size} / ${logs.size}  ·  " +
                                LogType.entries.joinToString("  ") { t ->
                                    "${t.name}:${filtered.count { it.type == t }}"
                                },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

// ── DateTimeInput ─────────────────────────────────────────────────────────────

@Composable
fun DateTimeInput(
    date: String,
    time: String,
    dateError: Boolean,
    timeError: Boolean,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
) {
    val today = LocalDate.now().toString()

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = date,
            onValueChange = { onDateChange(it.filter { c -> c.isDigit() }.take(8)) },
            placeholder = { Text(today, fontSize = 11.sp) },
            visualTransformation = DateVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(130.dp),
            singleLine = true,
            isError = dateError,
        )
        OutlinedTextField(
            value = time,
            onValueChange = { onTimeChange(it.filter { c -> c.isDigit() }.take(4)) },
            placeholder = { Text("00:00", fontSize = 11.sp) },
            visualTransformation = TimeVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(90.dp),
            singleLine = true,
            isError = timeError,
        )
    }
}

// ── 나머지 컴포넌트 (기존과 동일) ──────────────────────────────────────────────

@Composable
fun LogEntryRow(log: LogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (log is LogEntry.Error)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = log.displayTime(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Surface(
            color = log.type.color().copy(alpha = 0.15f),
            shape = RoundedCornerShape(3.dp)
        ) {
            Text(
                text = log.type.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = log.type.color(),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        when (log) {
            is LogEntry.Response -> StatusBadge(
                text = log.status.name,
                color = if (log.status == ResponseStatus.ACCEPTED)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            is LogEntry.Result -> StatusBadge(
                text = log.status.name,
                color = if (log.status == ResultStatus.COMPLETED)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            is LogEntry.Error -> Spacer(Modifier.width(0.dp))
            is LogEntry.State -> Spacer(Modifier.width(0.dp))
        }
        Text(
            text = log.displayText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun LogTypeChip(
    label: String,
    selected: Boolean,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        colors = if (color != Color.Unspecified) FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.15f),
            selectedLabelColor = color
        ) else FilterChipDefaults.filterChipColors()
    )
}

fun LogType.color(): Color = when (this) {
    LogType.RESPONSE -> Color(0xFF378ADD)
    LogType.RESULT   -> Color(0xFF639922)
    LogType.ERROR    -> Color(0xFFE24B4A)
    LogType.STATE -> Color(0xFF888780)  // 회색
}

@Preview(showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun LogScreenPreview() {
    AndroidTheme {
        Surface {
            LogScreen(
                logs = listOf(
                    LogEntry.Response(
                        id = "res-1", timestamp = "2026-04-22T13:50:04",
                        ipcId = "preview-device",  // 추가
                        requestId = "req-1", cmd = "MOVE_RELATIVE_WHEEL",
                        status = ResponseStatus.ACCEPTED, message = "이동 요청 수락"
                    ),
                    LogEntry.Result(
                        id = "rst-1", timestamp = "2026-04-22T13:50:06",
                        ipcId = "preview-device",  // 추가
                        requestId = "req-1", cmd = "MOVE_RELATIVE_WHEEL",
                        status = ResultStatus.COMPLETED, message = "이동 완료",
                        position = 12.0, speed = 0.0
                    ),
                    LogEntry.Response(
                        id = "res-2", timestamp = "2026-04-22T13:50:08",
                        ipcId = "preview-device",  // 추가
                        requestId = "req-2", cmd = "EMERGENCY_STOP",
                        status = ResponseStatus.REJECTED, message = "정지 요청 거절"
                    ),
                    LogEntry.Error(
                        id = "err-1", timestamp = "2026-04-22T13:50:10",
                        ipcId = "preview-device",  // 추가
                        errorCode = 301, errorName = "MOTOR_ALARM",
                        category = "Motion", severity = "Error",
                        context = "모터 동작 중 이상 상태가 감지된 경우",
                        solution = "모터 상태를 확인하고 초기화 후 재시도하세요",
                        requestId = "req-1"
                    ),
                ),
                selectedNav = NavMenu.LOG,
                onNavSelected = {},
                onSearch = {start, end, query ->}
            )
        }
    }
}
