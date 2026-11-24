package com.nhajc.android

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.data.model.ThemeMode
import com.nhajc.android.ui.navigation.AuthNavigation
import com.nhajc.android.ui.navigation.MainNavigation
import com.nhajc.android.ui.theme.NhajcTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            NhajcApp(uiState)
        }

        keepSplashScreenWhileLoading()
    }

    private fun keepSplashScreenWhileLoading() {
        val content = findViewById<View>(android.R.id.content)

        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (viewModel.uiState.value is MainActivityUiState.Loading) {
                        return false
                    }

                    content.viewTreeObserver.removeOnPreDrawListener(this)

                    return true
                }
            }
        )
    }
}

@Composable
fun NhajcApp(uiState: MainActivityUiState) {
    if (uiState !is MainActivityUiState.Success) {
        return
    }

    val darkTheme = when (uiState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    NhajcTheme(darkTheme = darkTheme) {
        if (uiState.isLoggedIn) {
            val application = LocalContext.current.applicationContext as NhajcApplication

            MainNavigation(application.searchSession)
        } else {
            AuthNavigation()
        }
    }
}
