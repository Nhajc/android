package com.nhajc.android.ui.login

import android.content.res.Configuration
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
import com.nhajc.android.R
import com.nhajc.android.data.model.LoginViewModel
import com.nhajc.android.ui.theme.NhajcTheme

@Composable
fun LoginScreen(
    model: LoginViewModel,
    onUsernameOrEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSubmit = !model.isLoading &&
            model.usernameOrEmail.isNotBlank() &&
            model.password.isNotBlank()

    AuthContainer(
        title = stringResource(R.string.login_title),
        modifier = modifier
    ) {
        OutlinedTextField(
            value = model.usernameOrEmail,
            onValueChange = onUsernameOrEmailChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !model.isLoading,
            singleLine = true,
            label = { Text(stringResource(R.string.username_or_email)) },
            leadingIcon = {
                Icon(Icons.Outlined.Person, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(
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
                    onLogin()
                }
            }
        )

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
            if (model.isLoading) {
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
            enabled = !model.isLoading,
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(stringResource(R.string.sign_up))
        }
    }
}

@Composable
internal fun AuthContainer(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }

                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                content()
            }
        }
    }
}

@Composable
internal fun PasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSubmit: () -> Unit
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }

    val transformation = if (isVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }

    val visibilityIcon = if (isVisible) {
        Icons.Outlined.VisibilityOff
    } else {
        Icons.Outlined.Visibility
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(stringResource(R.string.password)) },
        leadingIcon = {
            Icon(Icons.Outlined.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(
                onClick = { isVisible = !isVisible },
                enabled = enabled
            ) {
                Icon(
                    imageVector = visibilityIcon,
                    contentDescription = if (isVisible) {
                        stringResource(R.string.hide_password)
                    } else {
                        stringResource(R.string.show_password)
                    }
                )
            }
        },
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (enabled) {
                    onSubmit()
                }
            }
        ),
        shape = MaterialTheme.shapes.medium
    )
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
    var model by remember { mutableStateOf(LoginViewModel()) }

    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        LoginScreen(
            model = model,
            onUsernameOrEmailChange = {
                model = model.copy(usernameOrEmail = it)
            },
            onPasswordChange = {
                model = model.copy(password = it)
            },
            onLogin = {},
            onRegister = {}
        )
    }
}
