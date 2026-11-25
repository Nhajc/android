package com.nhajc.android.ui.playlist

import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.Playlist
import com.nhajc.android.data.model.Song

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isOwned: Boolean = false,
    val isUpdatingSave: Boolean = false,
    val isDeleteOpen: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val error: AppError? = null
)
