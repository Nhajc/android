package com.nhajc.android.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.model.ArtistSort
import com.nhajc.android.data.model.ArtistSummary
import com.nhajc.android.data.model.LibrarySort
import com.nhajc.android.data.model.LibraryTab
import com.nhajc.android.data.model.LibraryView
import com.nhajc.android.data.model.Playlist
import com.nhajc.android.data.model.Release
import com.nhajc.android.data.model.Song
import com.nhajc.android.data.repository.LibraryRepository
import com.nhajc.android.data.repository.SettingsRepository
import com.nhajc.android.data.repository.UserRepository
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
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackController: PlaybackController,
    userRepository: UserRepository
) : ViewModel() {
    private val state = MutableStateFlow(LibraryUiState())

    private val settings = settingsRepository.librarySettings

    private val query = state.map { it.query }.distinctUntilChanged()

    val uiState: StateFlow<LibraryUiState> = combine(
        state,
        settings,
        userRepository.user
    ) { current, settings, user ->
        current.copy(settings = settings, user = user)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    val releases: Flow<PagingData<Release>> =
        combine(query, settings.map { it.releaseSort }.distinctUntilChanged(), ::Pair)
            .flatMapLatest { (query, sort) -> libraryRepository.releases(query, sort) }
            .cachedIn(viewModelScope)

    val playlists: Flow<PagingData<Playlist>> =
        combine(query, settings.map { it.playlistSort }.distinctUntilChanged(), ::Pair)
            .flatMapLatest { (query, sort) -> libraryRepository.playlists(query, sort) }
            .cachedIn(viewModelScope)

    val artists: Flow<PagingData<ArtistSummary>> =
        combine(query, settings.map { it.artistSort }.distinctUntilChanged(), ::Pair)
            .flatMapLatest { (query, sort) -> libraryRepository.artists(query, sort) }
            .cachedIn(viewModelScope)

    val favorites: Flow<PagingData<Song>> =
        combine(query, settings.map { it.favoriteSort }.distinctUntilChanged(), ::Pair)
            .flatMapLatest { (query, sort) -> libraryRepository.songs(query, sort) }
            .cachedIn(viewModelScope)

    fun onFavoriteClick(index: Int) {
        val current = uiState.value

        viewModelScope.launch {
            val songs = libraryRepository.favoriteList(current.query, current.settings.favoriteSort)

            playbackController.play(songs, index)
        }
    }

    fun onTabSelect(tab: LibraryTab) {
        viewModelScope.launch { settingsRepository.setLibraryTab(tab) }
    }

    fun onSortChange(sort: LibrarySort) {
        viewModelScope.launch {
            when (uiState.value.settings.tab) {
                LibraryTab.RELEASES -> settingsRepository.setReleaseSort(sort)
                LibraryTab.PLAYLISTS -> settingsRepository.setPlaylistSort(sort)
                LibraryTab.FAVORITES -> settingsRepository.setFavoriteSort(sort)
                LibraryTab.ARTISTS -> Unit
            }
        }
    }

    fun onArtistSortChange(sort: ArtistSort) {
        viewModelScope.launch { settingsRepository.setArtistSort(sort) }
    }

    fun onViewChange() {
        val views = LibraryView.entries
        val current = uiState.value.settings.view
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

    fun onRefresh() {
        if (state.value.isRefreshing) {
            return
        }

        state.update { it.copy(isRefreshing = true, error = null) }

        viewModelScope.launch {
            try {
                libraryRepository.sync()
            } catch (exception: AppException) {
                state.update { it.copy(error = exception.error) }
            } finally {
                state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onErrorShown() {
        state.update { it.copy(error = null) }
    }

    fun onNewPlaylistOpen() {
        state.update { it.copy(isNewPlaylistOpen = true, newPlaylistError = null) }
    }

    fun onNewPlaylistDismiss() {
        if (state.value.isCreatingPlaylist) {
            return
        }

        state.update { it.copy(isNewPlaylistOpen = false) }
    }

    fun onCreatePlaylist(title: String) {
        if (state.value.isCreatingPlaylist) {
            return
        }

        state.update { it.copy(isCreatingPlaylist = true, newPlaylistError = null) }

        viewModelScope.launch {
            try {
                libraryRepository.createPlaylist(title)
                state.update { it.copy(isNewPlaylistOpen = false) }
            } catch (exception: AppException) {
                state.update { it.copy(newPlaylistError = exception.error) }
            } finally {
                state.update { it.copy(isCreatingPlaylist = false) }
            }
        }
    }
}
