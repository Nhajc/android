package com.nhajc.android.data.database.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.uuid.Uuid

@Entity(tableName = "Users")
data class UserEntity(@PrimaryKey val id: Uuid, val username: String, val avatarUrl: String?)
