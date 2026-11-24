package com.nhajc.android.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.nhajc.android.data.database.model.PlaylistEntity
import com.nhajc.android.data.database.model.PlaylistInfo
import com.nhajc.android.data.database.model.PlaylistSongCrossRef
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
interface PlaylistDao {
    @Upsert(entity = PlaylistEntity::class)
    suspend fun upsertFromServer(playlists: List<PlaylistInfo>)

    @Query("UPDATE Playlists SET lastActivityAt = :lastActivityAt WHERE id = :id")
    suspend fun setLastActivityAt(id: Uuid, lastActivityAt: Instant?)

    @Query("UPDATE Playlists SET downloadedAt = :downloadedAt WHERE id = :id")
    suspend fun setDownloadedAt(id: Uuid, downloadedAt: Instant?)

    @Query("UPDATE Playlists SET isRemoteRemoved = :isRemoteRemoved WHERE id = :id")
    suspend fun setRemoteRemoved(id: Uuid, isRemoteRemoved: Boolean)

    @Query("DELETE FROM Playlists WHERE id = :id")
    suspend fun delete(id: Uuid)

    @Query("DELETE FROM PlaylistSongs WHERE playlistId = :playlistId")
    suspend fun deleteSongs(playlistId: Uuid)

    @Insert
    suspend fun insertSongs(songs: List<PlaylistSongCrossRef>)

    @Transaction
    suspend fun replaceSongs(playlistId: Uuid, songs: List<PlaylistSongCrossRef>) {
        deleteSongs(playlistId)
        insertSongs(songs)
    }
}
