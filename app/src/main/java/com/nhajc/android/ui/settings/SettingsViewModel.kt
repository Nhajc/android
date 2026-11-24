package com.nhajc.android.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.model.AudioQuality
import com.nhajc.android.data.model.ThemeMode
import com.nhajc.android.data.repository.SettingsRepository
import com.nhajc.android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val state = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        state,
        userRepository.user,
        settingsRepository.audioQuality,
        settingsRepository.downloadQuality,
        settingsRepository.themeMode
    ) { current, user, audioQuality, downloadQuality, themeMode ->
        current.copy(
            user = user,
            audioQuality = audioQuality,
            downloadQuality = downloadQuality,
            themeMode = themeMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch {
            try {
                userRepository.refreshUser()
            } catch (_: AppException) {
            }
        }
    }

    fun onAudioQualityChange(quality: AudioQuality) {
        viewModelScope.launch { settingsRepository.setAudioQuality(quality) }
    }

    fun onDownloadQualityChange(quality: AudioQuality) {
        viewModelScope.launch { settingsRepository.setDownloadQuality(quality) }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun onAvatarPicked(uri: Uri) {
        updateAvatar(uri)
    }

    fun onRemoveAvatar() {
        updateAvatar(null)
    }

    fun logout() {
        if (state.value.isLoggingOut) {
            return
        }

        state.update { it.copy(isLoggingOut = true, error = null) }

        viewModelScope.launch {
            userRepository.logout()
        }
    }

    private fun updateAvatar(uri: Uri?) {
        if (state.value.isUpdatingAvatar) {
            return
        }

        state.update { it.copy(isUpdatingAvatar = true, error = null) }

        viewModelScope.launch {
            try {
                userRepository.updateAvatar(uri)
            } catch (exception: AppException) {
                state.update { it.copy(error = exception.error) }
            } finally {
                state.update { it.copy(isUpdatingAvatar = false) }
            }
        }
    }
}
