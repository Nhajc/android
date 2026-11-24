package com.nhajc.android.data.database.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(tableName = "Artists")
data class ArtistEntity(
    @PrimaryKey val id: Uuid,
    val name: String,
    val searchName: String,
    val avatarUrl: String?,
    val followedAt: Instant?
)
