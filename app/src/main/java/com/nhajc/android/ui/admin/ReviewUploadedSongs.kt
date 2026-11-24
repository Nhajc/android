package com.nhajc.android.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nhajc.android.R
import com.nhajc.android.data.model.*

internal fun LazyListScope.reviewUploadsContent(state: AdminUiState, onEvent: (AdminEvent) -> Unit, drag: SongDragState) {
    val uploaded = state.uploadQueue.filter { it.uploadedId != null }.associateBy { requireNotNull(it.uploadedId) }
    val existing = state.release?.songs.orEmpty().associateBy { it.id }
    val ordered = state.reviewOrder.mapIndexedNotNull { index, id ->
        uploaded[id]?.let { AdminSong(it.title, index + 1, it.durationSeconds ?: 0, id) } ?: existing[id]
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.release?.release?.title.orEmpty(), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.admin_review_hint), style = MaterialTheme.typography.bodyMedium)
            Button(enabled = !state.busy && drag.draggedId == null, onClick = { onEvent(AdminEvent.SaveUploads) }, modifier = Modifier.fillMaxWidth()) {
                if (state.busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                Text(stringResource(if (state.busy) R.string.admin_saving else R.string.admin_save))
            }
        }
    }
    state.uploadQueue.firstOrNull { it.id == state.editingUpload }?.let { editing ->
        item(key = "upload-editor") { UploadDetails(editing, state, onEvent) }
    }
    itemsIndexed(ordered, key = { _, song -> "song:${song.id}" }) { index, song ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReorderSongRow(song, index + 1, ordered.getOrNull(index - 1)?.id, ordered.getOrNull(index + 1)?.id,
                drag, !state.busy) { id, target -> onEvent(AdminEvent.MoveSong(id, target)) }
            val item = uploaded[song.id]
            if (item != null) {
                TextButton(enabled = !state.busy, modifier = Modifier.testTag("edit-upload:${item.id}"),
                    onClick = { onEvent(AdminEvent.EditUpload(if (state.editingUpload == item.id) null else item.id)) }) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (state.editingUpload == item.id) R.string.admin_hide_details else R.string.admin_edit_details))
                }
            } else Text(stringResource(R.string.admin_existing_song), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun UploadDetails(item: UploadQueueItem, state: AdminUiState, onEvent: (AdminEvent) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(item.title, { onEvent(AdminEvent.QueueTitle(item.id, it)) }, enabled = !state.busy,
                label = { Text(stringResource(R.string.admin_title)) }, singleLine = true,
                isError = AdminValidation.name(item.title) != null, modifier = Modifier.fillMaxWidth().testTag("upload-title:${item.id}"))
            OutlinedTextField(item.description, { onEvent(AdminEvent.QueueDescription(item.id, it)) }, enabled = !state.busy,
                label = { Text(stringResource(R.string.admin_song_description)) }, minLines = 2, maxLines = 5,
                isError = item.description.length > 4000, supportingText = { Text(stringResource(R.string.admin_character_limit, 4000)) },
                modifier = Modifier.fillMaxWidth().testTag("upload-description:${item.id}"))
            Text(stringResource(R.string.admin_file_details, item.file.name,
                (item.durationSeconds ?: 0) / 60, (item.durationSeconds ?: 0) % 60,
                item.file.size?.div(1024)?.toString() ?: "—"), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.admin_artist_credits), style = MaterialTheme.typography.titleSmall)
            item.artists.forEachIndexed { index, artist ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}. ${artist.name}", Modifier.weight(1f))
                    IconButton(enabled = !state.busy && index > 0, onClick = { onEvent(AdminEvent.MoveSongArtist(artist.id, -1)) }) { Icon(Icons.Default.ArrowUpward, stringResource(R.string.admin_move_up)) }
                    IconButton(enabled = !state.busy && index < item.artists.lastIndex, onClick = { onEvent(AdminEvent.MoveSongArtist(artist.id, 1)) }) { Icon(Icons.Default.ArrowDownward, stringResource(R.string.admin_move_down)) }
                    IconButton(enabled = !state.busy, onClick = { onEvent(AdminEvent.RemoveSongArtist(artist.id)) }) { Icon(Icons.Default.Close, stringResource(R.string.admin_remove_artist)) }
                }
            }
            if (item.artists.isEmpty()) Text(stringResource(R.string.admin_artists_required), color = MaterialTheme.colorScheme.error)
            OutlinedTextField(state.pickerQuery, { onEvent(AdminEvent.Query(it, true)) }, enabled = !state.busy,
                label = { Text(stringResource(R.string.admin_find_artist)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            state.pickerArtists.filterNot { a -> item.artists.any { it.id == a.id } }.forEach { artist ->
                TextButton(enabled = !state.busy && item.artists.size < 20, onClick = { onEvent(AdminEvent.AddSongArtist(artist)) }) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(artist.name)
                }
            }
            if (state.loadingPicker) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.morePickerArtists) TextButton(enabled = !state.busy && !state.loadingPicker, onClick = { onEvent(AdminEvent.MoreArtists) }) { Text(stringResource(R.string.search_load_more)) }
        }
    }
}
