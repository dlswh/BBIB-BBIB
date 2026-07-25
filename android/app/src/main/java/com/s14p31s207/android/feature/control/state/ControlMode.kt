package com.s14p31s207.android.feature.control.state

enum class ControlMode {
    DISCONNECTED,
    IDLE,
    MOVING,
    JOGGING,
    ERROR,
    E_STOPPED,
}

private const val STATUS_IDLE = "IDLE"
private const val STATUS_MOVING = "MOVING"
private const val STATUS_JOGGING = "JOGGING"
private const val STATUS_ERROR = "ERROR"
private const val STATUS_ESTOPPED = "E_STOPPED"

fun String?.toControlMode(): ControlMode {
    return when (this?.trim()?.uppercase()) {
        STATUS_MOVING -> ControlMode.MOVING
        STATUS_JOGGING -> ControlMode.JOGGING
        STATUS_ERROR -> ControlMode.ERROR
        STATUS_ESTOPPED -> ControlMode.E_STOPPED
        STATUS_IDLE,
        null -> ControlMode.IDLE
        else -> ControlMode.IDLE
    }
}
