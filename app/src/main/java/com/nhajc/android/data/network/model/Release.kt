package com.nhajc.android.data.network.model

import com.nhajc.android.data.model.ReleaseType
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReleaseRequest(
    val title: String,
    val releaseType: ReleaseType,
    val releaseDate: LocalDate,
    val artistIds: List<Uuid>,
    val genreIds: List<Uuid>
)

@Serializable
@SerialName("RELEASE")
data class ReleaseSummaryResponse(
    val id: Uuid,
    val title: String,
    val coverUrl: String?,
    val releaseType: ReleaseType = ReleaseType.UNKNOWN,
    val releaseDate: LocalDate,
    val songCount: Int,
    val artists: List<ArtistSummaryResponse>,
    val lastActivityAt: Instant?
) : SearchItemResponse,
    LibraryItemResponse

@Serializable
data class ReleaseResponse(
    val id: Uuid,
    val title: String,
    val coverUrl: String?,
    val releaseType: ReleaseType = ReleaseType.UNKNOWN,
    val releaseDate: LocalDate,
    val songCount: Int,
    val artists: List<ArtistSummaryResponse>,
    val lastActivityAt: Instant?,
    val songs: List<SongSummaryResponse>
)
