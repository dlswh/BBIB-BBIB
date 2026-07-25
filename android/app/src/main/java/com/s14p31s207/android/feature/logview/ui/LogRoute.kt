package com.s14p31s207.android.feature.logview.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.s14p31s207.android.app.navigation.NavMenu
import com.s14p31s207.android.feature.auth.session.SessionState
import com.s14p31s207.android.feature.logview.vm.LogViewModel

@Composable
fun LogRoute(
    onNavSelected: (NavMenu) -> Unit,
    viewModel: LogViewModel = hiltViewModel(),
    onLogout: () -> Unit,
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    LogScreen(
        logs = logs,
        selectedNav = NavMenu.LOG,
        onNavSelected = onNavSelected,
        onLogout = onLogout,
        username = SessionState.name,
        onSearch = { start, end, query ->
            viewModel.performSearch(start, end, query)
        }
    )
}