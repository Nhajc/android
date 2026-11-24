package com.nhajc.android.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import androidx.room3.withWriteTransaction
import com.nhajc.android.data.database.AppDatabase
import com.nhajc.android.data.database.model.ArtistEntity
import com.nhajc.android.data.database.model.PlaylistInfo
import com.nhajc.android.data.database.model.ReleaseArtistCrossRef
import com.nhajc.android.data.database.model.ReleaseInfo
import com.nhajc.android.data.database.model.SongArtistCrossRef
import com.nhajc.android.data.database.model.SongInfo
import com.nhajc.android.data.database.model.UserEntity
import com.nhajc.android.data.database.model.asModel
import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.model.ArtistSort
import com.nhajc.android.data.model.ArtistSummary
import com.nhajc.android.data.model.DownloadSort
import com.nhajc.android.data.model.LibrarySort
import com.nhajc.android.data.model.Playlist
import com.nhajc.android.data.model.Release
import com.nhajc.android.data.model.Song
import com.nhajc.android.data.network.LibraryApi
import com.nhajc.android.data.network.PlaylistApi
import com.nhajc.android.data.network.model.ArtistSummaryResponse
import com.nhajc.android.data.network.model.PageResponse
import com.nhajc.android.data.network.model.PlaylistRequest
import com.nhajc.android.data.network.model.PlaylistResponse
import com.nhajc.android.data.network.model.PlaylistSummaryResponse
import com.nhajc.android.data.network.model.ReleaseSummaryResponse
import com.nhajc.android.data.network.model.SongResponse
import com.nhajc.android.data.network.model.UserSummaryResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.apache.commons.lang3.StringUtils
import retrofit2.HttpException

private const val PAGE_SIZE = 50
private const val SYNC_PAGE_SIZE = 100
private const val MIN_QUERY_LENGTH = 3

