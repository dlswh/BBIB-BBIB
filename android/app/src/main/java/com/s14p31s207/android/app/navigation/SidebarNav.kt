package com.s14p31s207.android.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// 공통으로 사용할 메뉴 정의
enum class NavMenu { CONNECTION, CONTROL, STATE, LOG, WIFI_SETTING }

@Composable
fun SidebarNav(
    selectedNav: NavMenu,
    onNavSelected: (NavMenu) -> Unit,
    onLogout: () -> Unit,
    username: String = "",
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavMenu.CONNECTION to "CONNECT\nSETTING",
        NavMenu.CONTROL to "CONTROL",
//        NavMenu.STATE to "STATE",
        NavMenu.LOG to "LOG",
//        NavMenu.WIFI_SETTING to "TEST",
    )
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(100.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
        verticalArrangement = Arrangement.Top,
    ) {
        // 사용자 정보 - 상단 고정
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "👤",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = username.ifBlank { "OFFLINE" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // 기존 메뉴 아이템
        items.forEach { (nav, label) ->
            val isSelected = nav == selectedNav

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onNavSelected(nav) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }
        }

        // ✅ 로그아웃 버튼 맨 아래 고정
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.errorContainer)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { showLogoutDialog = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "LOGOUT",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃") },
            text = { Text("정말 로그아웃 하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}