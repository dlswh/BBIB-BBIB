package com.s14p31s207.android.data.repository

import com.s14p31s207.android.data.comm.api.AuthApi
import com.s14p31s207.android.data.comm.api.LoginRequestDto
import com.s14p31s207.android.domain.repository.AuthRepository
import com.s14p31s207.android.feature.auth.session.SessionState
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi
) : AuthRepository {

    private var token: String? = null

    override suspend fun login(username: String, password: String): Result<Unit> {
        return try {
            val response = authApi.login(LoginRequestDto(username, password))
            token = response.accessToken
            SessionState.name = response.name      // ← 추가
            SessionState.role = response.role      // ← 추가
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val currentToken = token ?: return Result.failure(Exception("토큰이 없습니다"))
            authApi.logout("Bearer $currentToken")
            token = null  // ✅ 로그아웃 후 토큰 초기화
            SessionState.name = ""
            SessionState.role = ""
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isLoggedIn(): Boolean = token != null
}