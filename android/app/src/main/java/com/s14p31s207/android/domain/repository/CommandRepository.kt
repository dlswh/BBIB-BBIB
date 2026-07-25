package com.s14p31s207.android.domain.repository

import com.s14p31s207.android.data.comm.tcp.CommandDto
import com.s14p31s207.android.data.comm.tcp.ServerMessage
import kotlinx.coroutines.flow.SharedFlow

interface CommandRepository {
    // 서버에서 오는 모든 메시지 스트림
    val messages: SharedFlow<ServerMessage>

    // 명령어 전송
    suspend fun send(command: CommandDto): Result<Unit>
}