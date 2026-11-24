package com.nhajc.android.ui.auth

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.R
import com.nhajc.android.data.model.AppError
import com.nhajc.android.ui.common.message
import com.nhajc.android.ui.theme.NhajcTheme

@Composable
fun LoginScreen(onRegister: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        uiState = uiState,
        onUsernameOrEmailChange = viewModel::onUsernameOrEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = viewModel::login,
        onRegister = onRegister
    )
}

@Composable
internal fun LoginScreen(
    uiState: LoginUiState,
    onUsernameOrEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSubmit = !uiState.isLoading &&
        uiState.usernameOrEmail.isNotBlank() &&
        uiState.password.isNotEmpty()

    AuthContainer(
        title = stringResource(R.string.login_title),
        modifier = modifier
    ) {
        OutlinedTextField(
            value = uiState.usernameOrEmail,
            onValueChange = onUsernameOrEmailChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            singleLine = true,
            label = { Text(stringResource(R.string.username_or_email)) },
            leadingIcon = {
                Icon(Icons.Outlined.Person, contentDescription = null)
            },
            supportingText = { Text("") },
            keyboardOptions = KeyboardOptions(
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
                    onLogin()
                }
            }
        )

        if (uiState.error != null) {
            Text(
                text = stringResource(uiState.error.message()),
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onLogin,
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
                    text = stringResource(R.string.log_in),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        OutlinedButton(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            enabled = !uiState.isLoading,
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(stringResource(R.string.sign_up))
        }
    }
}

@Preview(
    name = "Login Light",
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Login Dark",
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun LoginScreenPreview() {
    var uiState by remember { mutableStateOf(LoginUiState()) }

    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        LoginScreen(
            uiState = uiState,
            onUsernameOrEmailChange = {
                uiState = uiState.copy(usernameOrEmail = it)
            },
            onPasswordChange = {
                uiState = uiState.copy(password = it)
            },
            onLogin = {},
            onRegister = {}
        )
    }
}
