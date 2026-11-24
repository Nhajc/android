package com.nhajc.android.ui.settings

import com.nhajc.android.data.model.AudioQuality
import com.nhajc.android.data.model.ThemeMode
import com.nhajc.android.data.model.User
import com.nhajc.android.data.model.UserError

data class SettingsUiState(
    val user: User? = null,
    val audioQuality: AudioQuality = AudioQuality.LOSSLESS,
    val downloadQuality: AudioQuality = AudioQuality.LOSSLESS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isUpdatingAvatar: Boolean = false,
    val isLoggingOut: Boolean = false,
    val error: UserError? = null
)
