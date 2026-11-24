package com.nhajc.android.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val username: String, val email: String, val password: String)

@Serializable
data class LoginRequest(val usernameOrEmail: String, val password: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class AuthResponse(val accessToken: String, val refreshToken: String, val user: UserResponse)
