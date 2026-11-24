package com.nhajc.android.data.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Relation
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class PlaylistInfo(
    val id: Uuid,
    val title: String,
    val searchTitle: String,
    val ownerId: Uuid,
    val isPrivate: Boolean,
    val coverUrl: String?,
    val lastActivityAt: Instant?
)

@Entity(
    tableName = "Playlists",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ownerId"])]
)
data class PlaylistEntity(
    @Embedded val info: PlaylistInfo,
    val downloadedAt: Instant? = null,
    @ColumnInfo(defaultValue = "0") val isRemoteRemoved: Boolean = false
)

@Entity(
    tableName = "PlaylistSongs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId", "position"], unique = true),
        Index(value = ["songId"])
    ]
)
data class PlaylistSongCrossRef(val playlistId: Uuid, val songId: Uuid, val position: Int)

data class PlaylistWithOwner(
    @Embedded val playlist: PlaylistEntity,
    @Relation(parentColumns = ["ownerId"], entityColumns = ["id"])
    val owner: UserEntity
)
