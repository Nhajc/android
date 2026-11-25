package com.nhajc.android.ui.release

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.model.Song
import com.nhajc.android.data.repository.DownloadRepository
import com.nhajc.android.data.repository.LibraryRepository
import com.nhajc.android.data.repository.ReleaseRepository
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

@HiltViewModel(assistedFactory = ReleaseViewModel.Factory::class)
class ReleaseViewModel @AssistedInject constructor(
    @Assisted private val releaseId: Uuid,
    private val releaseRepository: ReleaseRepository,
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: DownloadRepository,
    private val playbackController: PlaybackController
) : ViewModel() {
    private val state = MutableStateFlow(ReleaseUiState())

    val uiState: StateFlow<ReleaseUiState> = combine(
        state,
        releaseRepository.release(releaseId),
        releaseRepository.songs(releaseId),
        downloadRepository.progress
    ) { current, release, songs, downloadProgress ->
        current.copy(release = release, songs = songs, downloadProgress = downloadProgress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReleaseUiState())

    init {
        refresh()
    }

    fun refresh() {
        state.update { it.copy(error = null) }

        viewModelScope.launch {
            try {
                releaseRepository.refresh(releaseId)
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
        val release = current.release

        if (release == null || current.isUpdatingSave) {
            return
        }

        state.update { it.copy(isUpdatingSave = true, error = null) }

        viewModelScope.launch {
            try {
                releaseRepository.setSaved(release.id, !release.isSaved)
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
        val release = uiState.value.release ?: return

        viewModelScope.launch {
            try {
                if (!release.isSaved) {
                    releaseRepository.setSaved(release.id, true)
                }

                downloadRepository.setReleaseDownloaded(release.id, true)
            } catch (exception: AppException) {
                state.update { it.copy(error = exception.error) }
            }
        }
    }

    fun onRemoveDownloadClick() {
        viewModelScope.launch { downloadRepository.setReleaseDownloaded(releaseId, false) }
    }

    fun onErrorShown() {
        state.update { it.copy(error = null) }
    }

    @AssistedFactory
    interface Factory {
        fun create(releaseId: Uuid): ReleaseViewModel
    }
}
