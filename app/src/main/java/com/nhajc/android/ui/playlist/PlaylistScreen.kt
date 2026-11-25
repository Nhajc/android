package com.nhajc.android.ui.playlist

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.R
import com.nhajc.android.data.model.ArtistSummary
import com.nhajc.android.data.model.Playlist
import com.nhajc.android.data.model.Song
import com.nhajc.android.data.model.UserSummary
import com.nhajc.android.ui.common.CollectionHeader
import com.nhajc.android.ui.common.CoverImage
import com.nhajc.android.ui.common.DownloadMenuItem
import com.nhajc.android.ui.common.SaveButton
import com.nhajc.android.ui.common.SongRow
import com.nhajc.android.ui.common.message
import com.nhajc.android.ui.theme.NhajcTheme
import kotlin.uuid.Uuid

@Composable
fun PlaylistScreen(
    playlistId: Uuid,
    onBack: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel<PlaylistViewModel, PlaylistViewModel.Factory>(
        creationCallback = { factory -> factory.create(playlistId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    PlaylistScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onPlayClick = viewModel::onPlayClick,
        onShuffleClick = viewModel::onShuffleClick,
        onSaveClick = viewModel::onSaveClick,
        onSongClick = viewModel::onSongClick,
        onFavoriteClick = viewModel::onFavoriteClick,
        onSongDownloadClick = viewModel::onSongDownloadClick,
        onDownloadClick = viewModel::onDownloadClick,
        onRemoveDownloadClick = viewModel::onRemoveDownloadClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onErrorShown = viewModel::onErrorShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistScreen(
    uiState: PlaylistUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSongClick: (index: Int) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onSongDownloadClick: (Song) -> Unit,
    onDownloadClick: () -> Unit,
    onRemoveDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playlist = uiState.playlist
    val snackbarHostState = remember { SnackbarHostState() }
    var addingSong by remember { mutableStateOf<Song?>(null) }

    val errorMessage = uiState.error?.let { stringResource(it.message()) }

    LaunchedEffect(errorMessage, playlist == null) {
        if (errorMessage != null && playlist != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    val hasMenu = uiState.isOwned ||
                        uiState.canDownload ||
                        uiState.canRemoveDownload

                    if (playlist != null && hasMenu) {
                        PlaylistMenuButton(
                            isOwned = uiState.isOwned,
                            canDownload = uiState.canDownload,
                            canRemoveDownload = uiState.canRemoveDownload,
                            onDownloadClick = onDownloadClick,
                            onRemoveDownloadClick = onRemoveDownloadClick,
                            onDeleteClick = onDeleteClick
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)

        when {
            playlist != null -> PlaylistContent(
                playlist = playlist,
                songs = uiState.songs,
                downloadProgress = uiState.downloadProgress,
                isOwned = uiState.isOwned,
                isUpdatingSave = uiState.isUpdatingSave,
                onPlayClick = onPlayClick,
                onShuffleClick = onShuffleClick,
                onSaveClick = onSaveClick,
                onSongClick = onSongClick,
                onFavoriteClick = onFavoriteClick,
                onAddToPlaylistClick = { song -> addingSong = song },
                onSongDownloadClick = onSongDownloadClick,
                modifier = contentModifier
            )

            errorMessage != null -> Column(
                modifier = contentModifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = errorMessage, textAlign = TextAlign.Center)

                OutlinedButton(onClick = onRetry) {
                    Text(stringResource(R.string.try_again))
                }
            }

            else -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    if (uiState.isDeleteOpen && playlist != null) {
        DeletePlaylistDialog(
            title = playlist.title,
            isDeleting = uiState.isDeleting,
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss
        )
    }

    AddToPlaylistSheet(
        song = addingSong,
        snackbarHostState = snackbarHostState,
        onDismiss = { addingSong = null }
    )
}

@Composable
private fun PlaylistContent(
    playlist: Playlist,
    songs: List<Song>,
    downloadProgress: Map<Uuid, Float>,
    isOwned: Boolean,
    isUpdatingSave: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSongClick: (index: Int) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onSongDownloadClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = pluralStringResource(R.plurals.playlist_subtitle, songs.size, songs.size)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            CollectionHeader(
                title = playlist.title,
                subtitle = subtitle,
                coverUrl = playlist.coverUrl,
                fallbackIcon = Icons.AutoMirrored.Outlined.QueueMusic,
                isPlayEnabled = songs.isNotEmpty(),
                onShuffleClick = onShuffleClick,
                onPlayClick = onPlayClick,
                byline = { OwnerLine(playlist.owner, playlist.isPrivate) },
                actions = {
                    if (!isOwned) {
                        SaveButton(
                            isSaved = playlist.isSaved,
                            enabled = !isUpdatingSave,
                            onClick = onSaveClick
                        )
                    }
                }
            )
        }

        if (songs.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_songs),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        itemsIndexed(songs, key = { _, song -> song.id.toString() }) { index, song ->
            SongRow(
                number = index + 1,
                song = song,
                showCover = true,
                downloadProgress = downloadProgress[song.id],
                onClick = { onSongClick(index) },
                onFavoriteClick = { onFavoriteClick(song) },
                onAddToPlaylistClick = { onAddToPlaylistClick(song) },
                onDownloadClick = { onSongDownloadClick(song) }
            )
        }
    }
}

@Composable
private fun OwnerLine(owner: UserSummary, isPrivate: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            url = owner.avatarUrl,
            fallbackIcon = Icons.Outlined.Person,
            modifier = Modifier.size(24.dp),
            shape = CircleShape
        )

        Text(
            text = owner.username,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isPrivate) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = stringResource(R.string.private_playlist),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistMenuButton(
    isOwned: Boolean,
    canDownload: Boolean,
    canRemoveDownload: Boolean,
    onDownloadClick: () -> Unit,
    onRemoveDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_options)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (canDownload) {
                DownloadMenuItem(
                    isRemove = false,
                    onClick = {
                        expanded = false
                        onDownloadClick()
                    }
                )
            }

            if (canRemoveDownload) {
                DownloadMenuItem(
                    isRemove = true,
                    onClick = {
                        expanded = false
                        onRemoveDownloadClick()
                    }
                )
            }

            if (isOwned) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_playlist)) },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    // TODO: edit mode with #29 (feat/playlist-edit)
                    onClick = { expanded = false }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_playlist)) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun DeletePlaylistDialog(
    title: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_playlist)) },
        text = { Text(stringResource(R.string.delete_playlist_message, title)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview(name = "Playlist Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Playlist Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaylistScreenPreview() {
    val artist = ArtistSummary(Uuid.random(), "Kite Theory", null)

    val playlist = Playlist(
        id = Uuid.random(),
        title = "Late Night Mix",
        coverUrl = null,
        isPrivate = true,
        owner = UserSummary(Uuid.random(), "karas", null),
        isSaved = true
    )

    val songs = listOf("Static Bloom", "Night Relay", "Orange Wire").mapIndexed { index, title ->
        Song(
            id = Uuid.random(),
            title = title,
            trackNumber = index + 1,
            durationSeconds = 200 + index * 17,
            artists = listOf(artist),
            releaseId = Uuid.random(),
            releaseTitle = "Glass Antenna",
            coverUrl = null,
            isFavorite = index == 1,
            fileQuality = null,
            isDownloaded = false
        )
    }

    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        PlaylistScreen(
            uiState = PlaylistUiState(playlist = playlist, songs = songs, isOwned = true),
            onBack = {},
            onRetry = {},
            onPlayClick = {},
            onShuffleClick = {},
            onSaveClick = {},
            onSongClick = {},
            onFavoriteClick = {},
            onSongDownloadClick = {},
            onDownloadClick = {},
            onRemoveDownloadClick = {},
            onDeleteClick = {},
            onDeleteDismiss = {},
            onDeleteConfirm = {},
            onErrorShown = {}
        )
    }
}
