package com.s14p31s207.android.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s14p31s207.android.data.comm.tcp.ServerMessage
import com.s14p31s207.android.feature.control.error.ControlErrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppErrorViewModel @Inject constructor(
    private val controlErrorManager: ControlErrorManager,
) : ViewModel() {

    private val _dialogError = MutableStateFlow<ServerMessage.Error?>(null)
    val dialogError: StateFlow<ServerMessage.Error?> = _dialogError.asStateFlow()

    init {
        viewModelScope.launch {
            controlErrorManager.activeBlockingError
                .combine(controlErrorManager.isErrorAcknowledged) { error, acknowledged ->
                    if (error != null && !acknowledged) error else null
                }
                .collect { error ->
                    _dialogError.value = error
                }
            }
    }

    fun dismissDialog() {
        _dialogError.value = null
        controlErrorManager.acknowledgeError()
    }
}
