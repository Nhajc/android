package com.nhajc.android.ui.home

import com.nhajc.android.data.model.User
import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.HomeContent

data class HomeUiState(
    val user: User? = null,
    val content: HomeContent = HomeContent(),
    val isRefreshing: Boolean = false,
    val error: AppError? = null
)
