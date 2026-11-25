package com.nhajc.android.ui.playlist

import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.Playlist

data class AddToPlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val isNewPlaylistOpen: Boolean = false,
    val isWorking: Boolean = false,
    val error: AppError? = null,
    val addedTo: String? = null
)
