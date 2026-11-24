package com.nhajc.android.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import com.nhajc.android.data.database.model.QueueItemEntity

@Dao
interface QueueDao {
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
