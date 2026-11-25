package com.nhajc.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nhajc.android.data.model.ArtistSort
import com.nhajc.android.data.model.AudioQuality
import com.nhajc.android.data.model.DownloadSort
import com.nhajc.android.data.model.LibrarySettings
import com.nhajc.android.data.model.LibrarySort
import com.nhajc.android.data.model.LibraryTab
import com.nhajc.android.data.model.LibraryView
import com.nhajc.android.data.model.QueuePosition
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
    private val libraryTabKey = stringPreferencesKey("library_tab")
    private val libraryViewKey = stringPreferencesKey("library_view")
    private val releaseSortKey = stringPreferencesKey("release_sort")
    private val playlistSortKey = stringPreferencesKey("playlist_sort")
    private val artistSortKey = stringPreferencesKey("artist_sort")
    private val favoriteSortKey = stringPreferencesKey("favorite_sort")
    private val downloadSortKey = stringPreferencesKey("download_sort")
    private val queueIndexKey = intPreferencesKey("queue_index")
    private val queuePositionKey = longPreferencesKey("queue_position")

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

    val librarySettings: Flow<LibrarySettings> = preferences.map { values ->
        val defaults = LibrarySettings()

        LibrarySettings(
            tab = values.enumValue(libraryTabKey, defaults.tab),
            view = values.enumValue(libraryViewKey, defaults.view),
            releaseSort = values.enumValue(releaseSortKey, defaults.releaseSort),
            playlistSort = values.enumValue(playlistSortKey, defaults.playlistSort),
            artistSort = values.enumValue(artistSortKey, defaults.artistSort),
            favoriteSort = values.enumValue(favoriteSortKey, defaults.favoriteSort),
            downloadSort = values.enumValue(downloadSortKey, defaults.downloadSort)
        )
    }

    val queuePosition: Flow<QueuePosition> = preferences.map { values ->
        QueuePosition(
            index = values[queueIndexKey] ?: 0,
            positionMs = values[queuePositionKey] ?: 0
        )
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

    suspend fun setLibraryTab(tab: LibraryTab) = save(libraryTabKey, tab)

    suspend fun setLibraryView(view: LibraryView) = save(libraryViewKey, view)

    suspend fun setReleaseSort(sort: LibrarySort) = save(releaseSortKey, sort)

    suspend fun setPlaylistSort(sort: LibrarySort) = save(playlistSortKey, sort)

    suspend fun setArtistSort(sort: ArtistSort) = save(artistSortKey, sort)

    suspend fun setFavoriteSort(sort: LibrarySort) = save(favoriteSortKey, sort)

    suspend fun setDownloadSort(sort: DownloadSort) = save(downloadSortKey, sort)

    suspend fun setQueuePosition(position: QueuePosition) {
        dataStore.edit { values ->
            values[queueIndexKey] = position.index
            values[queuePositionKey] = position.positionMs
        }
    }

    private suspend fun save(key: Preferences.Key<String>, value: Enum<*>) {
        dataStore.edit { values ->
            values[key] = value.name
        }
    }
}

private inline fun <reified T : Enum<T>> Preferences.enumValue(key: Preferences.Key<String>, default: T): T =
    enumValues<T>().firstOrNull { it.name == this[key] } ?: default
