package com.nhajc.android.data.model

import com.nhajc.android.data.datastore.AudioQuality
import com.nhajc.android.data.datastore.ThemeMode

enum class AccountError {
    REQUIRED_FIELDS,
    INVALID_EMAIL,
    INVALID_CREDENTIALS,
    ACCOUNT_EXISTS,
    UNAUTHORIZED,
    INVALID_AVATAR,
    NETWORK,
    SERVER,
    STORAGE,
    UNKNOWN
}

data class AccountViewModel(
    val username: String,
    val email: String,
    val avatarUrl: String? = null
)

data class LoginViewModel(
    val usernameOrEmail: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AccountError? = null
)

data class RegisterViewModel(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AccountError? = null
)

data class SettingsViewModel(
    val account: AccountViewModel? = null,
    val audioQuality: AudioQuality? = null,
    val downloadQuality: AudioQuality? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = false,
    val isUpdatingAvatar: Boolean = false,
    val isLoggingOut: Boolean = false,
    val error: AccountError? = null
)