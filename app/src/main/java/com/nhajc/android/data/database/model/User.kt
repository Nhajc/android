package com.nhajc.android.data.database.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.nhajc.android.data.model.UserSummary
import kotlin.uuid.Uuid

@Entity(tableName = "Users")
data class UserEntity(@PrimaryKey val id: Uuid, val username: String, val avatarUrl: String?)

fun UserEntity.asModel(): UserSummary = UserSummary(id = id, username = username, avatarUrl = avatarUrl)
