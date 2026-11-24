package com.nhajc.android.ui.common

import androidx.annotation.StringRes
import com.nhajc.android.R
import com.nhajc.android.data.model.AppError

@StringRes
fun AppError.message(): Int = when (this) {
    AppError.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
    AppError.USER_EXISTS -> R.string.error_user_exists
    AppError.INVALID_INPUT -> R.string.error_invalid_input
    AppError.INVALID_AVATAR -> R.string.error_invalid_avatar
    AppError.PLAYLIST_LIMIT -> R.string.error_playlist_limit
    AppError.NOT_FOUND -> R.string.error_not_found
    AppError.SESSION_ENDED -> R.string.error_session_ended
    AppError.NETWORK -> R.string.error_network
    AppError.SERVER -> R.string.error_server
}
