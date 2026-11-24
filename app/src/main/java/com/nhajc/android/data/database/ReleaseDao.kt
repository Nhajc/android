package com.nhajc.android.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.nhajc.android.data.database.model.ReleaseArtistCrossRef
import com.nhajc.android.data.database.model.ReleaseEntity
import com.nhajc.android.data.database.model.ReleaseInfo
import com.nhajc.android.data.database.model.ReleaseWithArtists
import com.nhajc.android.data.database.model.SongWithArtists
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDao {
    @Transaction
    @Query("SELECT * FROM Releases WHERE id = :id")
    fun observe(id: Uuid): Flow<ReleaseWithArtists?>

    @Transaction
    @Query("SELECT * FROM Songs WHERE releaseId = :id ORDER BY trackNumber, id")
    fun observeSongs(id: Uuid): Flow<List<SongWithArtists>>

    @Upsert(entity = ReleaseEntity::class)
    suspend fun upsertFromServer(releases: List<ReleaseInfo>)

    @Query("UPDATE Releases SET lastActivityAt = :lastActivityAt WHERE id = :id")
    suspend fun setLastActivityAt(id: Uuid, lastActivityAt: Instant?)

    @Query("UPDATE Releases SET downloadedAt = :downloadedAt WHERE id = :id")
    suspend fun setDownloadedAt(id: Uuid, downloadedAt: Instant?)

    @Query("DELETE FROM ReleaseArtists WHERE releaseId = :releaseId")
    suspend fun deleteArtists(releaseId: Uuid)

    @Insert
    suspend fun insertArtists(artists: List<ReleaseArtistCrossRef>)

    @Transaction
    suspend fun replaceArtists(releaseId: Uuid, artists: List<ReleaseArtistCrossRef>) {
        deleteArtists(releaseId)
        insertArtists(artists)
    }
}
