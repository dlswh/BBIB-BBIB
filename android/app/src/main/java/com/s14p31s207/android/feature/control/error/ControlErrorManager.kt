package com.s14p31s207.android.feature.control.error

import com.s14p31s207.android.data.comm.tcp.ServerMessage
import kotlinx.coroutines.flow.StateFlow

interface ControlErrorManager {
    val activeBlockingError: StateFlow<ServerMessage.Error?>
    val warningMessage: StateFlow<String?>
    val isResetPending: StateFlow<Boolean>
    val isErrorAcknowledged: StateFlow<Boolean>
    val isEmergencyStop: StateFlow<Boolean>

    fun clearSessionState()
    fun clearWarningMessage()

    suspend fun requestErrorReset(): Result<Unit>
    fun acknowledgeError()
}