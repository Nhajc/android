package com.nhajc.android.data.network.model

import com.nhajc.android.data.model.UserRole
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data object UpdateProfileRequest

@Serializable
data class UserResponse(
    val id: Uuid,
    val username: String,
    val email: String,
    val createdAt: Instant,
    val role: UserRole = UserRole.USER,
    val avatarUrl: String?
)

@Serializable
data class UserSummaryResponse(val id: Uuid, val username: String, val avatarUrl: String?)
