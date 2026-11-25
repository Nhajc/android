package com.nhajc.android.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nhajc.android.R

@Composable
fun CollectionHeader(
    title: String,
    subtitle: String,
    coverUrl: String?,
    fallbackIcon: ImageVector,
    isPlayEnabled: Boolean,
    onShuffleClick: () -> Unit,
    onPlayClick: () -> Unit,
    byline: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CoverImage(
            url = coverUrl,
            fallbackIcon = fallbackIcon,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.6f)
                .widthIn(max = 280.dp)
                .aspectRatio(1f)
        )

        Text(
            text = title,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        byline()

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onShuffleClick, enabled = isPlayEnabled) {
                Icon(Icons.Outlined.Shuffle, contentDescription = stringResource(R.string.shuffle))
            }

            FilledIconButton(
                onClick = onPlayClick,
                enabled = isPlayEnabled,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.play))
            }
        }
    }
}

@Composable
fun SaveButton(isSaved: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        if (isSaved) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.remove_from_library),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                Icons.Outlined.AddCircleOutline,
                contentDescription = stringResource(R.string.save_to_library)
            )
        }
    }
}
