package com.nhajc.android.ui.release

import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.Release
import com.nhajc.android.data.model.Song

data class ReleaseUiState(
    val release: Release? = null,
    val songs: List<Song> = emptyList(),
    val isUpdatingSave: Boolean = false,
    val error: AppError? = null
)
