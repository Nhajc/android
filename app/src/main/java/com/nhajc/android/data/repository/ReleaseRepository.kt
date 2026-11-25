package com.nhajc.android.data.repository

import androidx.room3.withWriteTransaction
import com.nhajc.android.data.database.AppDatabase
import com.nhajc.android.data.database.model.ArtistEntity
import com.nhajc.android.data.database.model.ReleaseArtistCrossRef
import com.nhajc.android.data.database.model.ReleaseInfo
import com.nhajc.android.data.database.model.SongArtistCrossRef
import com.nhajc.android.data.database.model.SongInfo
import com.nhajc.android.data.database.model.asModel
import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.model.Release
import com.nhajc.android.data.model.Song
import com.nhajc.android.data.network.LibraryApi
import com.nhajc.android.data.network.ReleaseApi
import com.nhajc.android.data.network.model.ReleaseResponse
import com.nhajc.android.data.network.model.SongSummaryResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

@Singleton
class ReleaseRepository @Inject constructor(
    private val releaseApi: ReleaseApi,
    private val libraryApi: LibraryApi,
    private val database: AppDatabase
) {
    private val artistDao = database.artistDao()
    private val releaseDao = database.releaseDao()
    private val songDao = database.songDao()

    fun release(id: Uuid): Flow<Release?> = releaseDao.observe(id).map { it?.asModel() }

    fun songs(id: Uuid): Flow<List<Song>> = releaseDao.observeSongs(id).map { songs -> songs.map { it.asModel() } }

    suspend fun refresh(id: Uuid) {
        val release = call { releaseApi.getRelease(id) }

        val artists = mutableMapOf<Uuid, ArtistEntity>()

        for (artist in release.artists) {
            artists.putIfAbsent(artist.id, artist.asEntity())
        }

        for (song in release.songs) {
            for (artist in song.artists) {
                artists.putIfAbsent(artist.id, artist.asEntity())
            }
        }

        database.withWriteTransaction {
            artistDao.upsert(artists.values.toList())
            releaseDao.upsertFromServer(listOf(release.asInfo()))
            songDao.upsertFromServer(release.songs.map { it.asInfo(release) })

            val releaseCredits = release.artists.mapIndexed { position, artist ->
                ReleaseArtistCrossRef(release.id, artist.id, position)
            }

            releaseDao.replaceArtists(release.id, releaseCredits)

            for (song in release.songs) {
                val songCredits = song.artists.mapIndexed { position, artist ->
                    SongArtistCrossRef(song.id, artist.id, position)
                }

                songDao.replaceArtists(song.id, songCredits)
            }
        }
    }

    suspend fun setSaved(id: Uuid, isSaved: Boolean) {
        if (isSaved) {
            call { libraryApi.saveRelease(id) }
        } else {
            call { libraryApi.unsaveRelease(id) }
        }

        val lastActivityAt = if (isSaved) {
            Clock.System.now()
        } else {
            null
        }

        releaseDao.setLastActivityAt(id, lastActivityAt)
    }

    private suspend fun <T> call(block: suspend () -> T): T {
        try {
            return block()
        } catch (exception: HttpException) {
            val error = when (exception.code()) {
                401 -> AppError.SESSION_ENDED
                404 -> AppError.NOT_FOUND
                else -> AppError.SERVER
            }

            throw AppException(error, exception)
        } catch (exception: IOException) {
            throw AppException(AppError.NETWORK, exception)
        }
    }
}

private fun ReleaseResponse.asInfo() = ReleaseInfo(
    id = id,
    title = title,
    searchTitle = searchText(title),
    releaseType = releaseType,
    releaseDate = releaseDate,
    coverUrl = coverUrl,
    lastActivityAt = lastActivityAt
)

private fun SongSummaryResponse.asInfo(release: ReleaseResponse) = SongInfo(
    id = id,
    releaseId = release.id,
    releaseTitle = release.title,
    coverUrl = release.coverUrl,
    title = title,
    searchTitle = searchText(title),
    trackNumber = trackNumber,
    durationSeconds = durationSeconds,
    favoritedAt = favoritedAt
)
