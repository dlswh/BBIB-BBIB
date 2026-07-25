package com.s14p31s207.android.data.comm.tcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpConnectionTester @Inject constructor() {

    /**
     * 주어진 ip/port로 TCP 연결을 시도합니다.
     * 성공 시 Result.success(Unit), 실패 시 Result.failure(exception) 반환.
     *
     * @param timeoutMs 연결 타임아웃 (기본 3초)
     */
    suspend fun test(ip: String, port: Int, timeoutMs: Int = 3000): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeoutMs)
                }
            }
        }
}
