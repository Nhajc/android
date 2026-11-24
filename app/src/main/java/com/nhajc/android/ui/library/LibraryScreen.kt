package com.nhajc.android.ui.library

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.nhajc.android.R
import com.nhajc.android.data.model.ArtistSort
import com.nhajc.android.data.model.ArtistSummary
import com.nhajc.android.data.model.LibrarySettings
import com.nhajc.android.data.model.LibrarySort
import com.nhajc.android.data.model.LibraryTab
import com.nhajc.android.data.model.Playlist
import com.nhajc.android.data.model.Release
import com.nhajc.android.data.model.ReleaseType
import com.nhajc.android.data.model.Song
import com.nhajc.android.ui.common.MediaEntry
import com.nhajc.android.ui.common.PagedMediaItems
import com.nhajc.android.ui.common.SearchTopBar
import com.nhajc.android.ui.common.SortButton
import com.nhajc.android.ui.common.UserAvatarButton
import com.nhajc.android.ui.common.ViewStyleButton
import com.nhajc.android.ui.common.message
import com.nhajc.android.ui.theme.NhajcTheme
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate

@Composable
fun LibraryScreen(
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val releases = viewModel.releases.collectAsLazyPagingItems()
    val playlists = viewModel.playlists.collectAsLazyPagingItems()
    val artists = viewModel.artists.collectAsLazyPagingItems()
    val favorites = viewModel.favorites.collectAsLazyPagingItems()

    LibraryScreen(
        uiState = uiState,
        releases = releases,
        playlists = playlists,
        artists = artists,
        favorites = favorites,
        onTabSelect = viewModel::onTabSelect,
        onSortChange = viewModel::onSortChange,
        onArtistSortChange = viewModel::onArtistSortChange,
        onViewChange = viewModel::onViewChange,
        onSearchOpen = viewModel::onSearchOpen,
        onSearchClose = viewModel::onSearchClose,
        onQueryChange = viewModel::onQueryChange,
        onRefresh = viewModel::onRefresh,
        onErrorShown = viewModel::onErrorShown,
        onNewPlaylistOpen = viewModel::onNewPlaylistOpen,
        onNewPlaylistDismiss = viewModel::onNewPlaylistDismiss,
        onCreatePlaylist = viewModel::onCreatePlaylist,
        onFavoriteClick = viewModel::onFavoriteClick,
        onOpenSettings = onOpenSettings,
        onOpenDownloads = onOpenDownloads
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    releases: LazyPagingItems<Release>,
    playlists: LazyPagingItems<Playlist>,
    artists: LazyPagingItems<ArtistSummary>,
    favorites: LazyPagingItems<Song>,
    onTabSelect: (LibraryTab) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onArtistSortChange: (ArtistSort) -> Unit,
    onViewChange: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onErrorShown: () -> Unit,
    onNewPlaylistOpen: () -> Unit,
    onNewPlaylistDismiss: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onFavoriteClick: (index: Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.message()) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchTopBar(
                title = stringResource(R.string.library),
                placeholder = stringResource(R.string.find_in_library),
                query = uiState.query,
                isSearching = uiState.isSearching,
                onQueryChange = onQueryChange,
                onSearchOpen = onSearchOpen,
                onSearchClose = onSearchClose,
                navigationIcon = {
                    UserAvatarButton(
                        avatarUrl = uiState.user?.avatarUrl,
                        onClick = onOpenSettings
                    )
                },
                actions = {
                    IconButton(onClick = onOpenDownloads) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = stringResource(R.string.downloads)
                        )
                    }

                    IconButton(onClick = onNewPlaylistOpen) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.new_playlist)
                        )
                    }
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
        ) {
            LibraryTabs(selected = settings.tab, onTabSelect = onTabSelect)

            LibraryToolbar(
                settings = settings,
                onSortChange = onSortChange,
                onArtistSortChange = onArtistSortChange,
                onViewChange = onViewChange
            )

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LibraryContent(
                    uiState = uiState,
                    releases = releases,
                    playlists = playlists,
                    artists = artists,
                    favorites = favorites,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }

    if (uiState.isNewPlaylistOpen) {
        NewPlaylistDialog(
            isCreating = uiState.isCreatingPlaylist,
            error = uiState.newPlaylistError,
            onCreate = onCreatePlaylist,
            onDismiss = onNewPlaylistDismiss
        )
    }
}

@Composable
private fun LibraryTabs(selected: LibraryTab, onTabSelect: (LibraryTab) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        for (tab in LibraryTab.entries) {
            FilterChip(
                selected = tab == selected,
                onClick = { onTabSelect(tab) },
                label = { Text(stringResource(tab.label())) }
            )
        }
    }
}

@Composable
private fun LibraryToolbar(
    settings: LibrarySettings,
    onSortChange: (LibrarySort) -> Unit,
    onArtistSortChange: (ArtistSort) -> Unit,
    onViewChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (settings.tab == LibraryTab.ARTISTS) {
            SortButton(
                options = ArtistSort.entries,
                selected = settings.artistSort,
                label = { stringResource(it.label()) },
                onSelect = onArtistSortChange
            )
        } else {
            val selected = when (settings.tab) {
                LibraryTab.RELEASES -> settings.releaseSort
                LibraryTab.PLAYLISTS -> settings.playlistSort
                else -> settings.favoriteSort
            }

            SortButton(
                options = LibrarySort.entries,
                selected = selected,
                label = {
                    stringResource(it.label(isPlaylist = settings.tab == LibraryTab.PLAYLISTS))
                },
                onSelect = onSortChange
            )
        }

        ViewStyleButton(view = settings.view, onClick = onViewChange)
    }
}

