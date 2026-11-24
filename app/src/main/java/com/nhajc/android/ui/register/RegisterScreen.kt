package com.nhajc.android.ui.register

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
import com.nhajc.android.R
import com.nhajc.android.data.model.RegisterViewModel
import com.nhajc.android.ui.login.AuthContainer
import com.nhajc.android.ui.login.PasswordInput
import com.nhajc.android.ui.theme.NhajcTheme

@Composable
fun RegisterScreen(
    model: RegisterViewModel,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(model.email.trim()).matches()

    val canSubmit = !model.isLoading &&
            model.username.isNotBlank() &&
            isEmailValid &&
            model.password.isNotBlank()

    AuthContainer(
        title = stringResource(R.string.register_title),
        modifier = modifier,
        onBack = onLogin
    ) {
        OutlinedTextField(
            value = model.username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !model.isLoading,
            singleLine = true,
            label = { Text(stringResource(R.string.username)) },
            leadingIcon = {
                Icon(Icons.Outlined.Person, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false
            ),
            shape = MaterialTheme.shapes.medium
        )

        OutlinedTextField(
            value = model.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !model.isLoading,
            singleLine = true,
            label = { Text(stringResource(R.string.email)) },
            placeholder = { Text(stringResource(R.string.email_placeholder)) },
            leadingIcon = {
                Icon(Icons.Outlined.Email, contentDescription = null)
            },
            isError = model.email.isNotBlank() && !isEmailValid,
            supportingText = if (model.email.isNotBlank() && !isEmailValid) {
                { Text(stringResource(R.string.invalid_email)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false
            ),
            shape = MaterialTheme.shapes.medium
        )

        PasswordInput(
            value = model.password,
            onValueChange = onPasswordChange,
            enabled = !model.isLoading,
            onSubmit = {
                if (canSubmit) {
                    onRegister()
                }
            }
        )

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
            if (model.isLoading) {
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
            enabled = !model.isLoading,
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
    var model by remember { mutableStateOf(RegisterViewModel()) }

    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        RegisterScreen(
            model = model,
            onUsernameChange = { model = model.copy(username = it) },
            onEmailChange = { model = model.copy(email = it) },
            onPasswordChange = { model = model.copy(password = it) },
            onRegister = {},
            onLogin = {}
        )
    }
}
