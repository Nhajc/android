package com.nhajc.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.nhajc.android.R
import com.nhajc.android.data.model.LibraryView

@Composable
fun MediaListItem(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    fallbackIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRound: Boolean = false
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = if (subtitle == null) {
            null
        } else {
            { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        leadingContent = {
            CoverImage(
                url = imageUrl,
                fallbackIcon = fallbackIcon,
                modifier = Modifier.size(56.dp),
                shape = mediaShape(isRound)
            )
        }
    )
}

@Composable
fun MediaGridItem(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    fallbackIcon: ImageVector,
    showText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRound: Boolean = false
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CoverImage(
            url = imageUrl,
            fallbackIcon = fallbackIcon,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = mediaShape(isRound)
        )

        if (showText) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun mediaShape(isRound: Boolean): Shape = if (isRound) {
    CircleShape
} else {
    RoundedCornerShape(8.dp)
}

data class MediaEntry(
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val fallbackIcon: ImageVector,
    val isRound: Boolean = false
)

@Composable
fun <T : Any> PagedMediaItems(
    items: LazyPagingItems<T>,
    view: LibraryView,
    emptyText: String,
    key: (T) -> String,
    entry: @Composable (T) -> MediaEntry,
    onItemClick: (index: Int) -> Unit = {}
) {
    val isEmpty = items.itemCount == 0 && items.loadState.refresh is LoadState.NotLoading

    if (isEmpty) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = emptyText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        return
    }

    if (view == LibraryView.LIST) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(count = items.itemCount, key = items.itemKey(key)) { index ->
                val item = items[index]

                if (item != null) {
                    val current = entry(item)

                    MediaListItem(
                        title = current.title,
                        subtitle = current.subtitle,
                        imageUrl = current.imageUrl,
                        fallbackIcon = current.fallbackIcon,
                        onClick = { onItemClick(index) },
                        isRound = current.isRound
                    )
                }
            }
        }

        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(count = items.itemCount, key = items.itemKey(key)) { index ->
            val item = items[index]

            if (item != null) {
                val current = entry(item)

                MediaGridItem(
                    title = current.title,
                    subtitle = current.subtitle,
                    imageUrl = current.imageUrl,
                    fallbackIcon = current.fallbackIcon,
                    showText = view == LibraryView.GRID,
                    onClick = { onItemClick(index) },
                    isRound = current.isRound
                )
            }
        }
    }
}

@Composable
fun ViewStyleButton(view: LibraryView, onClick: () -> Unit) {
    val icon = when (view) {
        LibraryView.LIST -> Icons.AutoMirrored.Outlined.ViewList
        LibraryView.GRID -> Icons.Outlined.GridView
        LibraryView.COVERS -> Icons.Outlined.Apps
    }

    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = stringResource(R.string.change_view))
    }
}
