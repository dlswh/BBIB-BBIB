package com.s14p31s207.android.data.comm.tcp

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 전송할 명령어 최상위 구조
data class CommandDto(
    val cmd: String,
    val id: String,
    val timestamp: String = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
    val data: Map<String, Any> = emptyMap()
)

// 명령어 종류별 생성 헬퍼
object CommandFactory {

    fun ping(id: String) = CommandDto(
        cmd = ProtocolCommands.PING,
        id = id
    )

    fun moveAbsolute(id: String, position: Double, speed: Double) = CommandDto(
        cmd = ProtocolCommands.MOVE_ABSOLUTE_WHEEL,
        id = id,
        data = mapOf("position" to position, "speed" to speed)
    )

    fun moveRelative(id: String, direction: Direction, distance: Double, speed: Double) =
        CommandDto(
            cmd = ProtocolCommands.MOVE_RELATIVE_WHEEL,
            id = id,
            data = mapOf("direction" to direction.name, "distance" to distance, "speed" to speed)
        )

    fun stop(id: String) = CommandDto(
        cmd = ProtocolCommands.STOP_WHEEL,
        id = id
    )

    fun jogStart(id: String, direction: Direction) = CommandDto(
        cmd = ProtocolCommands.JOG_START_WHEEL,
        id = id,
        data = mapOf("direction" to direction.name)
    )

    fun jogStop(id: String) = CommandDto(
        cmd = ProtocolCommands.JOG_STOP_WHEEL,
        id = id
    )

    fun emergencyStop(id: String) = CommandDto(
        cmd = ProtocolCommands.EMERGENCY_STOP,
        id = id
    )

    fun emergencyStopReset(id: String) = CommandDto(
        cmd = ProtocolCommands.EMERGENCY_STOP_RESET,
        id = id
    )

    fun resetError(id: String, errorId: String) = CommandDto(
        cmd = ProtocolCommands.RESET_ERROR,
        id = id,
        data = mapOf("errorId" to errorId)
    )

    fun moveHome(id: String) = CommandDto(
        cmd = ProtocolCommands.MOVE_HOME,
        id = id
    )

    fun resetHome(id: String) = CommandDto(
        cmd = ProtocolCommands.RESET_HOME,
        id = id
    )

    fun testMotor(id: String, direction: Direction, motorIndex: Int) = CommandDto(
        cmd = ProtocolCommands.TEST_MOTOR,
        id = id,
        data = mapOf("direction" to direction.name, "motorId" to motorIndex)
    )

    fun connect(id: String, channel: Channel) = CommandDto(
        cmd = ProtocolCommands.CONNECT,
        id = id,
        data = mapOf("channel" to channel.name)
    )
}

enum class Direction { POS, NEG }
enum class Channel { WIFI, BLUETOOTH }
