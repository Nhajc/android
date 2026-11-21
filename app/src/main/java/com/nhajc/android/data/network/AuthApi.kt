package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.AuthResponse
import com.nhajc.android.data.network.model.LoginRequest
import com.nhajc.android.data.network.model.RefreshTokenRequest
import com.nhajc.android.data.network.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest)
}
