package com.nhajc.android.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.nhajc.android.data.database.model.UserEntity

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(users: List<UserEntity>)
}
