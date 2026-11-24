package com.nhajc.android.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.data.datastore.SettingsDataStore
import com.nhajc.android.data.repository.AccountRepository

@Composable
fun SettingsRoute(
    repository: AccountRepository,
    settings: SettingsDataStore,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val controller = remember(repository, settings, scope) {
        SettingsController(repository, settings, scope)
    }

    val model by controller.model.collectAsStateWithLifecycle()

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            controller.changeAvatar(uri)
        }
    }

    LaunchedEffect(controller) {
        controller.loadProfile()
    }

    SettingsScreen(
        model = model,
        onAudioQualityChange = controller::setAudioQuality,
        onDownloadQualityChange = controller::setDownloadQuality,
        onThemeModeChange = controller::setThemeMode,
        onLogout = { controller.logout(onLoggedOut) },
        onBack = onBack,
        onChangeAvatar = {
            avatarPicker.launch(arrayOf("image/jpeg", "image/png"))
        },
        onRemoveAvatar = controller::removeAvatar
    )
}
