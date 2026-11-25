package com.nhajc.android.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.model.Song
import com.nhajc.android.data.repository.DownloadRepository
import com.nhajc.android.data.repository.LibraryRepository
import com.nhajc.android.data.repository.PlaylistRepository
import com.nhajc.android.data.repository.UserRepository
import com.nhajc.android.playback.PlaybackController
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = PlaylistViewModel.Factory::class)
class PlaylistViewModel @AssistedInject constructor(
    @Assisted private val playlistId: Uuid,
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: DownloadRepository,
    private val playbackController: PlaybackController,
    userRepository: UserRepository
) : ViewModel() {
    private val state = MutableStateFlow(PlaylistUiState())

    val uiState: StateFlow<PlaylistUiState> = combine(
        state,
        playlistRepository.playlist(playlistId),
        playlistRepository.songs(playlistId),
        downloadRepository.progress,
        userRepository.user
    ) { current, playlist, songs, downloadProgress, user ->
        val isOwned = playlist != null && playlist.owner.id == user?.id

        current.copy(
            playlist = playlist,
            songs = songs,
            downloadProgress = downloadProgress,
            isOwned = isOwned
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistUiState())

    init {
        refresh()
    }

    fun refresh() {
        state.update { it.copy(error = null) }

        viewModelScope.launch {
            try {
                playlistRepository.refresh(playlistId)
            } catch (exception: AppException) {
                state.update { it.copy(error = exception.error) }
            }
        }
    }

    fun onSongClick(index: Int) {
        viewModelScope.launch { playbackController.play(uiState.value.songs, index) }
    }

    fun onPlayClick() {
        onSongClick(0)
    }

    fun onShuffleClick() {
        viewModelScope.launch { playbackController.shuffle(uiState.value.songs) }
    }

    fun onSaveClick() {
        val current = uiState.value
        val playlist = current.playlist

        if (playlist == null || current.isOwned || current.isUpdatingSave) {
            return
        }

        state.update { it.copy(isUpdatingSave = true, error = null) }

        viewModelScope.launch {
            try {
                playlistRepository.setSaved(playlist.id, !playlist.isSaved)
            } catch (exception: AppException) {
                state.update { it.copy(error = exception.error) }
            } finally {
                state.update { it.copy(isUpdatingSave = false) }
            }
        }
    }

    fun onFavoriteClick(song: Song) {
        viewModelScope.launch {
            try {
                libraryRepository.setFavorite(song.id, !song.isFavorite)
            } catch (exception: AppException) {
                state.update { it.copy(error = exception.error) }
            }
        }
    }

    fun onSongDownloadClick(song: Song) {
        viewModelScope.launch {
            downloadRepository.setSongDownloaded(song.id, song.fileQuality == null)
        }
    }

    fun onDownloadClick() {
        val current = uiState.value
        val playlist = current.playlist ?: return

        viewModelScope.launch {
            try {
                if (!current.isOwned && !playlist.isSaved) {
                    playlistRepository.setSaved(playlist.id, true)
                }

                downloadRepository.setPlaylistDownloaded(playlist.id, true)
            } catch (exception: AppException) {
                state.update { it.copy(error = exception.error) }
            }
        }
    }

    fun onRemoveDownloadClick() {
        viewModelScope.launch { downloadRepository.setPlaylistDownloaded(playlistId, false) }
    }

    fun onDeleteClick() {
        state.update { it.copy(isDeleteOpen = true) }
    }

    fun onDeleteDismiss() {
        if (state.value.isDeleting) {
            return
        }

        state.update { it.copy(isDeleteOpen = false) }
    }

    fun onDeleteConfirm() {
        if (state.value.isDeleting) {
            return
        }

        state.update { it.copy(isDeleting = true, error = null) }

        viewModelScope.launch {
            try {
                playlistRepository.delete(playlistId)
                state.update { it.copy(isDeleteOpen = false, isDeleted = true) }
            } catch (exception: AppException) {
                state.update { it.copy(isDeleteOpen = false, error = exception.error) }
            } finally {
                state.update { it.copy(isDeleting = false) }
            }
        }
    }

    fun onErrorShown() {
        state.update { it.copy(error = null) }
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistId: Uuid): PlaylistViewModel
    }
}
