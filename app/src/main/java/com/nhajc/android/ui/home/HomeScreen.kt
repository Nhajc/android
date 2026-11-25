package com.nhajc.android.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.R
import com.nhajc.android.data.model.LibraryTab
import com.nhajc.android.ui.common.UserAvatarButton
import com.nhajc.android.ui.theme.NhajcTheme
import kotlin.uuid.Uuid

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenArtist: (Uuid) -> Unit,
    onOpenRelease: (Uuid) -> Unit,
    onOpenPlaylist: (Uuid) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState, onOpenSettings = onOpenSettings,
        onOpenSearch = onOpenSearch,
        onOpenLibrary = { tab -> viewModel.openLibrary(tab, onOpenLibrary) },
        onOpenArtist = onOpenArtist, onOpenRelease = onOpenRelease,
        onOpenPlaylist = onOpenPlaylist,
        onRefresh = viewModel::refresh, onPlayFavorite = viewModel::playFavorite
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSearch: () -> Unit = {},
    onOpenLibrary: (LibraryTab) -> Unit = {},
    onOpenArtist: (Uuid) -> Unit = {},
    onOpenRelease: (Uuid) -> Unit = {},
    onOpenPlaylist: (Uuid) -> Unit = {},
    onRefresh: () -> Unit = {},
    onPlayFavorite: (Uuid) -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    UserAvatarButton(
                        avatarUrl = uiState.user?.avatarUrl,
                        onClick = onOpenSettings
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                        Icon(Icons.Outlined.Refresh, stringResource(R.string.home_refresh))
                    }
                }
            )
        }
    ) { innerPadding ->
        HomeFeed(
            uiState = uiState,
            onOpenSearch = onOpenSearch,
            onOpenLibrary = onOpenLibrary,
            onOpenArtist = onOpenArtist,
            onOpenRelease = onOpenRelease,
            onOpenPlaylist = onOpenPlaylist,
            onRefresh = onRefresh,
            onPlayFavorite = onPlayFavorite,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    }
}

@Preview(name = "Home Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Home Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        HomeScreen(uiState = HomeUiState(), onOpenSettings = {})
    }
}
