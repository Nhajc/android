package com.nhajc.android.ui.player

import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlaylistState
import com.nhajc.android.R
import com.nhajc.android.ui.common.MediaListItem

internal enum class PlayerTab { UP_NEXT, COMMENTS }

private const val SHEET_HEIGHT_FRACTION = 0.9f

@Composable
internal fun PlayerSheet(player: Player, selectedTab: PlayerTab, onTabSelect: (PlayerTab) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(SHEET_HEIGHT_FRACTION)
    ) {
        PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
            for (tab in PlayerTab.entries) {
                Tab(
                    selected = tab == selectedTab,
                    onClick = { onTabSelect(tab) },
                    text = { Text(stringResource(tab.label())) }
                )
            }
        }

        when (selectedTab) {
            PlayerTab.UP_NEXT -> UpNext(player)
            PlayerTab.COMMENTS -> Comments()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun UpNext(player: Player) {
    val playlistState = rememberPlaylistState(player)

    LazyColumn {
        items(count = playlistState.mediaItemCount) { index ->
            val metadata = playlistState.getMediaItemAt(index).mediaMetadata

            MediaListItem(
                title = metadata.title?.toString().orEmpty(),
                subtitle = metadata.artist?.toString(),
                imageUrl = metadata.artworkUri?.toString(),
                fallbackIcon = Icons.Outlined.MusicNote,
                onClick = { playlistState.seekToMediaItem(index) },
                isHighlighted = index == playlistState.currentMediaItemIndex
            )
        }
    }
}

@Composable
private fun Comments() {
    // TODO: load, post and delete comments (#32–36) in the comments branch
    Text(
        text = stringResource(R.string.comments_coming_soon),
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@StringRes
private fun PlayerTab.label(): Int = when (this) {
    PlayerTab.UP_NEXT -> R.string.up_next
    PlayerTab.COMMENTS -> R.string.comments
}
