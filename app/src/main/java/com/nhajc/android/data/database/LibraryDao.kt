package com.nhajc.android.data.database

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.nhajc.android.data.database.model.ArtistEntity
import com.nhajc.android.data.database.model.PlaylistWithOwner
import com.nhajc.android.data.database.model.ReleaseWithArtists
import com.nhajc.android.data.database.model.SongWithArtists
import com.nhajc.android.data.model.ArtistSort
import com.nhajc.android.data.model.DownloadSort
import com.nhajc.android.data.model.LibrarySort
import kotlinx.coroutines.flow.Flow

private const val FAVORITES_QUERY = """
    SELECT * FROM Songs
    WHERE favoritedAt IS NOT NULL
      AND (:query IS NULL OR instr(searchTitle, :query) > 0)
      AND (:sort != 'DOWNLOADED' OR downloadedAt IS NOT NULL)
    ORDER BY
      CASE WHEN :sort = 'RECENT' THEN favoritedAt END DESC,
      CASE WHEN :sort = 'DOWNLOADED' THEN downloadedAt END DESC,
      CASE WHEN :sort = 'ARTIST' THEN (SELECT a.searchName FROM SongArtists sa JOIN Artists a ON a.id = sa.artistId WHERE sa.songId = Songs.id AND sa.position = 0) END,
      searchTitle,
      id
    """

private const val DOWNLOADS_QUERY = """
    SELECT * FROM Songs
    WHERE fileQuality IS NOT NULL
      AND (:query IS NULL OR instr(searchTitle, :query) > 0)
    ORDER BY
      CASE WHEN :sort = 'RECENT' THEN downloadedAt END DESC,
      CASE WHEN :sort = 'ARTIST' THEN (SELECT a.searchName FROM SongArtists sa JOIN Artists a ON a.id = sa.artistId WHERE sa.songId = Songs.id AND sa.position = 0) END,
      searchTitle,
      id
    """

@Dao
interface LibraryDao {
    @Transaction
    @Query(
        "SELECT * FROM Releases WHERE lastActivityAt IS NOT NULL ORDER BY lastActivityAt DESC, searchTitle, id LIMIT :limit"
    )
    fun homeReleases(limit: Int): Flow<List<ReleaseWithArtists>>

    @Transaction
    @Query(
        "SELECT * FROM Playlists WHERE lastActivityAt IS NOT NULL ORDER BY lastActivityAt DESC, searchTitle, id LIMIT :limit"
    )
    fun homePlaylists(limit: Int): Flow<List<PlaylistWithOwner>>

    @Query("SELECT * FROM Artists WHERE followedAt IS NOT NULL ORDER BY followedAt DESC, searchName, id LIMIT :limit")
    fun homeArtists(limit: Int): Flow<List<ArtistEntity>>

    @Transaction
    @Query("SELECT * FROM Songs WHERE favoritedAt IS NOT NULL ORDER BY favoritedAt DESC, searchTitle, id LIMIT :limit")
    fun homeFavorites(limit: Int): Flow<List<SongWithArtists>>

    @Transaction
    @Query(
        """
        SELECT * FROM Releases
        WHERE lastActivityAt IS NOT NULL
          AND (:query IS NULL OR instr(searchTitle, :query) > 0)
          AND (:sort != 'DOWNLOADED' OR downloadedAt IS NOT NULL)
        ORDER BY
          CASE WHEN :sort = 'RECENT' THEN lastActivityAt END DESC,
          CASE WHEN :sort = 'DOWNLOADED' THEN downloadedAt END DESC,
          CASE WHEN :sort = 'ARTIST' THEN (SELECT a.searchName FROM ReleaseArtists ra JOIN Artists a ON a.id = ra.artistId WHERE ra.releaseId = Releases.id AND ra.position = 0) END,
          searchTitle,
          id
        """
    )
    fun releases(query: String?, sort: LibrarySort): PagingSource<Int, ReleaseWithArtists>

    @Transaction
    @Query(
        """
        SELECT * FROM Playlists
        WHERE lastActivityAt IS NOT NULL
          AND (:query IS NULL OR instr(searchTitle, :query) > 0)
          AND (:sort != 'DOWNLOADED' OR downloadedAt IS NOT NULL)
        ORDER BY
          CASE WHEN :sort = 'RECENT' THEN lastActivityAt END DESC,
          CASE WHEN :sort = 'DOWNLOADED' THEN downloadedAt END DESC,
          CASE WHEN :sort = 'ARTIST' THEN (SELECT username FROM Users WHERE Users.id = Playlists.ownerId) END COLLATE NOCASE,
          searchTitle,
          id
        """
    )
    fun playlists(query: String?, sort: LibrarySort): PagingSource<Int, PlaylistWithOwner>

    @Query(
        """
        SELECT * FROM Artists
        WHERE followedAt IS NOT NULL
          AND (:query IS NULL OR instr(searchName, :query) > 0)
        ORDER BY
          CASE WHEN :sort = 'RECENT' THEN followedAt END DESC,
          searchName,
          id
        """
    )
    fun artists(query: String?, sort: ArtistSort): PagingSource<Int, ArtistEntity>

    @Transaction
    @Query(FAVORITES_QUERY)
    fun songs(query: String?, sort: LibrarySort): PagingSource<Int, SongWithArtists>

    @Transaction
    @Query(FAVORITES_QUERY)
    suspend fun songList(query: String?, sort: LibrarySort): List<SongWithArtists>

    @Transaction
    @Query(DOWNLOADS_QUERY)
    fun downloads(query: String?, sort: DownloadSort): PagingSource<Int, SongWithArtists>

    @Transaction
    @Query(DOWNLOADS_QUERY)
    suspend fun downloadList(query: String?, sort: DownloadSort): List<SongWithArtists>

    @Query("DELETE FROM Playlists WHERE lastActivityAt IS NULL AND downloadedAt IS NULL")
    suspend fun deleteUnusedPlaylists()

    @Query(
        """
        DELETE FROM Songs
        WHERE favoritedAt IS NULL
          AND fileQuality IS NULL
          AND downloadChoice = 'INHERIT'
          AND id NOT IN (SELECT songId FROM PlaylistSongs)
          AND id NOT IN (SELECT songId FROM QueueItems)
          AND releaseId NOT IN (
              SELECT id FROM Releases WHERE lastActivityAt IS NOT NULL OR downloadedAt IS NOT NULL
          )
        """
    )
    suspend fun deleteUnusedSongs()

    @Query(
        """
        DELETE FROM Releases
        WHERE lastActivityAt IS NULL
          AND downloadedAt IS NULL
          AND id NOT IN (SELECT releaseId FROM Songs)
        """
    )
    suspend fun deleteUnusedReleases()

    @Query(
        """
        DELETE FROM Artists
        WHERE followedAt IS NULL
          AND id NOT IN (SELECT artistId FROM ReleaseArtists)
          AND id NOT IN (SELECT artistId FROM SongArtists)
        """
    )
    suspend fun deleteUnusedArtists()

    @Query("DELETE FROM Users WHERE id NOT IN (SELECT ownerId FROM Playlists)")
    suspend fun deleteUnusedUsers()

    @Transaction
    suspend fun deleteUnused() {
        deleteUnusedPlaylists()
        deleteUnusedSongs()
        deleteUnusedReleases()
        deleteUnusedArtists()
        deleteUnusedUsers()
    }
}
