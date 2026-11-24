package com.nhajc.android.data.model

import kotlin.uuid.Uuid

data class User(
    val id: Uuid,
    val username: String,
    val email: String,
    val role: UserRole,
    val avatarUrl: String?
)

enum class UserRole { USER, ADMIN }
