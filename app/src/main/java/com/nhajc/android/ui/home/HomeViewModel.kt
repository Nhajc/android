package com.nhajc.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.model.HomeContent
import com.nhajc.android.data.model.LibraryTab
import com.nhajc.android.data.repository.HomeRepository
import com.nhajc.android.data.repository.SettingsRepository
import com.nhajc.android.data.repository.UserRepository
import com.nhajc.android.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val settings: SettingsRepository,
    private val playback: PlaybackController,
    users: UserRepository
) : ViewModel() {
    private val state = MutableStateFlow(HomeUiState())
    private var refreshJob: Job? = null
    private var playJob: Job? = null
    private var actor: Uuid? = null

    val uiState = combine(
        state, users.user,
        repository.content.catch { exception ->
            if (exception is CancellationException) throw exception
            state.update { it.copy(error = AppError.SERVER) }
            emit(HomeContent())
        }
    ) { current, user, content ->
        current.copy(user = user, content = content)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            users.user.map { it?.id }.distinctUntilChanged().collect { id ->
                refreshJob?.cancel()
                playJob?.cancel()
                actor = id
                state.value = HomeUiState()
                if (id != null) refresh()
            }
        }
    }

    fun refresh() {
        if (actor == null || refreshJob?.isActive == true) return
        state.update { it.copy(isRefreshing = true, error = null) }
        refreshJob = viewModelScope.launch {
            try {
                repository.refresh()
                currentCoroutineContext().ensureActive()
                state.update { it.copy(isRefreshing = false) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                state.update {
                    it.copy(isRefreshing = false,
                        error = (exception as? AppException)?.error ?: AppError.SERVER)
                }
            }
        }
    }

    fun openLibrary(tab: LibraryTab, navigate: () -> Unit) {
        viewModelScope.launch {
            try {
                settings.setLibraryTab(tab)
                navigate()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                state.update { it.copy(error = AppError.SERVER) }
            }
        }
    }

    fun playFavorite(id: Uuid) {
        if (playJob?.isActive == true) return
        val songs = uiState.value.content.favorites
        val index = songs.indexOfFirst { it.id == id }
        if (index < 0) return
        playJob = viewModelScope.launch {
            try {
                playback.play(songs, index)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                state.update { it.copy(error = (exception as? AppException)?.error ?: AppError.SERVER) }
            }
        }
    }
}