@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    releases: LazyPagingItems<Release>,
    playlists: LazyPagingItems<Playlist>,
    artists: LazyPagingItems<ArtistSummary>,
    favorites: LazyPagingItems<Song>,
    onFavoriteClick: (index: Int) -> Unit
) {
    val view = uiState.settings.view
    val isFiltered = uiState.query.isNotBlank()

    when (uiState.settings.tab) {
        LibraryTab.RELEASES -> PagedMediaItems(
            items = releases,
            view = view,
            emptyText = emptyText(isFiltered, R.string.empty_releases),
            key = { it.id.toString() },
            entry = { release -> release.entry() },
            // TODO: open the release screen once it exists
            onItemClick = {}
        )

        LibraryTab.PLAYLISTS -> PagedMediaItems(
            items = playlists,
            view = view,
            emptyText = emptyText(isFiltered, R.string.empty_playlists),
            key = { it.id.toString() },
            entry = { playlist -> playlist.entry() },
            // TODO: open the playlist screen once it exists
            onItemClick = {}
        )

        LibraryTab.ARTISTS -> PagedMediaItems(
            items = artists,
            view = view,
            emptyText = emptyText(isFiltered, R.string.empty_artists),
            key = { it.id.toString() },
            entry = { artist -> artist.entry() },
            // TODO: open the artist screen once it exists
            onItemClick = {}
        )

        LibraryTab.FAVORITES -> PagedMediaItems(
            items = favorites,
            view = view,
            emptyText = emptyText(isFiltered, R.string.empty_favorites),
            key = { it.id.toString() },
            entry = { song -> song.entry() },
            onItemClick = onFavoriteClick
        )
    }
}

@Composable
private fun Release.entry(): MediaEntry {
    val type = stringResource(releaseType.label())
    val artistNames = artists.joinToString(", ") { it.name }

    val subtitle = if (artistNames.isEmpty()) {
        type
    } else {
        "$type · $artistNames"
    }

    return MediaEntry(title, subtitle, coverUrl, Icons.Outlined.Album)
}

private fun Playlist.entry() =
    MediaEntry(title, owner.username, coverUrl, Icons.AutoMirrored.Outlined.QueueMusic)

private fun ArtistSummary.entry() =
    MediaEntry(name, null, avatarUrl, Icons.Outlined.Person, isRound = true)

private fun Song.entry() =
    MediaEntry(title, artists.joinToString(", ") { it.name }, coverUrl, Icons.Outlined.MusicNote)

@Composable
private fun emptyText(isFiltered: Boolean, @StringRes empty: Int): String = if (isFiltered) {
    stringResource(R.string.no_results)
} else {
    stringResource(empty)
}

@StringRes
private fun LibraryTab.label(): Int = when (this) {
    LibraryTab.RELEASES -> R.string.releases
    LibraryTab.PLAYLISTS -> R.string.playlists
    LibraryTab.ARTISTS -> R.string.artists
    LibraryTab.FAVORITES -> R.string.favorites
}

@StringRes
private fun LibrarySort.label(isPlaylist: Boolean): Int = when (this) {
    LibrarySort.RECENT -> R.string.sort_recent
    LibrarySort.NAME -> R.string.sort_name
    LibrarySort.ARTIST -> if (isPlaylist) R.string.sort_owner else R.string.sort_artist
    LibrarySort.DOWNLOADED -> R.string.sort_downloaded
}

@StringRes
private fun ArtistSort.label(): Int = when (this) {
    ArtistSort.RECENT -> R.string.sort_recent
    ArtistSort.NAME -> R.string.sort_name
}

@StringRes
private fun ReleaseType.label(): Int = when (this) {
    ReleaseType.ALBUM -> R.string.release_type_album
    ReleaseType.SINGLE -> R.string.release_type_single
    ReleaseType.EP -> R.string.release_type_ep
    ReleaseType.COMPILATION -> R.string.release_type_compilation
    ReleaseType.UNKNOWN -> R.string.release_type_unknown
}

@Preview(name = "Library Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Library Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LibraryScreenPreview() {
    val sampleReleases = listOf(
        Release(
            id = Uuid.random(),
            title = "First Light",
            releaseType = ReleaseType.ALBUM,
            releaseDate = LocalDate(2026, 1, 1),
            coverUrl = null,
            artists = listOf(ArtistSummary(Uuid.random(), "Artist A", null))
        ),
        Release(
            id = Uuid.random(),
            title = "Night Drive",
            releaseType = ReleaseType.SINGLE,
            releaseDate = LocalDate(2026, 2, 1),
            coverUrl = null,
            artists = emptyList()
        )
    )

    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        LibraryScreen(
            uiState = LibraryUiState(),
            releases = flowOf(PagingData.from(sampleReleases)).collectAsLazyPagingItems(),
            playlists = flowOf(PagingData.empty<Playlist>()).collectAsLazyPagingItems(),
            artists = flowOf(PagingData.empty<ArtistSummary>()).collectAsLazyPagingItems(),
            favorites = flowOf(PagingData.empty<Song>()).collectAsLazyPagingItems(),
            onTabSelect = {},
            onSortChange = {},
            onArtistSortChange = {},
            onViewChange = {},
            onSearchOpen = {},
            onSearchClose = {},
            onQueryChange = {},
            onRefresh = {},
            onErrorShown = {},
            onNewPlaylistOpen = {},
            onNewPlaylistDismiss = {},
            onCreatePlaylist = {},
            onFavoriteClick = {},
            onOpenSettings = {},
            onOpenDownloads = {}
        )
    }
}
