package com.s14p31s207.android.data.comm.tcp

object ProtocolCommands {
    const val PING = "PING"
    const val MOVE_ABSOLUTE_WHEEL = "MOVE_ABSOLUTE_WHEEL"
    const val MOVE_RELATIVE_WHEEL = "MOVE_RELATIVE_WHEEL"
    const val STOP_WHEEL = "STOP_WHEEL"
    const val JOG_START_WHEEL = "JOG_START_WHEEL"
    const val JOG_STOP_WHEEL = "JOG_STOP_WHEEL"
    const val EMERGENCY_STOP = "EMERGENCY_STOP"
    const val EMERGENCY_STOP_RESET = "EMERGENCY_STOP_RESET"
    const val RESET_ERROR = "RESET_ERROR"
    const val CONNECT = "CONNECT"
    const val MOVE_HOME = "MOVE_HOME"
    const val RESET_HOME = "RESET_HOME"
    const val TEST_MOTOR = "TEST_MOTOR"
    const val HEARTBEAT = "HEARTBEAT"
}
