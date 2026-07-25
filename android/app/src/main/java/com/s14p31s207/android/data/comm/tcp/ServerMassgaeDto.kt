package com.s14p31s207.android.data.comm.tcp

data class ServerMessageDto(
    val type: String,
    val id: String,
    val timestamp: String,
    val data: Map<String, Any?> = emptyMap()
)

sealed class ServerMessage {
    data class Response(
        val id: String,
        val timestamp: String,
        val requestId: String? = null,
        val cmd: String,
        val status: ResponseStatus
    ) : ServerMessage()

    data class Result(
        val id: String,
        val timestamp: String,
        val requestId: String? = null,
        val cmd: String,
        val status: ResultStatus,
        val position: Double?,
        val speed: Double?
    ) : ServerMessage()

    data class State(
        val id: String,
        val timestamp: String,
        val status: String,
        val position: Double?,
        val speed: Double?
    ) : ServerMessage()

    data class Error(
        val id: String,
        val timestamp: String,
        val errorCode: Int,
        val errorName: String,
        val category: String,
        val severity: String,
        val context: String,
        val solution: String,
        val requestId: String? = null
    ) : ServerMessage()

    data class Connect(
        val id: String,
        val channel: String,
        val status: String
    ) : ServerMessage()

    data class Unknown(val raw: String) : ServerMessage()
}

enum class ResponseStatus { ACCEPTED, REJECTED }
enum class ResultStatus { COMPLETED, FAILED }
