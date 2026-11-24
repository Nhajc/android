package com.nhajc.android.ui.playlist

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.R
import com.nhajc.android.data.model.Song
import com.nhajc.android.ui.common.MediaListItem
import com.nhajc.android.ui.common.message
import com.nhajc.android.ui.library.NewPlaylistDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(song: Song?, snackbarHostState: SnackbarHostState, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()

    if (song == null) {
        return
    }

    val viewModel: AddToPlaylistViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val addedMessage = uiState.addedTo?.let { stringResource(R.string.added_to_playlist, it) }

    LaunchedEffect(addedMessage) {
        if (addedMessage != null) {
            scope.launch { snackbarHostState.showSnackbar(addedMessage) }
            viewModel.onAddedShown()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.onDismiss()
            onDismiss()
        }
    ) {
        Text(
            text = stringResource(R.string.add_to_playlist),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium
        )

        val error = uiState.error

        if (error != null && !uiState.isNewPlaylistOpen) {
            Text(
                text = stringResource(error.message()),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.error
            )
        }

        LazyColumn {
            item {
                MediaListItem(
                    title = stringResource(R.string.new_playlist),
                    subtitle = null,
                    imageUrl = null,
                    fallbackIcon = Icons.Outlined.Add,
                    onClick = viewModel::onNewPlaylistOpen
                )
            }

            items(uiState.playlists, key = { it.id.toString() }) { playlist ->
                MediaListItem(
                    title = playlist.title,
                    subtitle = null,
                    imageUrl = playlist.coverUrl,
                    fallbackIcon = Icons.AutoMirrored.Outlined.QueueMusic,
                    onClick = { viewModel.onAdd(playlist, song.id) }
                )
            }
        }
    }

    if (uiState.isNewPlaylistOpen) {
        NewPlaylistDialog(
            isCreating = uiState.isWorking,
            error = uiState.error,
            onCreate = { title -> viewModel.onCreate(title, song.id) },
            onDismiss = viewModel::onNewPlaylistDismiss
        )
    }
}
