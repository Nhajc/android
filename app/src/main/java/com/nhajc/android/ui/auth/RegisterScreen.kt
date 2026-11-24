package com.nhajc.android.ui.auth

import android.content.res.Configuration
import android.util.Patterns
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.R
import com.nhajc.android.ui.common.message
import com.nhajc.android.ui.theme.NhajcTheme

@Composable
fun RegisterScreen(onLogin: () -> Unit, viewModel: RegisterViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RegisterScreen(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onRegister = viewModel::register,
        onLogin = onLogin
    )
}

@Composable
internal fun RegisterScreen(
    uiState: RegisterUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val username = uiState.username.trim()
    val isUsernameValid =
        username.length in 3..50 && username.none { it == '@' || it.isWhitespace() }

    val email = uiState.email.trim()
    val isEmailValid = email.length <= 254 && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    val passwordBytes = uiState.password.encodeToByteArray().size
    val isPasswordValid = uiState.password.length >= 12 && passwordBytes <= 72

    val canSubmit = !uiState.isLoading && isUsernameValid && isEmailValid && isPasswordValid

    AuthContainer(
        title = stringResource(R.string.register_title),
        modifier = modifier
    ) {
        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            singleLine = true,
            label = { Text(stringResource(R.string.username)) },
            leadingIcon = {
                Icon(Icons.Outlined.Person, contentDescription = null)
            },
            isError = uiState.username.isNotEmpty() && !isUsernameValid,
            supportingText = { Text(stringResource(R.string.username_rule)) },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false
            ),
            shape = MaterialTheme.shapes.medium
        )

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            singleLine = true,
            label = { Text(stringResource(R.string.email)) },
            placeholder = { Text(stringResource(R.string.email_placeholder)) },
            leadingIcon = {
                Icon(Icons.Outlined.Email, contentDescription = null)
            },
            isError = uiState.email.isNotBlank() && !isEmailValid,
            supportingText = {
                if (uiState.email.isNotBlank() && !isEmailValid) {
                    Text(stringResource(R.string.invalid_email))
                } else {
                    Text("")
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false
            ),
            shape = MaterialTheme.shapes.medium
        )

        PasswordInput(
            value = uiState.password,
            onValueChange = onPasswordChange,
            enabled = !uiState.isLoading,
            onSubmit = {
                if (canSubmit) {
                    onRegister()
                }
            },
            isError = uiState.password.isNotEmpty() && !isPasswordValid,
            supportingText = stringResource(R.string.password_rule)
        )

        if (uiState.error != null) {
            Text(
                text = stringResource(uiState.error.message()),
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            enabled = canSubmit,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.sign_up),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        OutlinedButton(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            enabled = !uiState.isLoading,
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(stringResource(R.string.log_in))
        }
    }
}

@Preview(
    name = "Register Light",
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Register Dark",
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RegisterScreenPreview() {
    var uiState by remember { mutableStateOf(RegisterUiState()) }

    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        RegisterScreen(
            uiState = uiState,
            onUsernameChange = { uiState = uiState.copy(username = it) },
            onEmailChange = { uiState = uiState.copy(email = it) },
            onPasswordChange = { uiState = uiState.copy(password = it) },
            onRegister = {},
            onLogin = {}
        )
    }
}
