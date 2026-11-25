package com.nhajc.android.ui.library

import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.LibrarySettings
import com.nhajc.android.data.model.User
import kotlin.uuid.Uuid

data class LibraryUiState(
    val user: User? = null,
    val settings: LibrarySettings = LibrarySettings(),
    val query: String = "",
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val downloadProgress: Map<Uuid, Float> = emptyMap(),
    val error: AppError? = null,
    val isNewPlaylistOpen: Boolean = false,
    val isCreatingPlaylist: Boolean = false,
    val newPlaylistError: AppError? = null
)
