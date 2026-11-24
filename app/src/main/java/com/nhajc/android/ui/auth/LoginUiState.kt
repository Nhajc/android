package com.nhajc.android.ui.auth

import com.nhajc.android.data.model.UserError

data class LoginUiState(
    val usernameOrEmail: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: UserError? = null
)
