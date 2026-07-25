package com.s14p31s207.android.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.s14p31s207.android.feature.auth.vm.LoginViewModel

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onOfflineMode: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onOfflineModeClick = onOfflineMode,
        onLoginClick = { viewModel.login(onLoginSuccess) }
    )
}