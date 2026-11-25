package com.nhajc.android.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.nhajc.android.data.database.model.DownloadChoice
import com.nhajc.android.data.database.model.SongArtistCrossRef
import com.nhajc.android.data.database.model.SongEntity
import com.nhajc.android.data.database.model.SongFile
import com.nhajc.android.data.database.model.SongInfo
import com.nhajc.android.data.database.model.SongWithArtists
import com.nhajc.android.data.model.AudioQuality
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Upsert(entity = SongEntity::class)
    suspend fun upsertFromServer(songs: List<SongInfo>)

    @Insert(entity = SongEntity::class, onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(songs: List<SongInfo>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtistsIfMissing(artists: List<SongArtistCrossRef>)

    @Transaction
    @Query("SELECT * FROM Songs WHERE id = :id")
    fun observe(id: Uuid): Flow<SongWithArtists?>

    @Query("UPDATE Songs SET favoritedAt = NULL")
    suspend fun clearFavoritedAt()

    @Query("UPDATE Songs SET favoritedAt = :favoritedAt WHERE id = :id")
    suspend fun setFavoritedAt(id: Uuid, favoritedAt: Instant?)

    @Query("UPDATE Songs SET downloadChoice = :downloadChoice WHERE id = :id")
    suspend fun setDownloadChoice(id: Uuid, downloadChoice: DownloadChoice)

    @Query(
        """
        UPDATE Songs SET downloadChoice = :to
        WHERE releaseId = :releaseId AND downloadChoice = :from
        """
    )
    suspend fun replaceReleaseChoices(releaseId: Uuid, from: DownloadChoice, to: DownloadChoice)

    @Query(
        """
        UPDATE Songs SET downloadChoice = :to
        WHERE downloadChoice = :from
          AND id IN (SELECT songId FROM PlaylistSongs WHERE playlistId = :playlistId)
        """
    )
    suspend fun replacePlaylistChoices(playlistId: Uuid, from: DownloadChoice, to: DownloadChoice)

    @Query(
        """
        SELECT id, fileQuality FROM Songs
        WHERE id IN (SELECT id FROM WantedSongs)
          AND (fileQuality IS NULL OR fileQuality != :quality OR downloadedAt IS NULL)
        """
    )
    fun observeSongsToDownload(quality: AudioQuality): Flow<List<SongFile>>

    @Query(
        """
        SELECT id, fileQuality FROM Songs
        WHERE fileQuality IS NOT NULL AND id NOT IN (SELECT id FROM WantedSongs)
        """
    )
    fun observeSongsToRemove(): Flow<List<SongFile>>

    @Query("SELECT fileQuality FROM Songs WHERE id = :id")
    suspend fun fileQuality(id: Uuid): AudioQuality?

    @Query("UPDATE Songs SET fileQuality = :quality, downloadedAt = NULL WHERE id = :id")
    suspend fun setFileStarted(id: Uuid, quality: AudioQuality)

    @Query("UPDATE Songs SET fileQuality = :quality, downloadedAt = :downloadedAt WHERE id = :id")
    suspend fun setFileDownloaded(id: Uuid, quality: AudioQuality, downloadedAt: Instant)

    @Query(
        """
        UPDATE Songs SET fileQuality = NULL
        WHERE id = :id AND fileQuality = :quality AND downloadedAt IS NULL
        """
    )
    suspend fun clearUnfinishedFile(id: Uuid, quality: AudioQuality)

    @Query("UPDATE Songs SET fileQuality = NULL, downloadedAt = NULL WHERE id = :id")
    suspend fun clearFile(id: Uuid)

    @Query("DELETE FROM SongArtists WHERE songId = :songId")
    suspend fun deleteArtists(songId: Uuid)

    @Insert
    suspend fun insertArtists(artists: List<SongArtistCrossRef>)

    @Transaction
    suspend fun replaceArtists(songId: Uuid, artists: List<SongArtistCrossRef>) {
        deleteArtists(songId)
        insertArtists(artists)
    }
}
