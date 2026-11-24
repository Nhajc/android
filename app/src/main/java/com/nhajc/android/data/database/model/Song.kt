package com.nhajc.android.data.database.model

import androidx.room3.ColumnInfo
import androidx.room3.DatabaseView
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Junction
import androidx.room3.Relation
import com.nhajc.android.data.model.Song
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class SongInfo(
    val id: Uuid,
    val releaseId: Uuid,
    val releaseTitle: String,
    val coverUrl: String?,
    val title: String,
    val searchTitle: String,
    val trackNumber: Int,
    val durationSeconds: Int,
    val favoritedAt: Instant?
)

@Entity(
    tableName = "Songs",
    primaryKeys = ["id"],
    indices = [Index(value = ["releaseId"])]
)
data class SongEntity(
    @Embedded val info: SongInfo,
    @ColumnInfo(
        defaultValue = "INHERIT"
    ) val downloadChoice: DownloadChoice = DownloadChoice.INHERIT,
    val fileQuality: String? = null,
    val downloadedAt: Instant? = null
)

enum class DownloadChoice { INHERIT, ALWAYS, NEVER }

@Entity(
    tableName = "SongArtists",
    primaryKeys = ["songId", "artistId"],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
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
        Index(value = ["songId", "position"], unique = true),
        Index(value = ["artistId"])
    ]
)
data class SongArtistCrossRef(val songId: Uuid, val artistId: Uuid, val position: Int)

data class SongWithArtists(
    @Embedded val song: SongEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["id"],
        associateBy = Junction(
            SongArtistCrossRef::class,
            parentColumns = ["songId"],
            entityColumns = ["artistId"]
        )
    )
    val artists: List<ArtistEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["songId"])
    val credits: List<SongArtistCrossRef>
)

@DatabaseView(
    viewName = "WantedSongs",
    value = """
        SELECT id FROM Songs WHERE downloadChoice = 'ALWAYS'
        UNION
        SELECT s.id FROM Songs s
        JOIN Releases r ON r.id = s.releaseId
        WHERE r.downloadedAt IS NOT NULL AND s.downloadChoice != 'NEVER'
        UNION
        SELECT s.id FROM Songs s
        JOIN PlaylistSongs ps ON ps.songId = s.id
        JOIN Playlists p ON p.id = ps.playlistId
        WHERE p.downloadedAt IS NOT NULL AND s.downloadChoice != 'NEVER'
    """
)
data class WantedSongView(val id: Uuid)

fun SongWithArtists.asModel(): Song {
    val positions = credits.associate { it.artistId to it.position }
    val sortedArtists = artists.sortedBy { positions[it.id] }

    return Song(
        id = song.info.id,
        title = song.info.title,
        trackNumber = song.info.trackNumber,
        durationSeconds = song.info.durationSeconds,
        artists = sortedArtists.map { it.asModel() },
        releaseId = song.info.releaseId,
        releaseTitle = song.info.releaseTitle,
        coverUrl = song.info.coverUrl,
        isFavorite = song.info.favoritedAt != null
    )
}
