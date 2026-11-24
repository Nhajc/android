package com.nhajc.android.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface LibraryDao {
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
