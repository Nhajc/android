package com.nhajc.android.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

class SettingsDataStore(private val dataStore: DataStore<Preferences>) {
    private val audioQualityKey = stringPreferencesKey("audio_quality")

    private val themeModeKey = stringPreferencesKey("theme_mode")

    private val downloadQualityKey = stringPreferencesKey("download_quality")

    private val preferences: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    val audioQuality: Flow<AudioQuality> = preferences.map { values ->
        val storedName = values[audioQualityKey]

        parseAudioQuality(storedName)
    }

    val themeMode: Flow<ThemeMode> = preferences.map { values ->
        val storedName = values[themeModeKey]

        ThemeMode.entries.firstOrNull { mode ->
            mode.name == storedName
        } ?: ThemeMode.SYSTEM
    }

    val downloadQuality: Flow<AudioQuality> = preferences.map { values ->
        val storedName = values[downloadQualityKey]

        parseAudioQuality(storedName)
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        dataStore.edit { values ->
            values[audioQualityKey] = quality.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { values ->
            values[themeModeKey] = mode.name
        }
    }

    suspend fun setDownloadQuality(quality: AudioQuality) {
        dataStore.edit { values ->
            values[downloadQualityKey] = quality.name
        }
    }

    private fun parseAudioQuality(storedName: String?): AudioQuality {
        for (quality in AudioQuality.entries) {
            if (quality.name == storedName) {
                return quality
            }
        }

        return AudioQuality.LOSSLESS
    }
}
