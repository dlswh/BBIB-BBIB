package com.s14p31s207.android.feature.control.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val JogLineWidth = 332.dp
private val JogCellWidth = 160.dp
private val JogGap = 12.dp
private val JogButtonHeight = 56.dp

@Composable
fun JogSection(
    modifier: Modifier = Modifier,
    directionEnabled: Boolean = true,
    jogStopEnabled: Boolean = false,
    onHomeLeft: () -> Unit = {},
    onHomeRight: () -> Unit = {},
    onJogStop: () -> Unit = {},
) {
    Column(
        modifier = modifier.width(JogLineWidth),
        verticalArrangement = Arrangement.spacedBy(JogGap)
    ) {
        Row(
            modifier = Modifier.width(JogLineWidth),
            horizontalArrangement = Arrangement.spacedBy(JogGap)
        ) {
            IconButton(
                onClick = onHomeLeft,
                enabled = directionEnabled,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .width(JogCellWidth)
                    .height(JogButtonHeight)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "JOG NEG"
                )
            }
            IconButton(
                onClick = onHomeRight,
                enabled = directionEnabled,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .width(JogCellWidth)
                    .height(JogButtonHeight)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "JOG POS"
                )
            }
        }

        Button(
            onClick = onJogStop,
            enabled = jogStopEnabled,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .width(JogLineWidth)
                .height(JogButtonHeight)
        ) {
            Text(
                "JOG STOP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
