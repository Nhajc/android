package com.nhajc.android.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nhajc.android.data.model.DownloadSort
import com.nhajc.android.data.model.LibraryView
import com.nhajc.android.data.model.Song
import com.nhajc.android.data.repository.DownloadRepository
import com.nhajc.android.data.repository.LibraryRepository
import com.nhajc.android.data.repository.SettingsRepository
import com.nhajc.android.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackController: PlaybackController,
    downloadRepository: DownloadRepository
) : ViewModel() {
    private val state = MutableStateFlow(DownloadsUiState())

    private val settings = settingsRepository.librarySettings

    private val sort = settings.map { it.downloadSort }.distinctUntilChanged()

    private val query = state.map { it.query }.distinctUntilChanged()

    val uiState: StateFlow<DownloadsUiState> = combine(
        state,
        settings,
        downloadRepository.progress
    ) { current, settings, downloadProgress ->
        current.copy(
            sort = settings.downloadSort,
            view = settings.view,
            downloadProgress = downloadProgress
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    val songs: Flow<PagingData<Song>> = combine(query, sort, ::Pair)
        .flatMapLatest { (query, sort) -> libraryRepository.downloads(query, sort) }
        .cachedIn(viewModelScope)

    fun onSongClick(index: Int) {
        val current = uiState.value

        viewModelScope.launch {
            val songs = libraryRepository.downloadList(current.query, current.sort)

            playbackController.play(songs, index)
        }
    }

    fun onSortChange(sort: DownloadSort) {
        viewModelScope.launch { settingsRepository.setDownloadSort(sort) }
    }

    fun onViewChange() {
        val views = LibraryView.entries
        val current = uiState.value.view
        val next = views[(current.ordinal + 1) % views.size]

        viewModelScope.launch { settingsRepository.setLibraryView(next) }
    }

    fun onSearchOpen() {
        state.update { it.copy(isSearching = true) }
    }

    fun onSearchClose() {
        state.update { it.copy(isSearching = false, query = "") }
    }

    fun onQueryChange(query: String) {
        state.update { it.copy(query = query) }
    }
}
