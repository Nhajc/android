package com.nhajc.android.ui.downloads

import com.nhajc.android.data.model.DownloadSort
import com.nhajc.android.data.model.LibraryView
import kotlin.uuid.Uuid

data class DownloadsUiState(
    val sort: DownloadSort = DownloadSort.RECENT,
    val view: LibraryView = LibraryView.LIST,
    val query: String = "",
    val isSearching: Boolean = false,
    val downloadProgress: Map<Uuid, Float> = emptyMap()
)
