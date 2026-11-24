package com.nhajc.android

import com.nhajc.android.data.model.ThemeMode

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(val themeMode: ThemeMode, val isLoggedIn: Boolean) : MainActivityUiState
}
