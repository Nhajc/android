package com.nhajc.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nhajc.android.data.model.AudioQuality
import com.nhajc.android.data.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val audioQualityKey = stringPreferencesKey("audio_quality")
    private val downloadQualityKey = stringPreferencesKey("download_quality")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    private val preferences: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    val audioQuality: Flow<AudioQuality> = preferences.map { values ->
        AudioQuality.entries.firstOrNull { it.name == values[audioQualityKey] }
            ?: AudioQuality.LOSSLESS
    }

    val downloadQuality: Flow<AudioQuality> = preferences.map { values ->
        AudioQuality.entries.firstOrNull { it.name == values[downloadQualityKey] }
            ?: AudioQuality.LOSSLESS
    }

    val themeMode: Flow<ThemeMode> = preferences.map { values ->
        ThemeMode.entries.firstOrNull { it.name == values[themeModeKey] } ?: ThemeMode.SYSTEM
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        dataStore.edit { values ->
            values[audioQualityKey] = quality.name
        }
    }

    suspend fun setDownloadQuality(quality: AudioQuality) {
        dataStore.edit { values ->
            values[downloadQualityKey] = quality.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { values ->
            values[themeModeKey] = mode.name
        }
    }
}
