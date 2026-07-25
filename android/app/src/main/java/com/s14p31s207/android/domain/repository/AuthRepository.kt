package com.s14p31s207.android.domain.repository

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun isLoggedIn(): Boolean
}