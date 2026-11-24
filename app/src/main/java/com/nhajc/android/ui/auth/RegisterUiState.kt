package com.nhajc.android.ui.auth

import com.nhajc.android.data.model.UserError

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: UserError? = null
)
