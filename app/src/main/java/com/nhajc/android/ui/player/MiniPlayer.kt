package com.nhajc.android.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.material3.MiniController
import androidx.media3.ui.compose.state.rememberCurrentMediaItemState
import com.nhajc.android.ui.common.CoverImage

@OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(onOpen: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val player by viewModel.player.collectAsStateWithLifecycle()

    val current = player

    if (current == null) {
        return
    }

    val mediaItemState = rememberCurrentMediaItemState(current)

    if (mediaItemState.mediaItem == null) {
        return
    }

    MiniController(
        player = current,
        onClick = onOpen,
        artwork = {
            CoverImage(
                url = mediaItemState.mediaMetadata.artworkUri?.toString(),
                fallbackIcon = Icons.Outlined.Album,
                modifier = Modifier.size(48.dp)
            )
        }
    )
}
