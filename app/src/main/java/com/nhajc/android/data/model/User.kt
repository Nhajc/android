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

enum class UserError {
    INVALID_CREDENTIALS,
    USER_EXISTS,
    INVALID_INPUT,
    INVALID_AVATAR,
    SESSION_ENDED,
    NETWORK,
    SERVER
}

class UserException(val error: UserError, cause: Throwable? = null) : Exception(error.name, cause)
