package com.nhajc.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownloadOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nhajc.android.R

@Composable
fun SongMenuButton(
    isFavorite: Boolean,
    isFavoriteEnabled: Boolean,
    hasDownload: Boolean,
    onFavoriteClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onDownloadClick: () -> Unit
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
                onClick = {
                    expanded = false
                    onAddToPlaylistClick()
                }
            )

            DownloadMenuItem(
                isRemove = hasDownload,
                onClick = {
                    expanded = false
                    onDownloadClick()
                }
            )
        }
    }
}

@Composable
fun DownloadMenuItem(isRemove: Boolean, onClick: () -> Unit) {
    if (isRemove) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.remove_download)) },
            leadingIcon = { Icon(Icons.Outlined.FileDownloadOff, contentDescription = null) },
            onClick = onClick
        )
    } else {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.download)) },
            leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
            onClick = onClick
        )
    }
}
