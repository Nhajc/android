package com.nhajc.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nhajc.android.data.model.ArtistSummary
import kotlin.uuid.Uuid

private const val MAX_AVATARS = 3
private const val AVATAR_OFFSET = 16

@Composable
fun ArtistLine(artists: List<ArtistSummary>, onOpenArtist: (Uuid) -> Unit) {
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
