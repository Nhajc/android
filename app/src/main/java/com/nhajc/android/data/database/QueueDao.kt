package com.nhajc.android.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import com.nhajc.android.data.database.model.QueueItemEntity
import com.nhajc.android.data.database.model.SongWithArtists
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Transaction
    @Query("SELECT s.* FROM Songs s JOIN QueueItems q ON q.songId = s.id ORDER BY q.position")
    fun observeSongs(): Flow<List<SongWithArtists>>

    @Query("DELETE FROM QueueItems")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(items: List<QueueItemEntity>)

    @Transaction
    suspend fun replace(items: List<QueueItemEntity>) {
        deleteAll()
        insert(items)
    }
}
