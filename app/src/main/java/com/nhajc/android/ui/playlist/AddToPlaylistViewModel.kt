package com.nhajc.android.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.model.Playlist
import com.nhajc.android.data.repository.PlaylistRepository
import com.nhajc.android.data.repository.UserRepository
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
class AddToPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    userRepository: UserRepository
) : ViewModel() {
    private val state = MutableStateFlow(AddToPlaylistUiState())

    private val playlists = userRepository.user.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            playlistRepository.ownedPlaylists(user.id)
        }
    }

    val uiState: StateFlow<AddToPlaylistUiState> = combine(state, playlists) { current, playlists ->
        current.copy(playlists = playlists)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddToPlaylistUiState())

    fun onAdd(playlist: Playlist, songId: Uuid) {
        run(playlist.title) { playlistRepository.addSong(playlist.id, songId) }
    }

    fun onCreate(title: String, songId: Uuid) {
        run(title) {
            val playlistId = playlistRepository.create(title)

            playlistRepository.addSong(playlistId, songId)
        }
    }

    fun onNewPlaylistOpen() {
        state.update { it.copy(isNewPlaylistOpen = true, error = null) }
    }

    fun onNewPlaylistDismiss() {
        if (state.value.isWorking) {
            return
        }

        state.update { it.copy(isNewPlaylistOpen = false, error = null) }
    }

    fun onAddedShown() {
        state.update { it.copy(addedTo = null) }
    }

    fun onDismiss() {
        state.update { it.copy(isNewPlaylistOpen = false, error = null) }
    }

    private fun run(title: String, block: suspend () -> Unit) {
        if (state.value.isWorking) {
            return
        }

        state.update { it.copy(isWorking = true, error = null) }

        viewModelScope.launch {
            try {
                block()
                state.update { it.copy(isNewPlaylistOpen = false, addedTo = title) }
            } catch (exception: AppException) {
                state.update { it.copy(error = exception.error) }
            } finally {
                state.update { it.copy(isWorking = false) }
            }
        }
    }
}
