package com.nhajc.android.ui.login

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
fun LoginRoute(
    repository: AccountRepository,
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit
) {
    var usernameOrEmail by rememberSaveable { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val controller = remember(repository, scope) {
        LoginController(repository, scope, usernameOrEmail)
    }

    val model by controller.model.collectAsStateWithLifecycle()

    LoginScreen(
        model = model,
        onUsernameOrEmailChange = {
            usernameOrEmail = it
            controller.updateUsernameOrEmail(it)
        },
        onPasswordChange = controller::updatePassword,
        onLogin = { controller.login(onLoggedIn) },
        onRegister = onRegister
    )
}
