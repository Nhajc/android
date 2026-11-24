package com.nhajc.android.ui.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.data.repository.AccountRepository

@Composable
fun RegisterRoute(
    repository: AccountRepository,
    onRegistered: () -> Unit,
    onLogin: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val controller = remember(repository, scope) {
        RegisterController(repository, scope, username, email)
    }

    val model by controller.model.collectAsStateWithLifecycle()

    RegisterScreen(
        model = model,
        onUsernameChange = {
            username = it
            controller.updateUsername(it)
        },
        onEmailChange = {
            email = it
            controller.updateEmail(it)
        },
        onPasswordChange = controller::updatePassword,
        onRegister = { controller.register(onRegistered) },
        onLogin = onLogin
    )
}
