package com.nhajc.android.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.nhajc.android.data.database.model.ArtistEntity
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
interface ArtistDao {
    @Upsert
    suspend fun upsert(artists: List<ArtistEntity>)

    @Query("UPDATE Artists SET followedAt = NULL")
    suspend fun clearFollowedAt()

    @Query("UPDATE Artists SET followedAt = :followedAt WHERE id = :id")
    suspend fun setFollowedAt(id: Uuid, followedAt: Instant?)
}
