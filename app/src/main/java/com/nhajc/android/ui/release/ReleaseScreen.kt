package com.nhajc.android.ui.release

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.R
import com.nhajc.android.data.model.ArtistSummary
import com.nhajc.android.data.model.Release
import com.nhajc.android.data.model.ReleaseType
import com.nhajc.android.data.model.Song
import com.nhajc.android.ui.common.ArtistLine
import com.nhajc.android.ui.common.CollectionHeader
import com.nhajc.android.ui.common.SaveButton
import com.nhajc.android.ui.common.SongRow
import com.nhajc.android.ui.common.label
import com.nhajc.android.ui.common.message
import com.nhajc.android.ui.theme.NhajcTheme
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

@Composable
fun ReleaseScreen(
    releaseId: Uuid,
    onBack: () -> Unit,
    onOpenArtist: (Uuid) -> Unit,
    viewModel: ReleaseViewModel = hiltViewModel<ReleaseViewModel, ReleaseViewModel.Factory>(
        creationCallback = { factory -> factory.create(releaseId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReleaseScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenArtist = onOpenArtist,
        onRetry = viewModel::refresh,
        onPlayClick = viewModel::onPlayClick,
        onShuffleClick = viewModel::onShuffleClick,
        onSaveClick = viewModel::onSaveClick,
        onSongClick = viewModel::onSongClick,
        onFavoriteClick = viewModel::onFavoriteClick,
        onErrorShown = viewModel::onErrorShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReleaseScreen(
    uiState: ReleaseUiState,
    onBack: () -> Unit,
    onOpenArtist: (Uuid) -> Unit,
    onRetry: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSongClick: (index: Int) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val release = uiState.release
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = uiState.error?.let { stringResource(it.message()) }

    LaunchedEffect(errorMessage, release == null) {
        if (errorMessage != null && release != null) {
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
                    if (release != null) {
                        ReleaseMenuButton()
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
            release != null -> ReleaseContent(
                release = release,
                songs = uiState.songs,
                isUpdatingSave = uiState.isUpdatingSave,
                onOpenArtist = onOpenArtist,
                onPlayClick = onPlayClick,
                onShuffleClick = onShuffleClick,
                onSaveClick = onSaveClick,
                onSongClick = onSongClick,
                onFavoriteClick = onFavoriteClick,
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
}

@Composable
private fun ReleaseContent(
    release: Release,
    songs: List<Song>,
    isUpdatingSave: Boolean,
    onOpenArtist: (Uuid) -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSongClick: (index: Int) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = stringResource(
        R.string.release_subtitle,
        stringResource(release.releaseType.label()),
        release.releaseDate.year
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            CollectionHeader(
                title = release.title,
                subtitle = subtitle,
                coverUrl = release.coverUrl,
                fallbackIcon = Icons.Outlined.Album,
                isPlayEnabled = songs.isNotEmpty(),
                onShuffleClick = onShuffleClick,
                onPlayClick = onPlayClick,
                byline = { ArtistLine(release.artists, onOpenArtist) },
                actions = {
                    SaveButton(
                        isSaved = release.isSaved,
                        enabled = !isUpdatingSave,
                        onClick = onSaveClick
                    )
                }
            )
        }

        if (songs.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.release_no_songs),
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
                number = song.trackNumber,
                song = song,
                showCover = false,
                onClick = { onSongClick(index) },
                onFavoriteClick = { onFavoriteClick(song) }
            )
        }
    }
}

@Composable
private fun ReleaseMenuButton() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_options)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.download)) },
                leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                // TODO: download the release (Media3 DownloadManager, downloads branch)
                onClick = { expanded = false }
            )
        }
    }
}

@Preview(name = "Release Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Release Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReleaseScreenPreview() {
    val artist = ArtistSummary(Uuid.random(), "Kite Theory", null)

    val release = Release(
        id = Uuid.random(),
        title = "Glass Antenna",
        releaseType = ReleaseType.ALBUM,
        releaseDate = LocalDate(2025, 6, 20),
        coverUrl = null,
        artists = listOf(artist),
        isSaved = true
    )

    val songs = listOf("Static Bloom", "Night Relay", "Orange Wire").mapIndexed { index, title ->
        Song(
            id = Uuid.random(),
            title = title,
            trackNumber = index + 1,
            durationSeconds = 200 + index * 17,
            artists = listOf(artist),
            releaseId = release.id,
            releaseTitle = release.title,
            coverUrl = null,
            isFavorite = index == 1
        )
    }

    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        ReleaseScreen(
            uiState = ReleaseUiState(release = release, songs = songs),
            onBack = {},
            onOpenArtist = {},
            onRetry = {},
            onPlayClick = {},
            onShuffleClick = {},
            onSaveClick = {},
            onSongClick = {},
            onFavoriteClick = {},
            onErrorShown = {}
        )
    }
}
