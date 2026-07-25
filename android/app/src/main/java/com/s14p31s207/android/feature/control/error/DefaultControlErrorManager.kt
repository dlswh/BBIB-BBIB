package com.s14p31s207.android.feature.control.error

import com.s14p31s207.android.data.comm.tcp.CommandFactory
import com.s14p31s207.android.data.comm.tcp.ProtocolCommands
import com.s14p31s207.android.data.comm.tcp.ResponseStatus
import com.s14p31s207.android.data.comm.tcp.ServerMessage
import com.s14p31s207.android.domain.repository.CommandRepository
import com.s14p31s207.android.domain.repository.CommandSequenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class DefaultControlErrorManager @Inject constructor(
    private val commandRepository: CommandRepository,
    private val commandSequenceRepository: CommandSequenceRepository,
) : ControlErrorManager {

    private enum class ErrorSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL,
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeBlockingError = MutableStateFlow<ServerMessage.Error?>(null)
    override val activeBlockingError: StateFlow<ServerMessage.Error?> =
        _activeBlockingError.asStateFlow()

    private val _warningMessage = MutableStateFlow<String?>(null)
    override val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow()

    private val pendingResetRequestId = MutableStateFlow<String?>(null)

    private val _isResetPending = MutableStateFlow(false)
    override val isResetPending: StateFlow<Boolean> = _isResetPending.asStateFlow()

    private val _isErrorAcknowledged = MutableStateFlow(false)
    override val isErrorAcknowledged = _isErrorAcknowledged.asStateFlow()

    private val _isEmergencyStop = MutableStateFlow(false)
    override val isEmergencyStop: StateFlow<Boolean> = _isEmergencyStop.asStateFlow()

    private var resetTimeoutJob: kotlinx.coroutines.Job? = null

    init {
        scope.launch {
            commandRepository.messages.collect { message ->
                when (message) {
                    is ServerMessage.Response -> handleResponseMessage(message)
                    is ServerMessage.Error -> handleErrorMessage(message)
                    else -> Unit
                }
            }
        }
    }

    override fun clearSessionState() {
        _activeBlockingError.value = null
        _isErrorAcknowledged.value = false
        _isEmergencyStop.value = false
        _warningMessage.value = null
        clearResetPending()
    }

    override fun clearWarningMessage() {
        _warningMessage.value = null
    }

    override suspend fun requestErrorReset(): Result<Unit> {
        if (_isResetPending.value) {
            return Result.failure(IllegalStateException("Error reset is already pending."))
        }

        val activeError = _activeBlockingError.value
            ?: return Result.failure(IllegalStateException("No active blocking error."))

        return try {
            val requestId = commandSequenceRepository.nextId()
            setResetPending(requestId)

            commandRepository.send(
                CommandFactory.resetError(
                    id = requestId,
                    errorId = activeError.id
                )
            ).onFailure {
                clearResetPending()
            }
        } catch (error: Exception) {
            clearResetPending()
            Result.failure(error)
        }
    }

    private fun handleResponseMessage(message: ServerMessage.Response) {
        when (message.cmd) {
            ProtocolCommands.RESET_ERROR -> {
                val requestId = pendingResetRequestId.value ?: return
                if (message.requestId != requestId) return

                if (message.status == ResponseStatus.ACCEPTED) {
                    _activeBlockingError.value = null
                    _isErrorAcknowledged.value = false
                }
                clearResetPending()
            }

            ProtocolCommands.EMERGENCY_STOP_RESET -> {
                if (message.status == ResponseStatus.ACCEPTED) {
                    _activeBlockingError.value = null
                    _isErrorAcknowledged.value = false
                    _isEmergencyStop.value = false
                }
            }
        }
    }

    private fun handleErrorMessage(message: ServerMessage.Error) {
        when (message.toSeverity()) {
            ErrorSeverity.INFO -> Unit
            ErrorSeverity.WARNING -> {
                _warningMessage.value = message.context.ifBlank { message.errorName }
            }

            ErrorSeverity.ERROR,
            ErrorSeverity.CRITICAL -> {
                val isEstop = message.errorName.trim().uppercase() == "EMERGENCY_STOP_ACTIVE"
                _isEmergencyStop.value = isEstop
                _activeBlockingError.value = message
                _isErrorAcknowledged.value = false
                clearResetPending()
            }
        }
    }

    private fun ServerMessage.Error.toSeverity(): ErrorSeverity {
        return when (severity.trim().uppercase()) {
            "INFO" -> ErrorSeverity.INFO
            "WARNING" -> ErrorSeverity.WARNING
            "CRITICAL" -> ErrorSeverity.CRITICAL
            else -> ErrorSeverity.ERROR
        }
    }

    private fun setResetPending(requestId: String) {
        pendingResetRequestId.value = requestId
        _isResetPending.value = true

        resetTimeoutJob?.cancel()
        resetTimeoutJob = scope.launch {
            delay(3000L) // 3초 대기 (장비 응답 속도에 맞춰 2000L 등으로 조절 가능)

            // 3초가 지났는데도 여전히 같은 requestId로 대기 중이라면? -> 응답 유실(Timeout)
            if (pendingResetRequestId.value == requestId) {
                clearResetPending()
                // 사용자에게 스낵바로 실패 사실을 알려줌
                _warningMessage.value = "에러 리셋 응답이 지연되고 있습니다. 다시 시도해주세요."
            }
        }
    }

    private fun clearResetPending() {
        pendingResetRequestId.value = null
        _isResetPending.value = false

        resetTimeoutJob?.cancel()
        resetTimeoutJob = null
    }

    override fun acknowledgeError() {
        _isErrorAcknowledged.value = true
    }

}