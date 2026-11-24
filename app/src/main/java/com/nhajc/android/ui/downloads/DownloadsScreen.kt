package com.nhajc.android.ui.downloads

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.nhajc.android.data.model.DownloadSort
import com.nhajc.android.data.model.Song
import com.nhajc.android.ui.common.MediaEntry
import com.nhajc.android.ui.common.PagedMediaItems
import com.nhajc.android.ui.common.SearchTopBar
import com.nhajc.android.ui.common.SortButton
import com.nhajc.android.ui.common.ViewStyleButton
import com.nhajc.android.ui.theme.NhajcTheme
import kotlinx.coroutines.flow.flowOf

@Composable
fun DownloadsScreen(onBack: () -> Unit, viewModel: DownloadsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val songs = viewModel.songs.collectAsLazyPagingItems()

    DownloadsScreen(
        uiState = uiState,
        songs = songs,
        onSortChange = viewModel::onSortChange,
        onViewChange = viewModel::onViewChange,
        onSongClick = viewModel::onSongClick,
        onSearchOpen = viewModel::onSearchOpen,
        onSearchClose = viewModel::onSearchClose,
        onQueryChange = viewModel::onQueryChange,
        onBack = onBack
    )
}

@Composable
internal fun DownloadsScreen(
    uiState: DownloadsUiState,
    songs: LazyPagingItems<Song>,
    onSortChange: (DownloadSort) -> Unit,
    onViewChange: () -> Unit,
    onSongClick: (index: Int) -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SearchTopBar(
                title = stringResource(R.string.downloads),
                placeholder = stringResource(R.string.find_in_downloads),
                query = uiState.query,
                isSearching = uiState.isSearching,
                onQueryChange = onQueryChange,
                onSearchOpen = onSearchOpen,
                onSearchClose = onSearchClose,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortButton(
                    options = DownloadSort.entries,
                    selected = uiState.sort,
                    label = { stringResource(it.label()) },
                    onSelect = onSortChange
                )

                ViewStyleButton(view = uiState.view, onClick = onViewChange)
            }

            val emptyText = if (uiState.query.isNotBlank()) {
                stringResource(R.string.no_results)
            } else {
                stringResource(R.string.empty_downloads)
            }

            PagedMediaItems(
                items = songs,
                view = uiState.view,
                emptyText = emptyText,
                key = { it.id.toString() },
                entry = { song ->
                    MediaEntry(
                        title = song.title,
                        subtitle = song.artists.joinToString(", ") { it.name },
                        imageUrl = song.coverUrl,
                        fallbackIcon = Icons.Outlined.MusicNote
                    )
                },
                onItemClick = onSongClick
            )
        }
    }
}

@StringRes
private fun DownloadSort.label(): Int = when (this) {
    DownloadSort.RECENT -> R.string.sort_recent
    DownloadSort.NAME -> R.string.sort_name
    DownloadSort.ARTIST -> R.string.sort_artist
}

@Preview(name = "Downloads Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Downloads Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DownloadsScreenPreview() {
    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        DownloadsScreen(
            uiState = DownloadsUiState(),
            songs = flowOf(PagingData.empty<Song>()).collectAsLazyPagingItems(),
            onSortChange = {},
            onViewChange = {},
            onSongClick = {},
            onSearchOpen = {},
            onSearchClose = {},
            onQueryChange = {},
            onBack = {}
        )
    }
}
