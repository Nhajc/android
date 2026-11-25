package com.nhajc.android.ui.library

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nhajc.android.R
import com.nhajc.android.data.model.AppError
import com.nhajc.android.ui.common.message

private const val MAX_TITLE_LENGTH = 255

@Composable
internal fun NewPlaylistDialog(
    isCreating: Boolean,
    error: AppError?,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_playlist)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { value ->
                    if (value.length <= MAX_TITLE_LENGTH) {
                        title = value
                    }
                },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                isError = error != null,
                supportingText = {
                    if (error != null) {
                        Text(stringResource(error.message()))
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title.trim()) },
                enabled = title.isNotBlank() && !isCreating
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
