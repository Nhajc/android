package com.nhajc.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nhajc.android.R

@Composable
fun DownloadIndicator(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 2.dp
        )

        Icon(
            Icons.Outlined.ArrowDownward,
            contentDescription = stringResource(R.string.downloading),
            modifier = Modifier.size(14.dp)
        )
    }
}
