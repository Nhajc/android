package com.nhajc.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nhajc.android.data.repository.SearchSession
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

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
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
fun MainNavigation(searchSession: SearchSession) {
    val backStack = rememberNavBackStack(SearchNavKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<SearchNavKey> {
                SearchRoute(searchSession)
            }

            entry<SettingsNavKey> {
                SettingsScreen(onBack = { backStack.removeLastOrNull() })
            }
        }
    )
}
