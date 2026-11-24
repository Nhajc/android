package com.nhajc.android.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.repository.LibraryRepository
import com.nhajc.android.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    playbackController: PlaybackController,
    private val libraryRepository: LibraryRepository
) : ViewModel() {
    val player: StateFlow<Player?> = playbackController.player

    private val state = MutableStateFlow(PlayerUiState())

    private val songId = MutableStateFlow<Uuid?>(null)

    private val song = songId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            libraryRepository.song(id)
        }
    }

    val uiState: StateFlow<PlayerUiState> = combine(state, song) { current, song ->
        current.copy(song = song)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    fun onSongChange(mediaId: String?) {
        songId.value = mediaId?.let { Uuid.parseOrNull(it) }
    }

    fun onFavoriteClick() {
        val current = uiState.value
        val song = current.song

        if (song == null || current.isUpdatingFavorite) {
            return
        }

        state.update { it.copy(isUpdatingFavorite = true, error = null) }

        viewModelScope.launch {
            try {
                libraryRepository.setFavorite(song.id, !song.isFavorite)
            } catch (exception: AppException) {
                state.update { it.copy(error = exception.error) }
            } finally {
                state.update { it.copy(isUpdatingFavorite = false) }
            }
        }
    }

    fun onErrorShown() {
        state.update { it.copy(error = null) }
    }
}
