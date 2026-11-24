package com.nhajc.android.ui.login

import com.nhajc.android.data.model.AccountError
import com.nhajc.android.data.model.LoginViewModel
import com.nhajc.android.data.repository.AccountRepository
import com.nhajc.android.data.repository.accountError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginController(
    private val repository: AccountRepository,
    private val scope: CoroutineScope,
    usernameOrEmail: String = ""
) {
    private val mutableModel = MutableStateFlow(
        LoginViewModel(usernameOrEmail = usernameOrEmail)
    )

    val model = mutableModel.asStateFlow()

    fun updateUsernameOrEmail(value: String) {
        mutableModel.update { it.copy(usernameOrEmail = value, error = null) }
    }

    fun updatePassword(value: String) {
        mutableModel.update { it.copy(password = value, error = null) }
    }

    fun login(onLoggedIn: () -> Unit) {
        val snapshot = model.value

        if (snapshot.isLoading) {
            return
        }

        val usernameOrEmail = snapshot.usernameOrEmail.trim()

        if (usernameOrEmail.isBlank() || snapshot.password.isBlank()) {
            mutableModel.update { it.copy(error = AccountError.REQUIRED_FIELDS) }

            return
        }

        mutableModel.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            try {
                repository.login(usernameOrEmail, snapshot.password)
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(password = "") }
                onLoggedIn()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(error = exception.accountError()) }
            } finally {
                mutableModel.update { it.copy(isLoading = false) }
            }
        }
    }
}