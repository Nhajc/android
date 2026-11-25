package com.nhajc.android.ui.player

import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.Song

data class PlayerUiState(
    val song: Song? = null,
    val isUpdatingFavorite: Boolean = false,
    val error: AppError? = null
)
