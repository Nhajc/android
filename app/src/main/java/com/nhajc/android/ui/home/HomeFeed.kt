package com.nhajc.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nhajc.android.R
import com.nhajc.android.data.model.LibraryTab
import com.nhajc.android.ui.common.MediaGridItem
import com.nhajc.android.ui.common.MediaListItem
import com.nhajc.android.ui.common.message
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeFeed(
    uiState: HomeUiState,
    onOpenSearch: () -> Unit,
    onOpenLibrary: (LibraryTab) -> Unit,
    onOpenArtist: (Uuid) -> Unit,
    onOpenRelease: (Uuid) -> Unit,
    onOpenPlaylist: (Uuid) -> Unit,
    onRefresh: () -> Unit,
    onPlayFavorite: (Uuid) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("home-feed"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "welcome") {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = uiState.user?.username?.let { stringResource(R.string.home_greeting, it) }
                            ?: stringResource(R.string.home_welcome),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Headphones, null, Modifier.size(36.dp))
                            Text(stringResource(R.string.home_hero_title), style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.home_hero_description), style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onOpenSearch, modifier = Modifier.testTag("home-open-search")) {
                                    Icon(Icons.Outlined.Search, null, Modifier.size(18.dp))
                                    Text(stringResource(R.string.search), Modifier.padding(start = 6.dp))
                                }
                                OutlinedButton(onClick = { onOpenLibrary(LibraryTab.RELEASES) }) {
                                    Text(stringResource(R.string.library))
                                }
                            }
                        }
                    }
                }
            }
            if (uiState.error != null) item(key = "error") {
                Card(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().testTag("home-error"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.CloudOff, null)
                        Text(stringResource(uiState.error.message()))
                        if (!uiState.content.isEmpty) Text(stringResource(R.string.home_cached_content),
                            style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                            Text(stringResource(R.string.search_retry))
                        }
                    }
                }
            }
            if (uiState.isRefreshing && uiState.content.isEmpty) item(key = "loading") {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.home_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (uiState.content.isEmpty && !uiState.isRefreshing && uiState.error == null) item(key = "empty") {
                Column(Modifier.padding(horizontal = 24.dp).testTag("home-empty"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.home_empty_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (uiState.content.releases.isNotEmpty()) item(key = "releases") {
                HomeShelf(R.string.home_saved_releases, LibraryTab.RELEASES, uiState.content.releases,
                    key = { it.id.toString() }, onOpenLibrary = onOpenLibrary) { release ->
                    MediaGridItem(
                        title = release.title, subtitle = release.artists.joinToString(", ") { it.name },
                        imageUrl = release.coverUrl, fallbackIcon = Icons.Outlined.Album,
                        showText = true, onClick = { onOpenRelease(release.id) }, modifier = Modifier.width(152.dp)
                    )
                }
            }
            if (uiState.content.playlists.isNotEmpty()) item(key = "playlists") {
                HomeShelf(R.string.home_playlists, LibraryTab.PLAYLISTS, uiState.content.playlists,
                    key = { it.id.toString() }, onOpenLibrary = onOpenLibrary) { playlist ->
                    MediaGridItem(
                        title = playlist.title, subtitle = playlist.owner.username,
                        imageUrl = playlist.coverUrl, fallbackIcon = Icons.AutoMirrored.Outlined.QueueMusic,
                        showText = true, onClick = { onOpenPlaylist(playlist.id) }, modifier = Modifier.width(152.dp)
                    )
                }
            }
            if (uiState.content.artists.isNotEmpty()) item(key = "artists") {
                HomeShelf(R.string.home_followed_artists, LibraryTab.ARTISTS, uiState.content.artists,
                    key = { it.id.toString() }, onOpenLibrary = onOpenLibrary) { artist ->
                    MediaGridItem(
                        title = artist.name, subtitle = null, imageUrl = artist.avatarUrl,
                        fallbackIcon = Icons.Outlined.Person, showText = true, isRound = true,
                        onClick = { onOpenArtist(artist.id) }, modifier = Modifier.width(128.dp)
                    )
                }
            }
            if (uiState.content.favorites.isNotEmpty()) {
                item(key = "favorites-title") {
                    HomeSectionTitle(R.string.home_favorites) { onOpenLibrary(LibraryTab.FAVORITES) }
                }
                items(uiState.content.favorites, key = { "favorite:${it.id}" }) { song ->
                    MediaListItem(
                        title = song.title,
                        subtitle = song.artists.joinToString(", ") { it.name },
                        imageUrl = song.coverUrl, fallbackIcon = Icons.Outlined.MusicNote,
                        onClick = { onPlayFavorite(song.id) },
                        modifier = Modifier.testTag("home-play:${song.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSectionTitle(@StringRes title: Int, onSeeAll: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onSeeAll) { Text(stringResource(R.string.home_see_all)) }
    }
}

@Composable
private fun <T> HomeShelf(
    @StringRes title: Int,
    tab: LibraryTab,
    items: List<T>,
    key: (T) -> String,
    onOpenLibrary: (LibraryTab) -> Unit,
    card: @Composable (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeSectionTitle(title) { onOpenLibrary(tab) }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(items, key = key) { card(it) }
        }
    }
}
