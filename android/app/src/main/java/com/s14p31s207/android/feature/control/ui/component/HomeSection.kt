package com.s14p31s207.android.feature.control.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val HomeActionGroupWidth = 332.dp

@Composable
fun HomeSection(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onMoveHome: () -> Unit = {},
    onResetHome: () -> Unit = {}
) {
    Row(
        modifier = modifier.width(HomeActionGroupWidth),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.width(160.dp)) {
            HomeButton(
                modifier = Modifier,
                enabled = enabled,
                onClick = onMoveHome,
                text = "MOVE HOME"
            )
        }
        Box(modifier = Modifier.width(160.dp)) {
            HomeButton(
                modifier = Modifier,
                enabled = enabled,
                onClick = onResetHome,
                text = "RESET HOME"
            )
        }
    }
}
