package com.nhajc.android.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhajc.android.data.model.ArtistException
import com.nhajc.android.data.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val artistRepository: ArtistRepository
) : ViewModel() {
    private val state = MutableStateFlow(ArtistUiState())
    private var requestedArtistId: Uuid? = null
    private var loadJob: Job? = null
    private var followJob: Job? = null

    val uiState = state.asStateFlow()

    fun loadArtist(id: Uuid) {
        if (requestedArtistId == id && (state.value.artist != null || state.value.isLoading)) {
            return
        }

        fetchArtist(id)
    }

    fun retry() {
        val id = requestedArtistId ?: return

        if (state.value.isLoading) {
            return
        }

        fetchArtist(id)
    }

    private fun fetchArtist(id: Uuid) {
        loadJob?.cancel()
        followJob?.cancel()

        requestedArtistId = id
        state.value = ArtistUiState(isLoading = true)

        loadJob = viewModelScope.launch {
            try {
                val artist = artistRepository.getArtist(id)
                currentCoroutineContext().ensureActive()

                state.update { it.copy(artist = artist, isLoading = false) }
            } catch (exception: ArtistException) {
                currentCoroutineContext().ensureActive()

                state.update { it.copy(isLoading = false, error = exception.error) }
            }
        }
    }

    fun toggleFollow() {
        val current = state.value
        val artist = current.artist ?: return

        if (current.isLoading || current.isUpdatingFollow) {
            return
        }

        val followed = !artist.isFollowed

        state.update { it.copy(isUpdatingFollow = true, error = null) }

        followJob = viewModelScope.launch {
            try {
                artistRepository.setFollowed(artist.id, followed)
                currentCoroutineContext().ensureActive()

                state.update {
                    it.copy(
                        artist = artist.copy(isFollowed = followed),
                        isUpdatingFollow = false
                    )
                }
            } catch (exception: ArtistException) {
                currentCoroutineContext().ensureActive()

                state.update {
                    it.copy(isUpdatingFollow = false, error = exception.error)
                }
            }
        }
    }
}
