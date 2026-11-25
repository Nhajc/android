package com.nhajc.android.ui.release

import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.Release
import com.nhajc.android.data.model.Song
import kotlin.uuid.Uuid

data class ReleaseUiState(
    val release: Release? = null,
    val songs: List<Song> = emptyList(),
    val downloadProgress: Map<Uuid, Float> = emptyMap(),
    val isUpdatingSave: Boolean = false,
    val error: AppError? = null
) {
    val canDownload: Boolean
        get() = songs.any { it.fileQuality == null }

    val canRemoveDownload: Boolean
        get() = songs.any { it.fileQuality != null }
}
