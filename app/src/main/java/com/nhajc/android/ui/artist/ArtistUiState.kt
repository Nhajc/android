package com.nhajc.android.ui.artist

import com.nhajc.android.data.model.Artist
import com.nhajc.android.data.model.ArtistError

data class ArtistUiState(
    val artist: Artist? = null,
    val isLoading: Boolean = true,
    val isUpdatingFollow: Boolean = false,
    val error: ArtistError? = null
)
