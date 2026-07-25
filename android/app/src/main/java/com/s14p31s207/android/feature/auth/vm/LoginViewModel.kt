package com.s14p31s207.android.feature.auth.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s14p31s207.android.domain.repository.AuthRepository
import com.s14p31s207.android.feature.auth.session.AuthMode
import com.s14p31s207.android.feature.auth.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "아이디와 비밀번호를 입력해주세요.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            authRepository.login(state.username, state.password)
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    android.util.Log.e("LoginViewModel", "로그인 실패: ${e.message}", e) // ✅ 추가
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "로그인 실패"
                    )
                }
        }
    }

    fun logout(onSuccess: () -> Unit) {

        // 오프라인 모드
        if (SessionState.authMode == AuthMode.OFFLINE) {
            SessionState.authMode = AuthMode.ONLINE
            onSuccess()
            return
        }

        // 온라인 모드
        viewModelScope.launch {
            authRepository.logout()
                .onSuccess {
                    onSuccess()
                }
                .onFailure { e ->
                    android.util.Log.e("LoginViewModel", "로그아웃 실패: ${e.message}", e)
                }
        }
    }
}