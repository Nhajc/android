package com.nhajc.android.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nhajc.android.R
import com.nhajc.android.data.model.SearchCategory
import com.nhajc.android.data.model.SearchError
import com.nhajc.android.data.model.SearchItemViewModel
import com.nhajc.android.data.model.SearchViewModel
import com.nhajc.android.ui.theme.NhajcTheme

// Pure view: all data arrives as presentation models; no DTO, API or entity imports.
@Composable
fun SearchScreen(
    model: SearchViewModel,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (SearchCategory) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(modifier = Modifier.fillMaxSize().imePadding()) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text(
                stringResource(R.string.search_title),
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = model.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (model.query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, stringResource(R.string.search_clear))
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                isError = model.query.trim().length > 100,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onSubmit() })
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                SearchCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category in model.categories,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category.label()) }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    model.isLoading -> item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.search_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    !model.hasSearched -> item { SearchWelcome() }
                    else -> {
                        if (model.items.isNotEmpty()) {
                            item {
                                Text(stringResource(R.string.search_results_title, model.query.trim()),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(pluralStringResource(R.plurals.search_results_loaded, model.items.size, model.items.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                            }
                            items(model.items, key = { "${it.category}:${it.id}" }) { item ->
                                SearchResultRow(item)
                            }
                        }
                        if (model.error != null) item {
                            SearchMessage(
                                title = stringResource(R.string.search_failed),
                                description = model.error.message(),
                                onRetry = if (model.error == SearchError.INVALID_QUERY) null else onRetry
                            )
                        }
                        else if (model.items.isEmpty()) item {
                            SearchMessage(stringResource(R.string.search_empty_title), stringResource(R.string.search_empty_description))
                        }
                        if (model.hasNext && model.error == null) item {
                            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                                if (model.isLoadingMore) CircularProgressIndicator(Modifier.size(28.dp))
                                else OutlinedButton(onClick = onLoadMore) { Text(stringResource(R.string.search_load_more)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchWelcome() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Headphones, null, Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                Text(stringResource(R.string.search_welcome_title), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.search_welcome_description),
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: SearchItemViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(64.dp)
                .clip(if (item.category == SearchCategory.ARTIST) CircleShape else RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.category.icon(), null, Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer)
            if (!item.imageUrl.isNullOrBlank()) AsyncImage(
                model = item.imageUrl, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.category.label(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            Text(item.title, style = MaterialTheme.typography.titleSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val detail = when (item.category) {
                SearchCategory.SONG -> item.artists.joinToString(", ")
                SearchCategory.RELEASE -> listOfNotNull(
                    item.artists.joinToString(", ").takeIf { it.isNotBlank() },
                    item.songCount?.let { pluralStringResource(R.plurals.search_song_count, it, it) }
                ).joinToString(" · ")
                SearchCategory.PLAYLIST -> listOfNotNull(item.ownerName,
                    item.songCount?.let { pluralStringResource(R.plurals.search_song_count, it, it) }).joinToString(" · ")
                SearchCategory.ARTIST -> ""
            }
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        item.durationSeconds?.let { duration ->
            Text("${duration / 60}:${(duration % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchMessage(
    title: String,
    description: String,
    onRetry: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(description, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (onRetry != null) OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.search_retry)) }
    }
}

@Composable
internal fun SearchCategory.label(): String = stringResource(when (this) {
    SearchCategory.SONG -> R.string.search_songs
    SearchCategory.RELEASE -> R.string.search_releases
    SearchCategory.PLAYLIST -> R.string.search_playlists
    SearchCategory.ARTIST -> R.string.search_artists
})

private fun SearchCategory.icon(): ImageVector = when (this) {
    SearchCategory.SONG -> Icons.Default.MusicNote
    SearchCategory.RELEASE -> Icons.Default.Album
    SearchCategory.PLAYLIST -> Icons.AutoMirrored.Filled.QueueMusic
    SearchCategory.ARTIST -> Icons.Default.Person
}

@Composable
internal fun SearchError.message(): String = stringResource(when (this) {
    SearchError.NETWORK -> R.string.error_network
    SearchError.UNAUTHORIZED -> R.string.error_session_ended
    SearchError.INVALID_QUERY -> R.string.search_invalid_query
    SearchError.INVALID_RESPONSE -> R.string.search_invalid_response
    SearchError.SERVER -> R.string.error_server
    SearchError.UNKNOWN -> R.string.search_unknown_error
})

@Preview(showBackground = true)
@Composable
private fun SearchWelcomePreview() {
    NhajcTheme { SearchScreen(SearchViewModel(), {}, {}, {}, {}, {}) }
}
