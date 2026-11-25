package com.nhajc.android.ui.artist

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nhajc.android.R
import com.nhajc.android.data.model.Artist
import com.nhajc.android.data.model.ArtistError
import com.nhajc.android.data.model.ArtistRelease
import com.nhajc.android.data.model.ReleaseType
import com.nhajc.android.ui.common.label
import com.nhajc.android.ui.theme.NhajcTheme
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

@Composable
fun ArtistScreen(
    artistId: Uuid,
    onBack: () -> Unit,
    onOpenRelease: (Uuid) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(artistId, viewModel) {
        viewModel.loadArtist(artistId)
    }

    ArtistScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onToggleFollow = viewModel::toggleFollow,
        onOpenRelease = onOpenRelease
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArtistScreen(
    uiState: ArtistUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenRelease: (Uuid) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.artist_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)

        val artist = uiState.artist

        when {
            uiState.isLoading -> {
                Column(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.artist_loading))
                }
            }

            artist == null -> {
                Column(
                    modifier = contentModifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(artistErrorMessage(uiState.error)),
                        textAlign = TextAlign.Center
                    )

                    OutlinedButton(onClick = onRetry) {
                        Text(stringResource(R.string.artist_retry))
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ArtistHeader(artist, uiState.isUpdatingFollow, onToggleFollow)
                    }

                    if (uiState.error != null) {
                        item {
                            Text(
                                text = stringResource(artistErrorMessage(uiState.error)),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.artist_releases),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (artist.releases.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.artist_no_releases),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(artist.releases, key = { it.id.toString() }) { release ->
                        ArtistReleaseItem(release, onClick = { onOpenRelease(release.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(artist: Artist, isUpdatingFollow: Boolean, onToggleFollow: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(64.dp))

            if (!artist.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artist.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Text(
            text = artist.name,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        OutlinedButton(onClick = onToggleFollow, enabled = !isUpdatingFollow) {
            if (isUpdatingFollow) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (artist.isFollowed) Icons.Outlined.Check else Icons.Outlined.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                stringResource(
                    if (artist.isFollowed) R.string.artist_following else R.string.artist_follow
                )
            )
        }
    }
}

@Composable
private fun ArtistReleaseItem(release: ArtistRelease, onClick: () -> Unit) {
    val songCount = pluralStringResource(
        R.plurals.artist_song_count,
        release.songCount,
        release.songCount
    )

    ListItem(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        headlineContent = {
            Text(release.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = stringResource(
                    R.string.artist_release_metadata,
                    stringResource(release.releaseType.label()),
                    release.releaseDate.year,
                    songCount
                )
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Album, contentDescription = null)

                if (!release.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = release.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    )
}

@StringRes
private fun artistErrorMessage(error: ArtistError?): Int {
    return when (error) {
        ArtistError.NETWORK -> R.string.artist_error_network
        ArtistError.SESSION_ENDED -> R.string.error_session_ended
        ArtistError.NOT_FOUND -> R.string.artist_error_not_found
        ArtistError.FORBIDDEN -> R.string.artist_error_forbidden
        ArtistError.INVALID_RESPONSE -> R.string.artist_error_response
        ArtistError.SERVER, null -> R.string.error_server
    }
}

@Preview(name = "Artist Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Artist Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ArtistScreenPreview() {
    val artist = Artist(
        id = Uuid.parse("00000000-0000-0000-0000-000000000001"),
        name = "Kite Theory",
        avatarUrl = null,
        isFollowed = false,
        releases = listOf(
            ArtistRelease(
                id = Uuid.parse("00000000-0000-0000-0000-000000000002"),
                title = "Glass Antenna",
                coverUrl = null,
                releaseType = ReleaseType.EP,
                releaseDate = LocalDate(2025, 6, 20),
                songCount = 5
            ),
            ArtistRelease(
                id = Uuid.parse("00000000-0000-0000-0000-000000000003"),
                title = "Orange Static",
                coverUrl = null,
                releaseType = ReleaseType.SINGLE,
                releaseDate = LocalDate(2024, 9, 12),
                songCount = 1
            )
        )
    )

    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        ArtistScreen(
            uiState = ArtistUiState(artist = artist, isLoading = false),
            onBack = {},
            onRetry = {},
            onToggleFollow = {},
            onOpenRelease = {}
        )
    }
}
