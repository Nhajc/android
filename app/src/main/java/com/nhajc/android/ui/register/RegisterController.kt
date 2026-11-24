package com.nhajc.android.ui.register

import android.util.Patterns
import com.nhajc.android.data.model.AccountError
import com.nhajc.android.data.model.RegisterViewModel
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

class RegisterController(
    private val repository: AccountRepository,
    private val scope: CoroutineScope,
    username: String = "",
    email: String = ""
) {
    private val mutableModel = MutableStateFlow(
        RegisterViewModel(username = username, email = email)
    )

    val model = mutableModel.asStateFlow()

    fun updateUsername(value: String) {
        mutableModel.update { it.copy(username = value, error = null) }
    }

    fun updateEmail(value: String) {
        mutableModel.update { it.copy(email = value, error = null) }
    }

    fun updatePassword(value: String) {
        mutableModel.update { it.copy(password = value, error = null) }
    }

    fun register(onRegistered: () -> Unit) {
        val snapshot = model.value

        if (snapshot.isLoading) {
            return
        }

        val username = snapshot.username.trim()
        val email = snapshot.email.trim()

        if (username.isBlank() || email.isBlank() || snapshot.password.isBlank()) {
            mutableModel.update { it.copy(error = AccountError.REQUIRED_FIELDS) }

            return
        }

        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()

        if (!isEmailValid) {
            mutableModel.update { it.copy(error = AccountError.INVALID_EMAIL) }

            return
        }

        mutableModel.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            try {
                repository.register(username, email, snapshot.password)
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(password = "") }
                onRegistered()
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