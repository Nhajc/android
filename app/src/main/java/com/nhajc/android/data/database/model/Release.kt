package com.nhajc.android.data.database.model

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import com.nhajc.android.data.model.ReleaseType
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

data class ReleaseInfo(
    val id: Uuid,
    val title: String,
    val searchTitle: String,
    val releaseType: ReleaseType,
    val releaseDate: LocalDate,
    val coverUrl: String?,
    val lastActivityAt: Instant?
)

@Entity(tableName = "Releases", primaryKeys = ["id"])
data class ReleaseEntity(@Embedded val info: ReleaseInfo, val downloadedAt: Instant? = null)

@Entity(
    tableName = "ReleaseArtists",
    primaryKeys = ["releaseId", "artistId"],
    foreignKeys = [
        ForeignKey(
            entity = ReleaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["releaseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["releaseId", "position"], unique = true),
        Index(value = ["artistId"])
    ]
)
data class ReleaseArtistCrossRef(val releaseId: Uuid, val artistId: Uuid, val position: Int)
