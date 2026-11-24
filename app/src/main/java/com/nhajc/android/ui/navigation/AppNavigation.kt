package com.nhajc.android.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nhajc.android.R
import com.nhajc.android.data.repository.SearchRepository
import com.nhajc.android.ui.auth.LoginScreen
import com.nhajc.android.ui.auth.RegisterScreen
import com.nhajc.android.ui.admin.AdminRoute
import com.nhajc.android.ui.downloads.DownloadsScreen
import com.nhajc.android.ui.home.HomeScreen
import com.nhajc.android.ui.library.LibraryScreen
import com.nhajc.android.ui.player.PlayerScreen
import com.nhajc.android.ui.search.SearchRoute
import com.nhajc.android.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object LoginNavKey : NavKey

@Serializable
data object RegisterNavKey : NavKey

@Serializable
data object HomeNavKey : NavKey

@Serializable
data object SearchNavKey : NavKey

@Serializable
data object LibraryNavKey : NavKey

@Serializable
data object SettingsNavKey : NavKey

@Serializable
data object AdminNavKey : NavKey

@Serializable
data object DownloadsNavKey : NavKey

@Serializable
data object PlayerNavKey : NavKey

private enum class TopLevelDestination(
    val navKey: NavKey,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    HOME(HomeNavKey, R.string.home, Icons.Outlined.Home, Icons.Filled.Home),
    SEARCH(SearchNavKey, R.string.search, Icons.Outlined.Search, Icons.Filled.Search),
    LIBRARY(LibraryNavKey, R.string.library, Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
    ADMIN(AdminNavKey, R.string.admin_title_home, Icons.Outlined.AdminPanelSettings, Icons.Filled.AdminPanelSettings)
}

@Composable
fun AuthNavigation() {
    val backStack = rememberNavBackStack(LoginNavKey)

    AppNavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<LoginNavKey> {
                LoginScreen(onRegister = { backStack.add(RegisterNavKey) })
            }

            entry<RegisterNavKey> {
                RegisterScreen(onLogin = { backStack.removeLastOrNull() })
            }
        }
    )
}

@Composable
fun MainNavigation(searchRepository: SearchRepository, viewModel: MainNavigationViewModel = hiltViewModel()) {
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    MainNavigationContent(searchRepository, isAdmin)
}

@Composable
internal fun MainNavigationContent(searchRepository: SearchRepository, isAdmin: Boolean) {
    val backStack = rememberNavBackStack(HomeNavKey)
    var adminBusy by remember { mutableStateOf(false) }
    val destinations = TopLevelDestination.entries.filter { it != TopLevelDestination.ADMIN || isAdmin }
    LaunchedEffect(isAdmin) {
        if (!isAdmin) {
            backStack.removeAll { it == AdminNavKey }
            adminBusy = false
        }
    }

    val currentDestination = destinations.lastOrNull { it.navKey in backStack } ?: TopLevelDestination.HOME

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = destination == currentDestination

                    NavigationBarItem(
                        selected = selected,
                        enabled = !adminBusy,
                        onClick = {
                            if (adminBusy || backStack.lastOrNull() == destination.navKey) return@NavigationBarItem
                            backStack.retainAll { it == HomeNavKey }

                            if (destination != TopLevelDestination.HOME) {
                                backStack.add(destination.navKey)
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) destination.selectedIcon else destination.icon,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(destination.label)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AppNavDisplay(
            backStack = backStack,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            entryProvider = entryProvider {
                entry<HomeNavKey> {
                    HomeScreen(
                        onOpenSettings = { backStack.add(SettingsNavKey) }
                    )
                }

                entry<SearchNavKey>(metadata = fadeThroughMetadata()) {
                    SearchRoute(searchRepository)
                }

                entry<LibraryNavKey>(metadata = fadeThroughMetadata()) {
                    LibraryScreen(
                        onOpenSettings = { backStack.add(SettingsNavKey) },
                        onOpenDownloads = { backStack.add(DownloadsNavKey) }
                    )
                }

                entry<DownloadsNavKey> {
                    DownloadsScreen(onBack = { backStack.removeLastOrNull() })
                }

                entry<SettingsNavKey> {
                    SettingsScreen(onBack = { backStack.removeLastOrNull() })
                }

                entry<AdminNavKey>(metadata = fadeThroughMetadata()) {
                    if (isAdmin) AdminRoute(
                        onExit = { backStack.removeLastOrNull() },
                        onBusyChanged = { adminBusy = it }
                    )
                }

                entry<PlayerNavKey> {
                    PlayerScreen(onClose = { backStack.removeLastOrNull() })
                }
            }
        )
    }
}

private const val TRANSITION_MILLIS = 300
private const val FADE_OUT_MILLIS = 105
private const val FADE_THROUGH_INITIAL_SCALE = 0.92f

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier
) {
    val slideDistance = with(LocalDensity.current) { 30.dp.roundToPx() }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = { sharedAxisX(slideDistance) },
        popTransitionSpec = { sharedAxisX(-slideDistance) },
        predictivePopTransitionSpec = { sharedAxisX(-slideDistance) },
        entryProvider = entryProvider
    )
}

private fun sharedAxisX(slideDistance: Int): ContentTransform {
    val enter = slideInHorizontally(tween(TRANSITION_MILLIS)) { slideDistance } +
        fadeIn(tween(TRANSITION_MILLIS - FADE_OUT_MILLIS, delayMillis = FADE_OUT_MILLIS))

    val exit = slideOutHorizontally(tween(TRANSITION_MILLIS)) { -slideDistance } +
        fadeOut(tween(FADE_OUT_MILLIS))

    return enter togetherWith exit
}

private fun fadeThroughMetadata(): Map<String, Any> = NavDisplay.transitionSpec { fadeThrough() } +
    NavDisplay.popTransitionSpec { fadeThrough() } +
    NavDisplay.predictivePopTransitionSpec { fadeThrough() }

private fun fadeThrough(): ContentTransform {
    val enterSpec = tween<Float>(TRANSITION_MILLIS - FADE_OUT_MILLIS, delayMillis = FADE_OUT_MILLIS)

    val enter = fadeIn(enterSpec) + scaleIn(enterSpec, initialScale = FADE_THROUGH_INITIAL_SCALE)
    val exit = fadeOut(tween(FADE_OUT_MILLIS))

    return enter togetherWith exit
}
