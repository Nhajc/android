package com.nhajc.android.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.nhajc.android.data.database.model.PlaylistEntity
import com.nhajc.android.data.database.model.PlaylistInfo
import com.nhajc.android.data.database.model.PlaylistSongCrossRef
import com.nhajc.android.data.database.model.PlaylistWithOwner
import com.nhajc.android.data.database.model.SongWithArtists
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM Playlists WHERE id = :id")
    fun observe(id: Uuid): Flow<PlaylistWithOwner?>

    @Transaction
    @Query(
        """
        SELECT s.* FROM Songs s
        JOIN PlaylistSongs ps ON ps.songId = s.id
        WHERE ps.playlistId = :id
        ORDER BY ps.position
        """
    )
    fun observeSongs(id: Uuid): Flow<List<SongWithArtists>>

    @Transaction
    @Query(
        """
        SELECT * FROM Playlists
        WHERE ownerId = :ownerId AND isRemoteRemoved = 0
        ORDER BY lastActivityAt DESC, id
        """
    )
    fun observeOwned(ownerId: Uuid): Flow<List<PlaylistWithOwner>>

    @Upsert(entity = PlaylistEntity::class)
    suspend fun upsertFromServer(playlists: List<PlaylistInfo>)

    @Query("UPDATE Playlists SET lastActivityAt = NULL")
    suspend fun clearLastActivityAt()

    @Query("UPDATE Playlists SET lastActivityAt = :lastActivityAt WHERE id = :id")
    suspend fun setLastActivityAt(id: Uuid, lastActivityAt: Instant?)

    @Query("UPDATE Playlists SET downloadedAt = :downloadedAt WHERE id = :id")
    suspend fun setDownloadedAt(id: Uuid, downloadedAt: Instant?)

    @Query(
        """
        UPDATE Playlists SET downloadedAt = NULL
        WHERE lastActivityAt IS NULL AND isRemoteRemoved = 0
        """
    )
    suspend fun clearUnsavedDownloads()

    @Query("UPDATE Playlists SET isRemoteRemoved = :isRemoteRemoved WHERE id = :id")
    suspend fun setRemoteRemoved(id: Uuid, isRemoteRemoved: Boolean)

    @Query("DELETE FROM Playlists WHERE id = :id")
    suspend fun delete(id: Uuid)

    @Query("DELETE FROM PlaylistSongs WHERE playlistId = :playlistId")
    suspend fun deleteSongs(playlistId: Uuid)

    @Insert
    suspend fun insertSongs(songs: List<PlaylistSongCrossRef>)

    @Query(
        "SELECT COALESCE(MAX(position) + 1, 0) FROM PlaylistSongs WHERE playlistId = :playlistId"
    )
    suspend fun nextPosition(playlistId: Uuid): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongIfMissing(song: PlaylistSongCrossRef)

    @Transaction
    suspend fun appendSong(playlistId: Uuid, songId: Uuid) {
        insertSongIfMissing(PlaylistSongCrossRef(playlistId, songId, nextPosition(playlistId)))
    }

    @Transaction
    suspend fun replaceSongs(playlistId: Uuid, songs: List<PlaylistSongCrossRef>) {
        deleteSongs(playlistId)
        insertSongs(songs)
    }
}
