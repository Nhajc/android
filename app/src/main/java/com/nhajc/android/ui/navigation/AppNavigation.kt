package com.nhajc.android.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nhajc.android.data.repository.SearchRepository
import com.nhajc.android.ui.auth.LoginScreen
import com.nhajc.android.ui.auth.RegisterScreen
import com.nhajc.android.ui.search.SearchRoute
import com.nhajc.android.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object LoginNavKey : NavKey

@Serializable
data object RegisterNavKey : NavKey

@Serializable
data object SearchNavKey : NavKey

@Serializable
data object SettingsNavKey : NavKey

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
fun MainNavigation(searchRepository: SearchRepository) {
    val backStack = rememberNavBackStack(SearchNavKey)

    AppNavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<SearchNavKey> {
                SearchRoute(searchRepository)
            }

            entry<SettingsNavKey> {
                SettingsScreen(onBack = { backStack.removeLastOrNull() })
            }
        }
    )
}

private const val TRANSITION_MILLIS = 300
private const val FADE_OUT_MILLIS = 105

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>
) {
    val slideDistance = with(LocalDensity.current) { 30.dp.roundToPx() }

    NavDisplay(
        backStack = backStack,
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
