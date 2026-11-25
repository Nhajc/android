package com.nhajc.android.data.database.model

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Junction
import androidx.room3.Relation
import com.nhajc.android.data.model.Release
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

data class ReleaseWithArtists(
    @Embedded val release: ReleaseEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["id"],
        associateBy = Junction(
            ReleaseArtistCrossRef::class,
            parentColumns = ["releaseId"],
            entityColumns = ["artistId"]
        )
    )
    val artists: List<ArtistEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["releaseId"])
    val credits: List<ReleaseArtistCrossRef>
)

fun ReleaseWithArtists.asModel(): Release {
    val positions = credits.associate { it.artistId to it.position }
    val sortedArtists = artists.sortedBy { positions[it.id] }

    return Release(
        id = release.info.id,
        title = release.info.title,
        releaseType = release.info.releaseType,
        releaseDate = release.info.releaseDate,
        coverUrl = release.info.coverUrl,
        artists = sortedArtists.map { it.asModel() },
        isSaved = release.info.lastActivityAt != null
    )
}