@Singleton
class LibraryRepository @Inject constructor(
    private val libraryApi: LibraryApi,
    private val playlistApi: PlaylistApi,
    private val database: AppDatabase
) {
    private val libraryDao = database.libraryDao()
    private val userDao = database.userDao()
    private val artistDao = database.artistDao()
    private val releaseDao = database.releaseDao()
    private val playlistDao = database.playlistDao()
    private val songDao = database.songDao()

    fun releases(query: String, sort: LibrarySort): Flow<PagingData<Release>> =
        pager { libraryDao.releases(searchKey(query), sort) }
            .map { page -> page.map { it.asModel() } }

    fun playlists(query: String, sort: LibrarySort): Flow<PagingData<Playlist>> =
        pager { libraryDao.playlists(searchKey(query), sort) }
            .map { page -> page.map { it.asModel() } }

    fun artists(query: String, sort: ArtistSort): Flow<PagingData<ArtistSummary>> =
        pager { libraryDao.artists(searchKey(query), sort) }
            .map { page -> page.map { it.asModel() } }

    fun songs(query: String, sort: LibrarySort): Flow<PagingData<Song>> =
        pager { libraryDao.songs(searchKey(query), sort) }
            .map { page -> page.map { it.asModel() } }

    fun downloads(query: String, sort: DownloadSort): Flow<PagingData<Song>> =
        pager { libraryDao.downloads(searchKey(query), sort) }
            .map { page -> page.map { it.asModel() } }

    suspend fun favoriteList(query: String, sort: LibrarySort): List<Song> =
        libraryDao.songList(searchKey(query), sort).map { it.asModel() }

    suspend fun downloadList(query: String, sort: DownloadSort): List<Song> =
        libraryDao.downloadList(searchKey(query), sort).map { it.asModel() }

    fun song(id: Uuid): Flow<Song?> = songDao.observe(id).map { it?.asModel() }

    suspend fun setFavorite(songId: Uuid, isFavorite: Boolean) {
        if (isFavorite) {
            call { libraryApi.favoriteSong(songId) }
        } else {
            call { libraryApi.unfavoriteSong(songId) }
        }

        val favoritedAt = if (isFavorite) {
            Clock.System.now()
        } else {
            null
        }

        songDao.setFavoritedAt(songId, favoritedAt)
    }

    suspend fun createPlaylist(title: String) {
        val request = PlaylistRequest(title = title, isPrivate = false, songIds = emptyList())
        val playlist = call { playlistApi.createPlaylist(request, null) }

        database.withWriteTransaction {
            userDao.upsert(listOf(playlist.owner.asEntity()))
            playlistDao.upsertFromServer(listOf(playlist.asInfo()))
        }
    }

    suspend fun sync() {
        val releases = fetchAll { page -> libraryApi.getReleases(page, SYNC_PAGE_SIZE) }
        val playlists = fetchAll { page -> libraryApi.getPlaylists(page, SYNC_PAGE_SIZE) }
        val artists = fetchAll { page -> libraryApi.getArtists(page, SYNC_PAGE_SIZE) }
        val songs = fetchAll { page -> libraryApi.getSongs(page, SYNC_PAGE_SIZE) }

        val artistEntities = mutableMapOf<Uuid, ArtistEntity>()

        for (artist in artists) {
            artistEntities[artist.id] = artist.asEntity()
        }

        for (release in releases) {
            for (artist in release.artists) {
                artistEntities.putIfAbsent(artist.id, artist.asEntity())
            }
        }

        for (song in songs) {
            for (artist in song.artists) {
                artistEntities.putIfAbsent(artist.id, artist.asEntity())
            }
        }

        database.withWriteTransaction {
            releaseDao.clearLastActivityAt()
            playlistDao.clearLastActivityAt()
            artistDao.clearFollowedAt()
            songDao.clearFavoritedAt()

            userDao.upsert(playlists.map { it.owner.asEntity() })
            artistDao.upsert(artistEntities.values.toList())
            releaseDao.upsertFromServer(releases.map { it.asInfo() })
            playlistDao.upsertFromServer(playlists.map { it.asInfo() })
            songDao.upsertFromServer(songs.map { it.asInfo() })

            for (release in releases) {
                val credits = release.artists.mapIndexed { position, artist ->
                    ReleaseArtistCrossRef(release.id, artist.id, position)
                }

                releaseDao.replaceArtists(release.id, credits)
            }

            for (song in songs) {
                val credits = song.artists.mapIndexed { position, artist ->
                    SongArtistCrossRef(song.id, artist.id, position)
                }

                songDao.replaceArtists(song.id, credits)
            }

            libraryDao.deleteUnused()
        }
    }

    private fun <T : Any> pager(source: () -> PagingSource<Int, T>): Flow<PagingData<T>> =
        Pager(PagingConfig(pageSize = PAGE_SIZE), pagingSourceFactory = source).flow

    private suspend fun <T> fetchAll(load: suspend (page: Int) -> PageResponse<T>): List<T> {
        val items = mutableListOf<T>()
        var page = 0

        do {
            val response = call { load(page) }

            items += response.content
            page++
        } while (response.hasNext)

        return items
    }

    private suspend fun <T> call(block: suspend () -> T): T {
        try {
            return block()
        } catch (exception: HttpException) {
            val error = when (exception.code()) {
                400 -> AppError.INVALID_INPUT
                401 -> AppError.SESSION_ENDED
                409 -> AppError.PLAYLIST_LIMIT
                else -> AppError.SERVER
            }

            throw AppException(error, exception)
        } catch (exception: IOException) {
            throw AppException(AppError.NETWORK, exception)
        }
    }
}

private fun searchKey(query: String): String? {
    val trimmed = query.trim()

    if (trimmed.length < MIN_QUERY_LENGTH) {
        return null
    }

    return searchText(trimmed)
}

private fun searchText(text: String): String = StringUtils.stripAccents(text).lowercase()

private fun UserSummaryResponse.asEntity() =
    UserEntity(id = id, username = username, avatarUrl = avatarUrl)

private fun ArtistSummaryResponse.asEntity() = ArtistEntity(
    id = id,
    name = name,
    searchName = searchText(name),
    avatarUrl = avatarUrl,
    followedAt = followedAt
)

private fun ReleaseSummaryResponse.asInfo() = ReleaseInfo(
    id = id,
    title = title,
    searchTitle = searchText(title),
    releaseType = releaseType,
    releaseDate = releaseDate,
    coverUrl = coverUrl,
    lastActivityAt = lastActivityAt
)

private fun PlaylistSummaryResponse.asInfo() = PlaylistInfo(
    id = id,
    title = title,
    searchTitle = searchText(title),
    ownerId = owner.id,
    isPrivate = isPrivate,
    coverUrl = coverUrl,
    lastActivityAt = lastActivityAt
)

private fun PlaylistResponse.asInfo() = PlaylistInfo(
    id = id,
    title = title,
    searchTitle = searchText(title),
    ownerId = owner.id,
    isPrivate = isPrivate,
    coverUrl = coverUrl,
    lastActivityAt = lastActivityAt
)

private fun SongResponse.asInfo() = SongInfo(
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
