package com.nhajc.android.data.network.model

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

enum class UserRole { USER, ADMIN }

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
