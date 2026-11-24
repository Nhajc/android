package com.nhajc.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhajc.android.data.repository.SettingsRepository
import com.nhajc.android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    userRepository: UserRepository
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = combine(
        settingsRepository.themeMode,
        userRepository.isLoggedIn
    ) { themeMode, isLoggedIn ->
        MainActivityUiState.Success(themeMode, isLoggedIn)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainActivityUiState.Loading)
}
