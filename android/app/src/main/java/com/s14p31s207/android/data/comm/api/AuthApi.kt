package com.s14p31s207.android.data.comm.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.Response

data class LoginRequestDto(
    val username: String,
    val password: String
)

data class LoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val username: String,
    val name: String,
    val role: String
)

interface AuthApi {
    @POST("auth/app/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>
}