package com.nhajc.android.data.model

enum class AppError {
    INVALID_CREDENTIALS,
    USER_EXISTS,
    INVALID_INPUT,
    INVALID_AVATAR,
    PLAYLIST_LIMIT,
    NOT_FOUND,
    SESSION_ENDED,
    NETWORK,
    SERVER
}

class AppException(val error: AppError, cause: Throwable? = null) : Exception(error.name, cause)
