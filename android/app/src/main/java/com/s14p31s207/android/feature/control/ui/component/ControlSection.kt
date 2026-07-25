package com.s14p31s207.android.feature.control.ui.component

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.s14p31s207.android.data.comm.tcp.Direction

private enum class DirectionToggle { CCW, CW }

private val ControlGap = 12.dp
private val ControlButtonHeight = 56.dp

@Composable
fun ControlSection(
    modifier: Modifier = Modifier,
    commandEnabled: Boolean = true,
    stopEnabled: Boolean = false,
    onSpeedChanged: (String) -> Unit = {},
    onAbsolutePositionChanged: (String) -> Unit = {},
    onAbsoluteMove: (position: Double, speed: Double) -> Unit = { _, _ -> },
    onRelativeDistanceChanged: (String) -> Unit = {},
    onRelativeMove: (direction: Direction, distance: Double, speed: Double) -> Unit = { _, _, _ -> },
    onCcw: () -> Unit = {},
    onCw: () -> Unit = {},
    onStop: () -> Unit = {},
) {
    val context = LocalContext.current
    var speed by rememberSaveable { mutableStateOf("1.0") }
    var absoluteTarget by rememberSaveable { mutableStateOf("") }
    var relativeDistance by rememberSaveable { mutableStateOf("") }
    var speedError by rememberSaveable { mutableStateOf(false) }
    var absoluteTargetError by rememberSaveable { mutableStateOf(false) }
    var relativeDistanceError by rememberSaveable { mutableStateOf(false) }
    var selectedDirection by rememberSaveable { mutableStateOf<DirectionToggle?>(null) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(ControlGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ControlGap)
        ) {
            FieldCell(
                modifier = Modifier.weight(1f),
                label = "SPEED",
                unit = "cm/s",
                isError = speedError,
                value = speed,
                enabled = commandEnabled,
                onValueChange = { input ->
                    speed = input.filter { it.isDigit() || it == '.' }
                    speedError = speed.toDoubleOrNull()?.let { it <= 0 } ?: speed.isBlank()
                    onSpeedChanged(speed)
                }
            )
            DirectionCell(
                modifier = Modifier.weight(1f),
                enabled = commandEnabled,
                selectedDirection = selectedDirection,
                onCcw = {
                    selectedDirection =
                        if (selectedDirection == DirectionToggle.CCW) null else DirectionToggle.CCW
                    onCcw()
                },
                onCw = {
                    selectedDirection =
                        if (selectedDirection == DirectionToggle.CW) null else DirectionToggle.CW
                    onCw()
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(ControlGap)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ControlGap)
            ) {
                FieldCell(
                    label = "POSITION",
                    unit = "cm",
                    isError = absoluteTargetError,
                    value = absoluteTarget,
                    enabled = commandEnabled,
                    onValueChange = { input ->
                        absoluteTarget = input.filter { it.isDigit() || it == '.' || it == '-' }
                        absoluteTargetError =
                            absoluteTarget.toDoubleOrNull()?.let { it < 0 } ?: absoluteTarget.isBlank()
                        onAbsolutePositionChanged(absoluteTarget)
                    }
                )
                Button(
                    onClick = {
                        if (absoluteTarget.isBlank()) {
                            absoluteTargetError = true
                            Toast.makeText(context, "위치를 입력하시오.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val pos = absoluteTarget.toDoubleOrNull() ?: return@Button
                        val spd = speed.toDoubleOrNull() ?: return@Button
                        if (pos < 0) {
                            absoluteTargetError = true
                            Toast.makeText(context, "잘못된 위치 값을 입력하였습니다.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (spd <= 0) {
                            speedError = true
                            Toast.makeText(context, "잘못된 속도 값을 입력하였습니다.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        absoluteTargetError = false
                        speedError = false
                        onAbsoluteMove(pos, spd)
                    },
                    enabled = commandEnabled,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ControlButtonHeight)
                ) {
                    Text(
                        "MOVE ABSOLUTE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ControlGap)
            ) {
                FieldCell(
                    label = "DISTANCE",
                    unit = "cm",
                    isError = relativeDistanceError,
                    value = relativeDistance,
                    enabled = commandEnabled,
                    onValueChange = { input ->
                        relativeDistance = input.filter { it.isDigit() || it == '.' || it == '-' }
                        relativeDistanceError =
                            relativeDistance.toDoubleOrNull()?.let { it < 0 } ?: relativeDistance.isBlank()
                        onRelativeDistanceChanged(relativeDistance)
                    }
                )
                Button(
                    onClick = {
                        if (selectedDirection == null) {
                            Toast.makeText(context, "방향을 입력하시오.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (relativeDistance.isBlank()) {
                            relativeDistanceError = true
                            Toast.makeText(context, "거리를 입력하시오.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val dir = when (selectedDirection) {
                            DirectionToggle.CCW -> Direction.NEG
                            DirectionToggle.CW -> Direction.POS
                            null -> return@Button
                        }
                        val dist = relativeDistance.toDoubleOrNull() ?: return@Button
                        val spd = speed.toDoubleOrNull() ?: return@Button
                        if (dist < 0) {
                            relativeDistanceError = true
                            Toast.makeText(context, "잘못된 거리 값을 입력하였습니다.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (spd <= 0) {
                            speedError = true
                            Toast.makeText(context, "잘못된 속도 값을 입력하였습니다.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        relativeDistanceError = false
                        speedError = false
                        onRelativeMove(dir, dist, spd)
                    },
                    enabled = commandEnabled,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ControlButtonHeight)
                ) {
                    Text(
                        "MOVE RELATIVE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Button(
            onClick = onStop,
            enabled = stopEnabled,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(ControlButtonHeight)
        ) {
            Text(
                "STOP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FieldCell(
    modifier: Modifier = Modifier,
    label: String,
    unit: String,
    isError: Boolean,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            isError = isError,
            suffix = {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(ControlButtonHeight),
            shape = RoundedCornerShape(4.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
    }
}

@Composable
private fun DirectionCell(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    selectedDirection: DirectionToggle?,
    onCcw: () -> Unit,
    onCw: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "DIRECTION",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ControlGap)
        ) {
            DirectionButton(
                modifier = Modifier.weight(1f),
                selected = selectedDirection == DirectionToggle.CCW,
                enabled = enabled,
                onClick = onCcw,
                icon = { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null) }
            )
            DirectionButton(
                modifier = Modifier.weight(1f),
                selected = selectedDirection == DirectionToggle.CW,
                enabled = enabled,
                onClick = onCw,
                icon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun DirectionButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .height(ControlButtonHeight)
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
