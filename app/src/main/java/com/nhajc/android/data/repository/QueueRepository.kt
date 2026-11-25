package com.nhajc.android.data.repository

import androidx.room3.withWriteTransaction
import com.nhajc.android.data.database.AppDatabase
import com.nhajc.android.data.database.model.ArtistEntity
import com.nhajc.android.data.database.model.QueueItemEntity
import com.nhajc.android.data.database.model.SongArtistCrossRef
import com.nhajc.android.data.database.model.SongInfo
import com.nhajc.android.data.database.model.asModel
import com.nhajc.android.data.model.QueuePosition
import com.nhajc.android.data.model.SavedQueue
import com.nhajc.android.data.model.Song
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlinx.coroutines.flow.first

@Singleton
class QueueRepository @Inject constructor(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val queueDao = database.queueDao()
    private val songDao = database.songDao()
    private val artistDao = database.artistDao()

    suspend fun replace(songs: List<Song>) {
        val songInfos = mutableListOf<SongInfo>()
        val artists = mutableListOf<ArtistEntity>()
        val credits = mutableListOf<SongArtistCrossRef>()
        val items = mutableListOf<QueueItemEntity>()

        for ((position, song) in songs.withIndex()) {
            songInfos += song.asInfo()
            items += QueueItemEntity(position, song.id)

            for ((artistPosition, artist) in song.artists.withIndex()) {
                artists += ArtistEntity(
                    id = artist.id,
                    name = artist.name,
                    searchName = searchText(artist.name),
                    avatarUrl = artist.avatarUrl,
                    followedAt = null
                )
                credits += SongArtistCrossRef(song.id, artist.id, artistPosition)
            }
        }

        database.withWriteTransaction {
            artistDao.insertIfMissing(artists)
            songDao.insertIfMissing(songInfos)
            songDao.insertArtistsIfMissing(credits)
            queueDao.replace(items)
        }
    }

    suspend fun savePosition(position: QueuePosition) {
        settingsRepository.setQueuePosition(position)
    }

    suspend fun saved(): SavedQueue {
        val songs = queueDao.observeSongs().first().map { it.asModel() }
        val position = settingsRepository.queuePosition.first()

        return SavedQueue(songs, position)
    }
}

private fun Song.asInfo(): SongInfo {
    val favoritedAt = if (isFavorite) {
        Clock.System.now()
    } else {
        null
    }

    return SongInfo(
        id = id,
        releaseId = releaseId,
        releaseTitle = releaseTitle,
        coverUrl = coverUrl,
        title = title,
        searchTitle = searchText(title),
        trackNumber = trackNumber,
        durationSeconds = durationSeconds,
        favoritedAt = favoritedAt
    )
}
