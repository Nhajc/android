package com.nhajc.android.data.repository

import androidx.room3.withWriteTransaction
import com.nhajc.android.data.database.AppDatabase
import com.nhajc.android.data.database.model.ArtistEntity
import com.nhajc.android.data.database.model.PlaylistInfo
import com.nhajc.android.data.database.model.PlaylistSongCrossRef
import com.nhajc.android.data.database.model.SongArtistCrossRef
import com.nhajc.android.data.database.model.asModel
import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.model.Playlist
import com.nhajc.android.data.model.Song
import com.nhajc.android.data.network.LibraryApi
import com.nhajc.android.data.network.PlaylistApi
import com.nhajc.android.data.network.model.PlaylistRequest
import com.nhajc.android.data.network.model.PlaylistResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistApi: PlaylistApi,
    private val libraryApi: LibraryApi,
    private val database: AppDatabase
) {
    private val userDao = database.userDao()
    private val artistDao = database.artistDao()
    private val playlistDao = database.playlistDao()
    private val songDao = database.songDao()

    fun playlist(id: Uuid): Flow<Playlist?> = playlistDao.observe(id).map { it?.asModel() }

    fun songs(id: Uuid): Flow<List<Song>> =
        playlistDao.observeSongs(id).map { songs -> songs.map { it.asModel() } }

    fun ownedPlaylists(ownerId: Uuid): Flow<List<Playlist>> =
        playlistDao.observeOwned(ownerId).map { playlists -> playlists.map { it.asModel() } }

    suspend fun refresh(id: Uuid) {
        val playlist = call { playlistApi.getPlaylist(id) }

        store(playlist)
    }

    suspend fun create(title: String): Uuid {
        val request = PlaylistRequest(title = title, isPrivate = false, songIds = emptyList())
        val playlist = call(conflict = AppError.PLAYLIST_LIMIT) {
            playlistApi.createPlaylist(request, null)
        }

        store(playlist)

        return playlist.id
    }

    suspend fun addSong(playlistId: Uuid, songId: Uuid) {
        call(conflict = AppError.PLAYLIST_FULL) { playlistApi.addSong(playlistId, songId) }

        playlistDao.appendSong(playlistId, songId)
    }

    suspend fun setSaved(id: Uuid, isSaved: Boolean) {
        if (isSaved) {
            call { libraryApi.savePlaylist(id) }
        } else {
            call { libraryApi.unsavePlaylist(id) }
        }

        val lastActivityAt = if (isSaved) {
            Clock.System.now()
        } else {
            null
        }

        playlistDao.setLastActivityAt(id, lastActivityAt)
    }

    suspend fun delete(id: Uuid) {
        call { playlistApi.deletePlaylist(id) }

        playlistDao.delete(id)
    }

    private suspend fun store(playlist: PlaylistResponse) {
        val artists = mutableMapOf<Uuid, ArtistEntity>()

        for (song in playlist.songs) {
            for (artist in song.artists) {
                artists.putIfAbsent(artist.id, artist.asEntity())
            }
        }

        val positions = playlist.songs.mapIndexed { position, song ->
            PlaylistSongCrossRef(playlist.id, song.id, position)
        }

        database.withWriteTransaction {
            userDao.upsert(listOf(playlist.owner.asEntity()))
            artistDao.upsert(artists.values.toList())
            songDao.upsertFromServer(playlist.songs.map { it.asInfo() })

            for (song in playlist.songs) {
                val credits = song.artists.mapIndexed { position, artist ->
                    SongArtistCrossRef(song.id, artist.id, position)
                }

                songDao.replaceArtists(song.id, credits)
            }

            playlistDao.upsertFromServer(listOf(playlist.asInfo()))
            playlistDao.replaceSongs(playlist.id, positions)
        }
    }

    private suspend fun <T> call(conflict: AppError = AppError.SERVER, block: suspend () -> T): T {
        try {
            return block()
        } catch (exception: HttpException) {
            val error = when (exception.code()) {
                401 -> AppError.SESSION_ENDED
                404 -> AppError.NOT_FOUND
                409 -> conflict
                else -> AppError.SERVER
            }

            throw AppException(error, exception)
        } catch (exception: IOException) {
            throw AppException(AppError.NETWORK, exception)
        }
    }
}

private fun PlaylistResponse.asInfo() = PlaylistInfo(
    id = id,
    title = title,
    searchTitle = searchText(title),
    ownerId = owner.id,
    isPrivate = isPrivate,
    coverUrl = coverUrl,
    lastActivityAt = lastActivityAt
)
