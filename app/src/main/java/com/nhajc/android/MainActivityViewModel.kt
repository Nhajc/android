package com.nhajc.android

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.repository.LibraryRepository
import com.nhajc.android.data.repository.SettingsRepository
import com.nhajc.android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "MainActivityViewModel"

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    userRepository: UserRepository,
    libraryRepository: LibraryRepository
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = combine(
        settingsRepository.themeMode,
        userRepository.isLoggedIn
    ) { themeMode, isLoggedIn ->
        MainActivityUiState.Success(themeMode, isLoggedIn)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainActivityUiState.Loading)

    init {
        viewModelScope.launch {
            userRepository.isLoggedIn.distinctUntilChanged().collectLatest { isLoggedIn ->
                if (isLoggedIn) {
                    try {
                        libraryRepository.sync()
                    } catch (exception: AppException) {
                        Log.w(TAG, "Library sync failed", exception)
                    }
                }
            }
        }
    }
}
