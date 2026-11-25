package com.nhajc.android.ui.admin

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.More
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nhajc.android.R
import com.nhajc.android.data.model.AdminError
import com.nhajc.android.data.model.AdminFile
import com.nhajc.android.data.model.AdminPage
import com.nhajc.android.data.model.AdminRelease
import com.nhajc.android.data.model.AdminUiState
import com.nhajc.android.data.model.AdminValidation
import com.nhajc.android.data.model.ReleaseType
import com.nhajc.android.data.model.UploadQueueItem
import com.nhajc.android.data.model.UploadStatus

@Composable
fun AdminRoute(
    onExit: () -> Unit,
    onBusyChanged: (Boolean) -> Unit = {},
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val back = { if (!viewModel.back()) onExit() }
    LaunchedEffect(state.busy, state.uploadQueue) {
        onBusyChanged(state.busy || state.uploadQueue.any { it.uploadedId != null })
    }
    DisposableEffect(Unit) { onDispose { onBusyChanged(false) } }
    BackHandler { back() }
    AdminScreen(state, viewModel::handle, back)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminScreen(state: AdminUiState, onEvent: (AdminEvent) -> Unit, onBack: () -> Unit) {
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onEvent(AdminEvent.FileSelected(it, true)) }
        }
    val batchPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) onEvent(AdminEvent.AudioFilesSelected(uris))
        }
    val listState = rememberLazyListState()
    val drag =
        rememberSongDragState(listState) { id, target -> onEvent(AdminEvent.MoveSong(id, target)) }
    LaunchedEffect(state.page, state.songOrder == null) { drag.stop() }
    LaunchedEffect(state.page) { listState.scrollToItem(0) }
    LaunchedEffect(state.editingUpload) {
        if (state.page == AdminPage.REVIEW_UPLOADS &&
            state.editingUpload != null
        ) {
            listState.animateScrollToItem(0)
        }
    }
    LaunchedEffect(state.error) { if (state.error != null) listState.animateScrollToItem(0) }
    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, topBar = {
        TopAppBar(
            title = {
                Text(
                    stringResource(state.page.title()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            navigationIcon = {
                IconButton(onClick = onBack, enabled = !state.busy) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            }
        )
    }) { padding ->
        if (state.confirmDiscard) {
            AlertDialog(
                onDismissRequest = { onEvent(AdminEvent.KeepUploads) },
                title = { Text(stringResource(R.string.admin_discard_title)) },
                text = { Text(stringResource(R.string.admin_discard_hint)) },
                confirmButton = {
                    TextButton(enabled = !state.busy, onClick = {
                        onEvent(AdminEvent.DiscardUploads)
                    }) { Text(stringResource(R.string.admin_discard)) }
                },
                dismissButton = {
                    TextButton(enabled = !state.busy, onClick = {
                        onEvent(AdminEvent.KeepUploads)
                    }) { Text(stringResource(R.string.admin_keep_editing)) }
                }
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(
                padding
            ).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.checkingAccess -> item { AdminLoading() }

                !state.isAdmin -> item { Text(stringResource(R.string.admin_access_required)) }

                else -> {
                    if (state.saved) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        stringResource(R.string.admin_saved),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    if (state.error != null) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(state.error.message()),
                                    color = MaterialTheme.colorScheme.error
                                )
                                if (!state.busy &&
                                    state.page in
                                    listOf(
                                        AdminPage.ARTISTS,
                                        AdminPage.RELEASES,
                                        AdminPage.GENRES,
                                        AdminPage.ARTIST,
                                        AdminPage.RELEASE,
                                        AdminPage.NEW_RELEASE,
                                        AdminPage.REVIEW_UPLOADS
                                    )
                                ) {
                                    TextButton(onClick = {
                                        onEvent(AdminEvent.Retry)
                                    }) { Text(stringResource(R.string.search_retry)) }
                                }
                            }
                        }
                    }
                    when (state.page) {
                        AdminPage.HOME -> {
                            item { AdminHero() }
                            item {
                                AdminDestination(
                                    R.string.search_artists,
                                    R.string.admin_artists_description,
                                    Icons.Default.Person
                                ) {
                                    onEvent(AdminEvent.Navigate(AdminPage.ARTISTS))
                                }
                            }
                            item {
                                AdminDestination(
                                    R.string.admin_genres,
                                    R.string.admin_genres_description,
                                    Icons.Default.Category
                                ) {
                                    onEvent(AdminEvent.Navigate(AdminPage.GENRES))
                                }
                            }
                            item {
                                AdminDestination(
                                    R.string.search_releases,
                                    R.string.admin_releases_description,
                                    Icons.Default.Album
                                ) {
                                    onEvent(AdminEvent.Navigate(AdminPage.RELEASES))
                                }
                            }
                        }

                        AdminPage.ARTISTS, AdminPage.RELEASES -> {
                            val artists = state.page == AdminPage.ARTISTS
                            item {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        pluralStringResource(
                                            R.plurals.admin_items_loaded,
                                            if (artists) {
                                                state.artists.size
                                            } else {
                                                state.releases.size
                                            },
                                            if (artists) state.artists.size else state.releases.size
                                        ),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FilledTonalButton(onClick = {
                                        onEvent(
                                            AdminEvent.Navigate(
                                                if (artists) {
                                                    AdminPage.NEW_ARTIST
                                                } else {
                                                    AdminPage.NEW_RELEASE
                                                }
                                            )
                                        )
                                    }, shape = RoundedCornerShape(14.dp)) {
                                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            stringResource(
                                                if (artists) {
                                                    R.string.admin_add_artist
                                                } else {
                                                    R.string.admin_add_release
                                                }
                                            )
                                        )
                                    }
                                }
                            }
                            item {
                                OutlinedTextField(
                                    value = state.query, onValueChange = {
                                        onEvent(AdminEvent.Query(it))
                                    },
                                    label = {
                                        Text(
                                            stringResource(
                                                if (artists) {
                                                    R.string.admin_find_artist
                                                } else {
                                                    R.string.admin_find_release
                                                }
                                            )
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = {
                                        if (state.query.isNotEmpty()) {
                                            IconButton(onClick = {
                                                onEvent(AdminEvent.Query(""))
                                            }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    stringResource(R.string.search_clear)
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    isError = state.query.trim().length > 100,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (artists) {
                                items(state.artists, key = { it.id.toString() }) { artist ->
                                    CatalogCard(
                                        artist.name,
                                        null,
                                        artist.avatarUrl,
                                        artist = true
                                    ) {
                                        onEvent(AdminEvent.OpenArtist(artist.id))
                                    }
                                }
                            } else {
                                items(state.releases, key = { it.id.toString() }) { release ->
                                    ReleaseCard(release) {
                                        onEvent(AdminEvent.OpenRelease(release.id))
                                    }
                                }
                            }
                            if (state.loading) {
                                item { AdminLoading() }
                            } else if (state.error == null &&
                                (if (artists) state.artists.isEmpty() else state.releases.isEmpty())
                            ) {
                                item {
                                    Text(
                                        stringResource(
                                            if (state.query.isBlank()) {
                                                R.string.admin_catalog_empty
                                            } else {
                                                R.string.search_empty_title
                                            }
                                        )
                                    )
                                }
                            }
                            if (state.hasNext) {
                                item {
                                    MoreButton(state.loading) { onEvent(AdminEvent.More) }
                                }
                            }
                        }

                        AdminPage.GENRES -> {
                            item {
                                Button(onClick = {
                                    onEvent(AdminEvent.Navigate(AdminPage.NEW_GENRE))
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.admin_add_genre))
                                }
                            }
                            items(state.genres, key = { it.id.toString() }) { genre ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Text(genre.name, Modifier.fillMaxWidth().padding(16.dp))
                                }
                            }
                            if (state.loadingGenres) {
                                item { AdminLoading() }
                            } else if (state.genres.isEmpty() &&
                                state.error == null
                            ) {
                                item { Text(stringResource(R.string.admin_no_genres)) }
                            }
                            if (state.moreGenres) {
                                item {
                                    MoreButton(state.loadingGenres) {
                                        onEvent(AdminEvent.MoreGenres)
                                    }
                                }
                            }
                        }

                        AdminPage.NEW_ARTIST -> item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                AdminTextField(
                                    state.artistDraft.name,
                                    R.string.admin_name,
                                    state.busy,
                                    255
                                ) {
                                    onEvent(AdminEvent.Field(AdminField.ARTIST_NAME, it))
                                }
                                FileSelection(
                                    state.artistDraft.image,
                                    true,
                                    state.busy,
                                    {
                                        imagePicker.launch(arrayOf("image/jpeg", "image/png"))
                                    },
                                    { onEvent(AdminEvent.RemoveFile) }
                                )
                                SaveButton(state.busy) { onEvent(AdminEvent.Submit) }
                            }
                        }

                        AdminPage.NEW_GENRE -> item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                AdminTextField(
                                    state.genreName,
                                    R.string.admin_name,
                                    state.busy,
                                    100
                                ) {
                                    onEvent(AdminEvent.Field(AdminField.GENRE_NAME, it))
                                }
                                SaveButton(state.busy) { onEvent(AdminEvent.Submit) }
                            }
                        }

                        AdminPage.NEW_RELEASE -> item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                AdminTextField(
                                    state.releaseDraft.title,
                                    R.string.admin_title,
                                    state.busy,
                                    255
                                ) {
                                    onEvent(AdminEvent.Field(AdminField.RELEASE_TITLE, it))
                                }
                                Text(
                                    stringResource(R.string.admin_release_type),
                                    fontWeight = FontWeight.SemiBold
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ReleaseType.entries.filter {
                                        it != ReleaseType.UNKNOWN
                                    }.forEach { type ->
                                        FilterChip(
                                            selected = state.releaseDraft.type == type,
                                            enabled = !state.busy,
                                            onClick = {
                                                onEvent(AdminEvent.Type(type))
                                            },
                                            label = { Text(stringResource(type.label())) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = state.releaseDraft.date,
                                    onValueChange = {
                                        onEvent(AdminEvent.Field(AdminField.RELEASE_DATE, it))
                                    },
                                    enabled = !state.busy,
                                    label = {
                                        Text(stringResource(R.string.admin_release_date))
                                    },
                                    placeholder = {
                                        Text(stringResource(R.string.admin_date_format))
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    stringResource(R.string.admin_artist_credits),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    stringResource(R.string.admin_credit_order),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                state.releaseDraft.artists.forEachIndexed { index, artist ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${index + 1}. ${artist.name}", Modifier.weight(1f))
                                        IconButton(
                                            enabled = !state.busy && index > 0,
                                            onClick = {
                                                onEvent(AdminEvent.MoveArtist(artist.id, -1))
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                stringResource(R.string.admin_move_up)
                                            )
                                        }
                                        IconButton(
                                            enabled =
                                                !state.busy &&
                                                    index < state.releaseDraft.artists.lastIndex,
                                            onClick = {
                                                onEvent(AdminEvent.MoveArtist(artist.id, 1))
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                stringResource(R.string.admin_move_down)
                                            )
                                        }
                                        IconButton(enabled = !state.busy, onClick = {
                                            onEvent(AdminEvent.RemoveArtist(artist.id))
                                        }) {
                                            Icon(
                                                Icons.Default.Close,
                                                stringResource(R.string.admin_remove_artist)
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    state.pickerQuery,
                                    {
                                        onEvent(AdminEvent.Query(it, true))
                                    },
                                    label = {
                                        Text(stringResource(R.string.admin_find_artist))
                                    },
                                    singleLine = true,
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                state.pickerArtists.filterNot { a ->
                                    state.releaseDraft.artists.any {
                                        it.id ==
                                            a.id
                                    }
                                }.forEach { artist ->
                                    OutlinedButton(onClick = {
                                        onEvent(AdminEvent.AddArtist(artist))
                                    }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Add, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(artist.name)
                                    }
                                }
                                if (state.loadingPicker) {
                                    AdminLoading()
                                } else if (state.pickerQuery.isNotBlank() &&
                                    state.pickerArtists.isEmpty()
                                ) {
                                    Text(stringResource(R.string.search_empty_title))
                                }
                                if (state.morePickerArtists) {
                                    MoreButton(state.loadingPicker || state.busy) {
                                        onEvent(AdminEvent.MoreArtists)
                                    }
                                }
                                Text(
                                    stringResource(R.string.admin_genres_optional),
                                    fontWeight = FontWeight.SemiBold
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.genres.forEach { genre ->
                                        FilterChip(
                                            selected =
                                                genre.id in state.releaseDraft.genreIds,
                                            enabled = !state.busy,
                                            onClick = {
                                                onEvent(AdminEvent.ToggleGenre(genre.id))
                                            },
                                            label = { Text(genre.name) }
                                        )
                                    }
                                }
                                if (state.loadingGenres) {
                                    AdminLoading()
                                } else if (state.genres.isEmpty()) {
                                    Text(stringResource(R.string.admin_no_genres))
                                }
                                if (state.moreGenres) {
                                    MoreButton(
                                        state.loadingGenres || state.busy
                                    ) {
                                        onEvent(AdminEvent.MoreGenres)
                                    }
                                }
                                FileSelection(
                                    state.releaseDraft.image,
                                    true,
                                    state.busy,
                                    {
                                        imagePicker.launch(arrayOf("image/jpeg", "image/png"))
                                    },
                                    { onEvent(AdminEvent.RemoveFile) }
                                )
                                SaveButton(state.busy) { onEvent(AdminEvent.Submit) }
                            }
                        }

                        AdminPage.ARTIST -> {
                            if (state.loading) {
                                item { AdminLoading() }
                            } else {
                                state.artist?.let { detail ->
                                    item {
                                        CatalogCard(
                                            detail.artist.name,
                                            null,
                                            detail.artist.avatarUrl,
                                            artist = true
                                        )
                                    }
                                    item {
                                        Text(
                                            stringResource(R.string.search_releases),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    if (detail.releases.isEmpty()) {
                                        item {
                                            Text(stringResource(R.string.admin_no_releases))
                                        }
                                    }
                                    items(detail.releases, key = {
                                        it.id.toString()
                                    }) { release ->
                                        ReleaseCard(release) {
                                            onEvent(AdminEvent.OpenRelease(release.id))
                                        }
                                    }
                                }
                            }
                        }

                        AdminPage.RELEASE -> {
                            if (state.loading) {
                                item { AdminLoading() }
                            } else {
                                state.release?.let { detail ->
                                    item { ReleaseCard(detail.release) }
                                    if (state.songOrder == null) {
                                        item {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        onEvent(
                                                            AdminEvent.Navigate(
                                                                AdminPage.BATCH_UPLOAD
                                                            )
                                                        )
                                                    },
                                                    enabled = !state.busy,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.QueueMusic, null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(stringResource(R.string.admin_upload_song))
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                stringResource(R.string.search_songs),
                                                Modifier.weight(1f),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (state.songOrder == null &&
                                                detail.songs.size > 1
                                            ) {
                                                TextButton(
                                                    enabled = !state.busy,
                                                    onClick = {
                                                        onEvent(AdminEvent.BeginReorder)
                                                    }
                                                ) { Text(stringResource(R.string.admin_reorder)) }
                                            }
                                        }
                                    }
                                    if (detail.songs.isEmpty()) {
                                        item {
                                            Text(stringResource(R.string.admin_no_songs))
                                        }
                                    }
                                    val ordered = state.songOrder?.let { ids ->
                                        val songs = detail.songs.associateBy { it.id }
                                        ids.mapNotNull { songs[it] }
                                    }
                                    if (ordered != null) {
                                        item {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    stringResource(R.string.admin_reorder_hint),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        8.dp
                                                    )
                                                ) {
                                                    Button(
                                                        enabled =
                                                            !state.busy && drag.draggedId == null,
                                                        onClick = {
                                                            onEvent(AdminEvent.SaveOrder)
                                                        }
                                                    ) {
                                                        Text(
                                                            stringResource(
                                                                R.string.admin_save_order
                                                            )
                                                        )
                                                    }
                                                    TextButton(enabled = !state.busy, onClick = {
                                                        onEvent(AdminEvent.CancelReorder)
                                                    }) {
                                                        Text(
                                                            stringResource(
                                                                R.string.admin_cancel_order
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        items(ordered, key = { "song:${it.id}" }) { song ->
                                            val index = ordered.indexOfFirst { it.id == song.id }
                                            ReorderSongRow(
                                                song,
                                                index + 1,
                                                ordered.getOrNull(index - 1)?.id,
                                                ordered.getOrNull(index + 1)?.id,
                                                drag,
                                                !state.busy
                                            ) { id, target ->
                                                onEvent(AdminEvent.MoveSong(id, target))
                                            }
                                        }
                                    } else {
                                        items(detail.songs, key = { "song:${it.id}" }) { song ->
                                            ListItem(
                                                headlineContent = {
                                                    Text(song.title)
                                                },
                                                leadingContent = {
                                                    Text(song.trackNumber.toString())
                                                },
                                                supportingContent = {
                                                    Column {
                                                        if (song.artists.isNotEmpty()) {
                                                            Text(
                                                                song.artists.joinToString(", ") {
                                                                    it.name
                                                                }
                                                            )
                                                        }
                                                        song.description?.let {
                                                            Text(
                                                                it,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                },
                                                trailingContent = {
                                                    val minutes = song.durationSeconds / 60
                                                    val seconds = (song.durationSeconds % 60)
                                                        .toString()
                                                        .padStart(2, '0')

                                                    Text("$minutes:$seconds")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AdminPage.BATCH_UPLOAD -> batchUploadContent(state, onEvent) {
                            batchPicker.launch(arrayOf("*/*"))
                        }

                        AdminPage.REVIEW_UPLOADS -> reviewUploadsContent(state, onEvent, drag)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.batchUploadContent(
    state: AdminUiState,
    onEvent: (AdminEvent) -> Unit,
    pick: () -> Unit
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.release?.release?.let { ReleaseCard(it) }
            Text(
                stringResource(R.string.admin_queue_hint),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = pick,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.admin_choose_files))
            }
            if (state.uploadQueue.isNotEmpty()) {
                Text(
                    stringResource(
                        R.string.admin_queue_summary,
                        state.uploadQueue.count {
                            it.status ==
                                UploadStatus.DONE
                        },
                        state.uploadQueue.size
                    )
                )
                if (state.busy) {
                    Text(
                        stringResource(R.string.admin_queue_keep_open),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!state.busy && state.uploadQueue.any { it.status == UploadStatus.WAITING }) {
                Button(onClick = {
                    onEvent(AdminEvent.StartQueue)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.admin_start_queue))
                }
            }
            if (!state.busy && state.uploadQueue.any { it.status == UploadStatus.DONE }) {
                Button(onClick = {
                    onEvent(AdminEvent.ReviewUploads)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.admin_review_songs))
                }
            }
        }
    }
    items(state.uploadQueue, key = {
        it.id.toString()
    }) { item -> UploadQueueRow(item, state.busy, onEvent) }
}

@Composable
private fun UploadQueueRow(item: UploadQueueItem, busy: Boolean, onEvent: (AdminEvent) -> Unit) {
    OutlinedCard(
        Modifier.fillMaxWidth().testTag("upload-queue:${item.id}"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (item.status) {
                    UploadStatus.UPLOADING, UploadStatus.PROCESSING -> CircularProgressIndicator(
                        Modifier.size(24.dp).testTag("queue-spinner:${item.id}"),
                        strokeWidth = 2.dp
                    )

                    UploadStatus.DONE -> Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    UploadStatus.FAILED -> Icon(
                        Icons.Default.ErrorOutline,
                        null,
                        tint = MaterialTheme.colorScheme.error
                    )

                    UploadStatus.WAITING -> Icon(
                        Icons.Default.Schedule,
                        null,
                        Modifier.testTag("queue-waiting:${item.id}"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(item.status.label()),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.status ==
                            UploadStatus.FAILED
                        ) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        item.file.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!busy &&
                    item.status != UploadStatus.DONE
                ) {
                    IconButton(onClick = { onEvent(AdminEvent.RemoveQueued(item.id)) }) {
                        Icon(Icons.Default.Close, stringResource(R.string.admin_remove_file))
                    }
                }
            }
            if (item.status == UploadStatus.UPLOADING && item.progress != null) {
                LinearProgressIndicator(progress = {
                    item.progress
                }, modifier = Modifier.fillMaxWidth())
            }
            if (!busy && item.status == UploadStatus.WAITING) {
                OutlinedTextField(
                    item.title,
                    {
                        onEvent(AdminEvent.QueueTitle(item.id, it))
                    },
                    label = { Text(stringResource(R.string.admin_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError =
                        AdminValidation.name(item.title) != null
                )
            }
            item.error?.let { error ->
                Text(
                    stringResource(error.message()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!busy && item.status == UploadStatus.FAILED) {
                if (item.error ==
                    AdminError.NETWORK
                ) {
                    Text(
                        stringResource(R.string.admin_queue_network_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = {
                    onEvent(AdminEvent.RetryQueued(item.id))
                }) { Text(stringResource(R.string.search_retry)) }
            }
        }
    }
}

private fun UploadStatus.label() = when (this) {
    UploadStatus.WAITING -> R.string.admin_queue_waiting
    UploadStatus.UPLOADING -> R.string.admin_queue_uploading
    UploadStatus.PROCESSING -> R.string.admin_queue_processing
    UploadStatus.DONE -> R.string.admin_queue_done
    UploadStatus.FAILED -> R.string.admin_queue_failed
}

@Composable
private fun AdminHero() {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.LibraryMusic,
                null,
                Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.admin_catalog_heading),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.admin_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AdminDestination(title: Int, description: Int, icon: ImageVector, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(
                    48.dp
                ).clip(
                    RoundedCornerShape(14.dp)
                ).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    Modifier.size(25.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CatalogCard(
    title: String,
    subtitle: String?,
    image: String?,
    artist: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = onClick != null) {
                onClick?.invoke()
            }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(54.dp).clip(if (artist) CircleShape else RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (artist) Icons.Default.Person else Icons.Default.Album,
                    null,
                    Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (!image.isNullOrBlank()) {
                    AsyncImage(
                        image,
                        null,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (onClick ==
                            null
                        ) {
                            4
                        } else {
                            2
                        },
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onClick !=
                null
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: AdminRelease, onClick: (() -> Unit)? = null) = CatalogCard(
    release.title,
    listOf(
        release.artists.joinToString(", "),
        release.date,
        stringResource(release.type.label())
    ).filter {
        it.isNotEmpty()
    }.joinToString(" · "),
    release.coverUrl,
    onClick = onClick
)

@Composable
private fun FormPanel(title: Int, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun AdminTextField(
    value: String,
    label: Int,
    busy: Boolean,
    limit: Int,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value, onChange, label = { Text(stringResource(label)) }, enabled = !busy,
        singleLine = true, shape = RoundedCornerShape(14.dp), isError = value.trim().length > limit,
        supportingText = {
            Text(stringResource(R.string.admin_character_limit, limit))
        }, modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FileSelection(
    file: AdminFile?,
    image: Boolean,
    busy: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    FormPanel(if (image) R.string.admin_image_optional else R.string.admin_audio_file) {
        if (image &&
            file != null
        ) {
            AsyncImage(
                file.uri,
                null,
                Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Fit
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (image) Icons.Default.Image else Icons.Default.AudioFile,
                null,
                Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    file?.name ?: stringResource(R.string.admin_no_file_selected),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                file?.size?.let {
                    Text(
                        stringResource(R.string.admin_file_size, it / 1024),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (file !=
                null
            ) {
                IconButton(onClick = onRemove, enabled = !busy) {
                    Icon(Icons.Default.Close, stringResource(R.string.admin_remove_file))
                }
            }
        }
        OutlinedButton(
            onClick = onPick,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(
                    if (image) R.string.admin_choose_image else R.string.admin_choose_flac
                )
            )
        }
        Text(
            stringResource(
                if (image) R.string.admin_image_requirements else R.string.admin_audio_requirements
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SaveButton(busy: Boolean, onClick: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    Button(
        {
            keyboard?.hide()
            focus.clearFocus()
            onClick()
        },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        } else {
            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(stringResource(if (busy) R.string.admin_saving else R.string.admin_save))
    }
}

@Composable private fun MoreButton(loading: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.search_load_more))
    }
}

@Composable private fun AdminLoading() {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun AdminPage.title() = when (this) {
    AdminPage.HOME -> R.string.admin_title_home
    AdminPage.ARTISTS, AdminPage.ARTIST -> R.string.search_artists
    AdminPage.GENRES -> R.string.admin_genres
    AdminPage.RELEASES, AdminPage.RELEASE -> R.string.search_releases
    AdminPage.NEW_ARTIST -> R.string.admin_add_artist
    AdminPage.NEW_GENRE -> R.string.admin_add_genre
    AdminPage.NEW_RELEASE -> R.string.admin_add_release
    AdminPage.BATCH_UPLOAD -> R.string.admin_upload_song
    AdminPage.REVIEW_UPLOADS -> R.string.admin_review_songs
}

private fun ReleaseType.label() = when (this) {
    ReleaseType.ALBUM -> R.string.admin_album
    ReleaseType.SINGLE -> R.string.admin_single
    ReleaseType.EP -> R.string.admin_ep
    ReleaseType.COMPILATION -> R.string.admin_compilation
    ReleaseType.UNKNOWN -> R.string.admin_unknown_type
}

private fun AdminError.message() = when (this) {
    AdminError.INVALID_INPUT -> R.string.admin_invalid_input
    AdminError.INVALID_DATE -> R.string.admin_invalid_date
    AdminError.ARTISTS_REQUIRED -> R.string.admin_artists_required
    AdminError.TOO_MANY_SELECTIONS -> R.string.admin_too_many_selections
    AdminError.INVALID_TRACK -> R.string.admin_invalid_track
    AdminError.TRACK_EXISTS -> R.string.admin_track_exists
    AdminError.GENRE_EXISTS -> R.string.admin_genre_exists
    AdminError.INVALID_IMAGE -> R.string.error_invalid_avatar
    AdminError.INVALID_AUDIO -> R.string.admin_invalid_audio
    AdminError.IMAGE_TOO_LARGE -> R.string.admin_image_too_large
    AdminError.AUDIO_TOO_LARGE -> R.string.admin_audio_too_large
    AdminError.FILE_UNAVAILABLE -> R.string.admin_file_unavailable
    AdminError.NETWORK -> R.string.error_network
    AdminError.UNAUTHORIZED -> R.string.error_session_ended
    AdminError.FORBIDDEN -> R.string.admin_access_required
    AdminError.NOT_FOUND -> R.string.admin_not_found
    AdminError.CONFLICT -> R.string.admin_conflict
    AdminError.TOO_LARGE -> R.string.admin_too_large
    AdminError.UNSUPPORTED_AUDIO -> R.string.admin_unsupported_audio
    AdminError.BUSY -> R.string.admin_server_busy
    AdminError.UNAVAILABLE -> R.string.admin_service_unavailable
    AdminError.SERVER -> R.string.error_server
    AdminError.ORDER_CHANGED -> R.string.admin_order_changed
}
