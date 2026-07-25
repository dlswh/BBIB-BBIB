package com.s14p31s207.android.perf

import android.content.Intent
import com.s14p31s207.android.BuildConfig
import com.s14p31s207.android.data.comm.tcp.CommandFactory
import com.s14p31s207.android.data.comm.tcp.Direction
import com.s14p31s207.android.data.comm.tcp.ProtocolCommands
import com.s14p31s207.android.data.comm.tcp.ResponseStatus
import com.s14p31s207.android.data.comm.tcp.ResultStatus
import com.s14p31s207.android.data.comm.tcp.ServerMessage
import com.s14p31s207.android.data.db.IpcDevice
import com.s14p31s207.android.data.db.IpcDeviceDao
import com.s14p31s207.android.data.repository.CommandRepositoryImpl
import com.s14p31s207.android.domain.repository.CommandRepository
import com.s14p31s207.android.domain.repository.CommandSequenceRepository
import com.s14p31s207.android.domain.repository.ConnectionChannel
import com.s14p31s207.android.domain.repository.DeviceConnectionRepository
import com.s14p31s207.android.util.ConnectionPerfTrace
import com.s14p31s207.android.util.PerfLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResponsePerfRunner @Inject constructor(
    private val ipcDeviceDao: IpcDeviceDao,
    private val deviceConnectionRepository: DeviceConnectionRepository,
    private val commandRepository: CommandRepository,
    private val commandRepositoryImpl: CommandRepositoryImpl,
    private val commandSequenceRepository: CommandSequenceRepository
) {
    companion object {
        private const val EXTRA_PERF_MODE = "perfMode"
        private const val EXTRA_REPEAT = "repeat"
        private const val EXTRA_INTERVAL_MS = "intervalMs"
        private const val EXTRA_TIMEOUT_MS = "timeoutMs"
        private const val EXTRA_PROFILE_NAME = "perfProfileName"

        private const val MODE_RESP_WIFI = "RESP_WIFI"
        private const val MODE_RESP_BT = "RESP_BT"
        private const val MODE_RANGE_BT = "RANGE_BT"
        private const val MODE_CTRL_WIFI = "CTRL_WIFI"
        private const val MODE_CTRL_BT = "CTRL_BT"
        private const val MODE_SWITCH_WIFI_BT = "SWITCH_WIFI_BT"
        private val SUPPORTED_MODES = setOf(
            MODE_RESP_WIFI,
            MODE_RESP_BT,
            MODE_RANGE_BT,
            MODE_CTRL_WIFI,
            MODE_CTRL_BT,
            MODE_SWITCH_WIFI_BT
        )
        private const val DEFAULT_MOVE_DISTANCE = 5.0
        private const val DEFAULT_MOVE_SPEED = 1.0
        private val DEFAULT_MOVE_DIRECTION = Direction.POS
    }

    suspend fun runIfRequested(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return

        val mode = intent.getStringExtra(EXTRA_PERF_MODE) ?: return
        if (mode !in SUPPORTED_MODES) return

        val repeat = intent.getIntExtra(EXTRA_REPEAT, 100).coerceAtLeast(1)
        val intervalMs = intent.getIntExtra(EXTRA_INTERVAL_MS, 200).coerceAtLeast(0)
        val timeoutMs = intent.getIntExtra(EXTRA_TIMEOUT_MS, 3000).coerceAtLeast(1)
        val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME)

        val profile = loadProfile(mode, profileName)
        if (profile == null) {
            PerfLogger.d("[${mode}][ABORT] reason=profile_not_found profileName=${profileName ?: "null"}")
            return
        }

        val channel = if (
            mode == MODE_RESP_WIFI ||
            mode == MODE_CTRL_WIFI ||
            mode == MODE_SWITCH_WIFI_BT
        ) {
            ConnectionChannel.WIFI
        } else {
            ConnectionChannel.BLUETOOTH
        }
        val target = targetOf(profile, channel)
        val connectStartedAt = System.currentTimeMillis()
        val connectTrace = ConnectionPerfTrace(
            category = when (mode) {
                MODE_RESP_WIFI, MODE_CTRL_WIFI, MODE_SWITCH_WIFI_BT -> "CONN_WIFI"
                MODE_RESP_BT, MODE_RANGE_BT, MODE_CTRL_BT -> "CONN_BT_RECONNECT"
                else -> "CONN_BT_RECONNECT"
            },
            attemptId = "perf-connect-$connectStartedAt",
            startedAt = connectStartedAt
        )

        PerfLogger.d(
            "[${mode}][SETUP] repeat=$repeat intervalMs=$intervalMs timeoutMs=$timeoutMs " +
                "moveDirection=${DEFAULT_MOVE_DIRECTION.name} " +
                "moveDistance=$DEFAULT_MOVE_DISTANCE moveSpeed=$DEFAULT_MOVE_SPEED target=$target"
        )
        PerfLogger.d(
            "[${connectTrace.category}][START] attemptId=${connectTrace.attemptId} " +
                "target=$target ts=${connectTrace.startedAt}"
        )

        val connectResult = deviceConnectionRepository.connect(profile, channel, connectTrace)
        if (connectResult.isFailure) {
            PerfLogger.d("[${mode}][ABORT] reason=connect_failed error=${connectResult.exceptionOrNull()?.message ?: "unknown"}")
            return
        }

        commandRepositoryImpl.isWifiMode = (channel == ConnectionChannel.WIFI)

        try {
            if (mode == MODE_CTRL_WIFI || mode == MODE_CTRL_BT) {
                runControlPerf(
                    mode = mode,
                    repeat = repeat,
                    intervalMs = intervalMs,
                    timeoutMs = timeoutMs,
                    target = target
                )
            } else if (mode == MODE_SWITCH_WIFI_BT) {
                runSwitchPerf(
                    repeat = repeat,
                    timeoutMs = timeoutMs,
                    target = target
                )
            } else {
                runResponsePerf(
                    mode = mode,
                    repeat = repeat,
                    intervalMs = intervalMs,
                    timeoutMs = timeoutMs,
                    target = target
                )
            }
        } finally {
            deviceConnectionRepository.disconnect()
        }
    }

    private suspend fun runResponsePerf(
        mode: String,
        repeat: Int,
        intervalMs: Int,
        timeoutMs: Int,
        target: String
    ) {
        val attemptId = mode.lowercase() + "-${System.currentTimeMillis()}"
        var successCount = 0
        var failCount = 0
        val rtts = mutableListOf<Long>()

        repeat(repeat) { index ->
            val seq = index + 1
            val requestId = commandSequenceRepository.nextId()
            val sendTs = System.currentTimeMillis()

            PerfLogger.d(
                "[${mode}][SEND] attemptId=$attemptId requestId=$requestId " +
                    "seq=$seq target=$target ts=$sendTs"
            )

            val sendResult = commandRepository.send(CommandFactory.ping(requestId))
            if (sendResult.isFailure) {
                failCount++
                PerfLogger.d(
                    "[${mode}][FAIL] attemptId=$attemptId requestId=$requestId " +
                        "seq=$seq target=$target ts=${System.currentTimeMillis()} " +
                        "error=${sendResult.exceptionOrNull()?.message ?: "send_failed"}"
                )
                delay(intervalMs.toLong())
                return@repeat
            }

            val response = withTimeoutOrNull(timeoutMs.toLong()) {
                commandRepository.messages
                    .filterIsInstance<ServerMessage.Response>()
                    .first {
                        it.requestId == requestId &&
                            it.cmd == ProtocolCommands.PING &&
                            it.status == ResponseStatus.ACCEPTED
                    }
            }

            if (response == null) {
                failCount++
                PerfLogger.d(
                    "[${mode}][FAIL] attemptId=$attemptId requestId=$requestId " +
                        "seq=$seq target=$target ts=${System.currentTimeMillis()} error=timeout"
                )
            } else {
                val recvTs = System.currentTimeMillis()
                val elapsedMs = recvTs - sendTs
                successCount++
                rtts += elapsedMs
                PerfLogger.d(
                    "[${mode}][RECV] attemptId=$attemptId requestId=$requestId " +
                        "seq=$seq target=$target ts=$recvTs elapsedMs=$elapsedMs"
                )
            }

            delay(intervalMs.toLong())
        }

        val avgMs = if (rtts.isEmpty()) 0.0 else rtts.average()
        val minMs = rtts.minOrNull() ?: -1L
        val maxMs = rtts.maxOrNull() ?: -1L
        PerfLogger.d(
            "[${mode}][SUMMARY] attemptId=$attemptId total=$repeat success=$successCount " +
                "fail=$failCount avgMs=${"%.2f".format(avgMs)} minMs=$minMs maxMs=$maxMs"
        )
    }

    private suspend fun runControlPerf(
        mode: String,
        repeat: Int,
        intervalMs: Int,
        timeoutMs: Int,
        target: String
    ) {
        val respCategory = if (mode == MODE_CTRL_WIFI) "CTRL_RESP_WIFI" else "CTRL_RESP_BT"
        val completeCategory = if (mode == MODE_CTRL_WIFI) "CTRL_RESULT_WIFI" else "CTRL_RESULT_BT"
        val attemptId = mode.lowercase() + "-${System.currentTimeMillis()}"

        var successCount = 0
        var failCount = 0
        val responseTimes = mutableListOf<Long>()
        val completionTimes = mutableListOf<Long>()

        repeat(repeat) { index ->
            val seq = index + 1
            val moveOk = executeControlStep(
                category = respCategory,
                completionCategory = completeCategory,
                attemptId = attemptId,
                seq = seq,
                target = target,
                cmd = ProtocolCommands.MOVE_RELATIVE_WHEEL,
                timeoutMs = timeoutMs,
                responseTimes = responseTimes,
                completionTimes = completionTimes,
                completionState = null
            ) { requestId ->
                CommandFactory.moveRelative(
                    id = requestId,
                    direction = DEFAULT_MOVE_DIRECTION,
                    distance = DEFAULT_MOVE_DISTANCE,
                    speed = DEFAULT_MOVE_SPEED
                )
            }

            if (moveOk) {
                successCount++
            } else {
                failCount++
            }

            delay(intervalMs.toLong())
        }

        logSummary(respCategory, attemptId, repeat, responseTimes)
        logSummary(completeCategory, attemptId, repeat, completionTimes)
        PerfLogger.d(
            "[${mode}][SUMMARY] attemptId=$attemptId total=$repeat success=$successCount " +
                "fail=$failCount " +
                "moveDirection=${DEFAULT_MOVE_DIRECTION.name} moveDistance=$DEFAULT_MOVE_DISTANCE " +
                "moveSpeed=$DEFAULT_MOVE_SPEED"
        )
    }

    private suspend fun runSwitchPerf(
        repeat: Int,
        timeoutMs: Int,
        target: String
    ) {
        val attemptId = "switch_wifi_bt-${System.currentTimeMillis()}"
        PerfLogger.d(
            "[SWITCH_WIFI_BT][ARMED] attemptId=$attemptId total=$repeat target=$target " +
                "message=disconnect_wifi_to_start_measurement"
        )

        val disconnectDetectedAt = withTimeoutOrNull(600_000L) {
            deviceConnectionRepository.connectionState.first { state ->
                state is com.s14p31s207.android.domain.repository.DeviceConnectionState.Reconnecting
            }
            System.currentTimeMillis()
        }

        if (disconnectDetectedAt == null) {
            PerfLogger.d("[SWITCH_WIFI_BT][ABORT] attemptId=$attemptId reason=disconnect_not_detected")
            return
        }

        PerfLogger.d(
            "[SWITCH_WIFI_DETECT][SUCCESS] attemptId=$attemptId target=$target ts=$disconnectDetectedAt"
        )

        val btSwitchStartedAt = withTimeoutOrNull(timeoutMs.toLong()) {
            deviceConnectionRepository.connectionState.first { state ->
                state is com.s14p31s207.android.domain.repository.DeviceConnectionState.SwitchingToBluetooth
            }
            System.currentTimeMillis()
        }

        if (btSwitchStartedAt == null) {
            PerfLogger.d(
                "[SWITCH_WIFI_RETRY][FAIL] attemptId=$attemptId target=$target ts=${System.currentTimeMillis()} " +
                    "error=wifi_retry_timeout"
            )
            return
        }

        PerfLogger.d(
            "[SWITCH_WIFI_RETRY][SUCCESS] attemptId=$attemptId target=$target ts=$btSwitchStartedAt " +
                "elapsedMs=${btSwitchStartedAt - disconnectDetectedAt}"
        )
        PerfLogger.d(
            "[SWITCH_BT_CONNECT][START] attemptId=$attemptId target=$target ts=$btSwitchStartedAt"
        )

        val btConnectedAt = withTimeoutOrNull(timeoutMs.toLong()) {
            deviceConnectionRepository.connectionState.first { state ->
                state is com.s14p31s207.android.domain.repository.DeviceConnectionState.Connected &&
                    state.channel == ConnectionChannel.BLUETOOTH
            }
            System.currentTimeMillis()
        }

        if (btConnectedAt == null) {
            PerfLogger.d(
                "[SWITCH_BT_CONNECT][FAIL] attemptId=$attemptId target=$target ts=${System.currentTimeMillis()} " +
                    "error=bluetooth_connect_timeout"
            )
            return
        }

        commandRepositoryImpl.isWifiMode = false
        PerfLogger.d(
            "[SWITCH_BT_CONNECT][SUCCESS] attemptId=$attemptId target=$target ts=$btConnectedAt " +
                "elapsedMs=${btConnectedAt - btSwitchStartedAt}"
        )

        val recoveryResponseAt = measureRecoveryResponse(
            attemptId = attemptId,
            target = target,
            timeoutMs = timeoutMs
        ) ?: return

        PerfLogger.d(
            "[SWITCH_TOTAL_RECOVERY][SUCCESS] attemptId=$attemptId target=$target ts=$recoveryResponseAt " +
                "elapsedMs=${recoveryResponseAt - disconnectDetectedAt}"
        )
    }

    private suspend fun measureRecoveryResponse(
        attemptId: String,
        target: String,
        timeoutMs: Int
    ): Long? {
        val requestId = commandSequenceRepository.nextId()
        val sendTs = System.currentTimeMillis()

        PerfLogger.d(
            "[SWITCH_RECOVERY_RESP][SEND] attemptId=$attemptId requestId=$requestId " +
                "cmd=${ProtocolCommands.PING} target=$target ts=$sendTs"
        )

        val sendResult = commandRepository.send(CommandFactory.ping(requestId))
        if (sendResult.isFailure) {
            PerfLogger.d(
                "[SWITCH_RECOVERY_RESP][FAIL] attemptId=$attemptId requestId=$requestId " +
                    "cmd=${ProtocolCommands.PING} target=$target ts=${System.currentTimeMillis()} " +
                    "error=${sendResult.exceptionOrNull()?.message ?: "send_failed"}"
            )
            return null
        }

        val response = withTimeoutOrNull(timeoutMs.toLong()) {
            commandRepository.messages
                .filterIsInstance<ServerMessage.Response>()
                .first {
                    it.requestId == requestId &&
                        it.cmd == ProtocolCommands.PING &&
                        it.status == ResponseStatus.ACCEPTED
                }
        }

        if (response == null) {
            PerfLogger.d(
                "[SWITCH_RECOVERY_RESP][FAIL] attemptId=$attemptId requestId=$requestId " +
                    "cmd=${ProtocolCommands.PING} target=$target ts=${System.currentTimeMillis()} " +
                    "error=response_timeout"
            )
            return null
        }

        val recvTs = System.currentTimeMillis()
        PerfLogger.d(
            "[SWITCH_RECOVERY_RESP][RECV] attemptId=$attemptId requestId=$requestId " +
                "cmd=${ProtocolCommands.PING} target=$target ts=$recvTs elapsedMs=${recvTs - sendTs}"
        )
        return recvTs
    }

    private suspend fun executeControlStep(
        category: String,
        completionCategory: String,
        attemptId: String,
        seq: Int,
        target: String,
        cmd: String,
        timeoutMs: Int,
        responseTimes: MutableList<Long>,
        completionTimes: MutableList<Long>,
        completionState: String?,
        buildCommand: (String) -> com.s14p31s207.android.data.comm.tcp.CommandDto
    ): Boolean {
        val requestId = commandSequenceRepository.nextId()
        val sendTs = System.currentTimeMillis()

        PerfLogger.d(
            "[$category][SEND] attemptId=$attemptId requestId=$requestId seq=$seq " +
                "cmd=$cmd target=$target ts=$sendTs"
        )

        val sendResult = commandRepository.send(buildCommand(requestId))
        if (sendResult.isFailure) {
            PerfLogger.d(
                "[$category][FAIL] attemptId=$attemptId requestId=$requestId seq=$seq " +
                    "cmd=$cmd target=$target ts=${System.currentTimeMillis()} " +
                    "error=${sendResult.exceptionOrNull()?.message ?: "send_failed"}"
            )
            return false
        }

        val response = withTimeoutOrNull(timeoutMs.toLong()) {
            commandRepository.messages
                .filterIsInstance<ServerMessage.Response>()
                .first {
                    it.requestId == requestId &&
                        it.cmd == cmd &&
                        it.status == ResponseStatus.ACCEPTED
                }
        }

        if (response == null) {
            PerfLogger.d(
                "[$category][FAIL] attemptId=$attemptId requestId=$requestId seq=$seq " +
                    "cmd=$cmd target=$target ts=${System.currentTimeMillis()} error=response_timeout"
            )
            return false
        }

        val responseTs = System.currentTimeMillis()
        val responseElapsed = responseTs - sendTs
        responseTimes += responseElapsed
        PerfLogger.d(
            "[$category][RECV] attemptId=$attemptId requestId=$requestId seq=$seq " +
                "cmd=$cmd target=$target ts=$responseTs elapsedMs=$responseElapsed"
        )

        val completion = awaitControlCompletion(
            requestId = requestId,
            cmd = cmd,
            expectedState = completionState,
            timeoutMs = timeoutMs
        )

        if (completion == null) {
            PerfLogger.d(
                "[$completionCategory][FAIL] attemptId=$attemptId requestId=$requestId seq=$seq " +
                    "cmd=$cmd target=$target ts=${System.currentTimeMillis()} error=completion_timeout"
            )
            return false
        }

        val completionTs = System.currentTimeMillis()
        val completionElapsed = completionTs - sendTs
        completionTimes += completionElapsed
        PerfLogger.d(
            "[$completionCategory][RECV] attemptId=$attemptId requestId=$requestId seq=$seq " +
                "cmd=$cmd target=$target ts=$completionTs elapsedMs=$completionElapsed " +
                "completionType=${completion.first} status=${completion.second}"
        )
        return true
    }

    private suspend fun awaitControlCompletion(
        requestId: String,
        cmd: String,
        expectedState: String?,
        timeoutMs: Int
    ): Pair<String, String>? {
        return withTimeoutOrNull(timeoutMs.toLong()) {
            commandRepository.messages
                .first { message ->
                    when (message) {
                        is ServerMessage.Result -> {
                            message.requestId == requestId &&
                                message.cmd == cmd &&
                                message.status == ResultStatus.COMPLETED
                        }

                        is ServerMessage.State -> {
                            expectedState != null &&
                                message.status.equals(expectedState, ignoreCase = true)
                        }

                        else -> false
                    }
                }
                .let { message ->
                    when (message) {
                        is ServerMessage.Result -> "RESULT" to message.status.name
                        is ServerMessage.State -> "STATE" to message.status
                        else -> null
                    }
                }
        }
    }

    private fun logSummary(
        category: String,
        attemptId: String,
        total: Int,
        values: List<Long>
    ) {
        val avgMs = if (values.isEmpty()) 0.0 else values.average()
        val minMs = values.minOrNull() ?: -1L
        val maxMs = values.maxOrNull() ?: -1L
        val successCount = values.size
        val failCount = total - successCount
        PerfLogger.d(
            "[$category][SUMMARY] attemptId=$attemptId total=$total success=$successCount " +
                "fail=$failCount avgMs=${"%.2f".format(avgMs)} minMs=$minMs maxMs=$maxMs"
        )
    }

    private suspend fun loadProfile(mode: String, profileName: String?): IpcDevice? {
        val devices = ipcDeviceDao.getAllDevices().first()
        if (profileName != null) {
            val matched = devices.firstOrNull { it.name == profileName }
            if (matched != null) return matched
        }

        return when (mode) {
            MODE_RESP_WIFI, MODE_CTRL_WIFI, MODE_SWITCH_WIFI_BT -> devices.firstOrNull {
                it.ip != null && it.port != null && it.macAddress != null
            } ?: devices.firstOrNull { it.ip != null && it.port != null }
            MODE_RESP_BT, MODE_RANGE_BT, MODE_CTRL_BT -> devices.firstOrNull { it.macAddress != null }
            else -> null
        }
    }

    private fun targetOf(device: IpcDevice, channel: ConnectionChannel): String {
        return when (channel) {
            ConnectionChannel.WIFI -> "${device.ip}:${device.port}"
            ConnectionChannel.BLUETOOTH -> device.macAddress ?: "unknown"
        }
    }
}
