package com.nhajc.android.ui.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.material3.buttons.NextButton
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.buttons.PreviousButton
import androidx.media3.ui.compose.material3.buttons.RepeatButton
import androidx.media3.ui.compose.material3.buttons.ShuffleButton
import androidx.media3.ui.compose.material3.indicator.DurationText
import androidx.media3.ui.compose.material3.indicator.PositionText
import androidx.media3.ui.compose.material3.indicator.ProgressSlider
import androidx.media3.ui.compose.state.rememberCurrentMediaItemState
import com.nhajc.android.R
import com.nhajc.android.data.model.ArtistSummary
import com.nhajc.android.ui.common.CoverImage
import com.nhajc.android.ui.common.message
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    onClose: () -> Unit,
    onOpenArtist: (Uuid) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()

    val current = player

    if (current == null) {
        return
    }

    PlayerScreen(
        uiState = uiState,
        player = current,
        onSongChange = viewModel::onSongChange,
        onFavoriteClick = viewModel::onFavoriteClick,
        onErrorShown = viewModel::onErrorShown,
        onClose = onClose,
        onOpenArtist = onOpenArtist
    )
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerScreen(
    uiState: PlayerUiState,
    player: Player,
    onSongChange: (String?) -> Unit,
    onFavoriteClick: () -> Unit,
    onErrorShown: () -> Unit,
    onClose: () -> Unit,
    onOpenArtist: (Uuid) -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaItemState = rememberCurrentMediaItemState(player)
    val mediaId = mediaItemState.mediaItem?.mediaId
    val metadata = mediaItemState.mediaMetadata
    val song = uiState.song

    LaunchedEffect(mediaId) {
        onSongChange(mediaId)
    }

    val scaffoldState = rememberBottomSheetScaffoldState()
    val sheetState = scaffoldState.bottomSheetState
    val snackbarHostState = scaffoldState.snackbarHostState
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableStateOf(PlayerTab.UP_NEXT) }

    BackHandler(enabled = sheetState.currentValue == SheetValue.Expanded) {
        scope.launch { sheetState.partialExpand() }
    }

    val errorMessage = uiState.error?.let { stringResource(it.message()) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }

    val title = song?.title ?: metadata.title?.toString().orEmpty()
    val coverUrl = song?.coverUrl ?: metadata.artworkUri?.toString()

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = SHEET_PEEK_HEIGHT.dp + navigationBarHeight,
        sheetDragHandle = null,
        sheetContent = {
            PlayerSheet(
                player = player,
                selectedTab = selectedTab,
                onTabSelect = { tab ->
                    selectedTab = tab
                    scope.launch { sheetState.expand() }
                }
            )
        },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Outlined.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.close_player)
                        )
                    }
                },
                actions = {
                    SongMenuButton(
                        isFavorite = song?.isFavorite == true,
                        isFavoriteEnabled = song != null && !uiState.isUpdatingFavorite,
                        onFavoriteClick = onFavoriteClick
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CoverImage(
                url = coverUrl,
                fallbackIcon = Icons.Outlined.Album,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (song != null) {
                        ArtistLine(song.artists, onOpenArtist)
                    } else {
                        Text(
                            text = metadata.artist?.toString().orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FavoriteButton(
                    isFavorite = song?.isFavorite == true,
                    enabled = song != null && !uiState.isUpdatingFavorite,
                    onClick = onFavoriteClick
                )
            }

            ProgressSlider(player = player, modifier = Modifier.fillMaxWidth())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PositionText(player = player)
                DurationText(player = player)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShuffleButton(player = player)
                PreviousButton(player = player)
                PlayPauseButton(player = player, modifier = Modifier.size(72.dp))
                NextButton(player = player)
                RepeatButton(player = player)
            }
        }
    }
}

@Composable
private fun ArtistLine(artists: List<ArtistSummary>, onOpenArtist: (Uuid) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.clickable {
            if (artists.size == 1) {
                onOpenArtist(artists.first().id)
            } else {
                expanded = true
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            for ((index, artist) in artists.take(MAX_AVATARS).withIndex()) {
                CoverImage(
                    url = artist.avatarUrl,
                    fallbackIcon = Icons.Outlined.Person,
                    modifier = Modifier
                        .padding(start = (index * AVATAR_OFFSET).dp)
                        .size(24.dp),
                    shape = CircleShape
                )
            }
        }

        Text(
            text = artists.joinToString(", ") { it.name },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (artist in artists) {
                DropdownMenuItem(
                    text = { Text(artist.name) },
                    onClick = {
                        expanded = false
                        onOpenArtist(artist.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        if (isFavorite) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.remove_from_favorites),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(R.string.add_to_favorites)
            )
        }
    }
}

@Composable
private fun SongMenuButton(
    isFavorite: Boolean,
    isFavoriteEnabled: Boolean,
    onFavoriteClick: () -> Unit
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
            val favoriteText = if (isFavorite) {
                R.string.remove_from_favorites
            } else {
                R.string.add_to_favorites
            }

            DropdownMenuItem(
                text = { Text(stringResource(favoriteText)) },
                leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) },
                enabled = isFavoriteEnabled,
                onClick = {
                    expanded = false
                    onFavoriteClick()
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_to_playlist)) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = null)
                },
                // TODO: pick one of the user's playlists, then add the song with #31
                onClick = { expanded = false }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.download)) },
                leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                // TODO: download the song (Media3 DownloadManager, downloads branch)
                onClick = { expanded = false }
            )
        }
    }
}

private const val SHEET_PEEK_HEIGHT = 48
private const val MAX_AVATARS = 3
private const val AVATAR_OFFSET = 16
