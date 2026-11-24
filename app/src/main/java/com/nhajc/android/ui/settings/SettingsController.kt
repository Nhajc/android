package com.nhajc.android.ui.settings

import android.net.Uri
import com.nhajc.android.data.datastore.AudioQuality
import com.nhajc.android.data.datastore.SettingsDataStore
import com.nhajc.android.data.datastore.ThemeMode
import com.nhajc.android.data.model.AccountError
import com.nhajc.android.data.model.SettingsViewModel
import com.nhajc.android.data.repository.AccountRepository
import com.nhajc.android.data.repository.accountError
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsController(
    private val repository: AccountRepository,
    private val settings: SettingsDataStore,
    private val scope: CoroutineScope
) {
    private val mutableModel = MutableStateFlow(SettingsViewModel())

    val model = mutableModel.asStateFlow()

    init {
        scope.launch {
            settings.audioQuality.collect { quality ->
                mutableModel.update { it.copy(audioQuality = quality) }
            }
        }

        scope.launch {
            settings.downloadQuality.collect { quality ->
                mutableModel.update { it.copy(downloadQuality = quality) }
            }
        }

        scope.launch {
            settings.themeMode.collect { mode ->
                mutableModel.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun loadProfile() {
        val snapshot = model.value

        if (snapshot.isLoading || snapshot.isUpdatingAvatar || snapshot.isLoggingOut) {
            return
        }

        mutableModel.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            try {
                val account = repository.getAccount()
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(account = account) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(error = exception.accountError()) }
            } finally {
                mutableModel.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        saveSetting { settings.setAudioQuality(quality) }
    }

    fun setDownloadQuality(quality: AudioQuality) {
        saveSetting { settings.setDownloadQuality(quality) }
    }

    fun setThemeMode(mode: ThemeMode) {
        saveSetting { settings.setThemeMode(mode) }
    }

    private fun saveSetting(action: suspend () -> Unit) {
        mutableModel.update { it.copy(error = null) }

        scope.launch {
            try {
                action()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(error = AccountError.STORAGE) }
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(error = exception.accountError()) }
            }
        }
    }

    fun changeAvatar(uri: Uri) {
        updateAvatar(uri)
    }

    fun removeAvatar() {
        updateAvatar(null)
    }

    private fun updateAvatar(uri: Uri?) {
        val snapshot = model.value

        if (snapshot.isLoading || snapshot.isUpdatingAvatar || snapshot.isLoggingOut) {
            return
        }

        mutableModel.update { it.copy(isUpdatingAvatar = true, error = null) }

        scope.launch {
            try {
                val account = repository.updateAvatar(uri)
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(account = account) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(error = exception.accountError()) }
            } finally {
                mutableModel.update { it.copy(isUpdatingAvatar = false) }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        val snapshot = model.value

        if (snapshot.isLoading || snapshot.isUpdatingAvatar || snapshot.isLoggingOut) {
            return
        }

        mutableModel.update { it.copy(isLoggingOut = true, error = null) }

        scope.launch {
            try {
                repository.logout()
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(account = null) }
                onLoggedOut()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()

                mutableModel.update { it.copy(error = exception.accountError()) }
            } finally {
                mutableModel.update { it.copy(isLoggingOut = false) }
            }
        }
    }
}