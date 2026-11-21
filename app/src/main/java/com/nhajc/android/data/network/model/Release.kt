package com.nhajc.android.data.network.model

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ReleaseType { ALBUM, SINGLE, EP, COMPILATION, UNKNOWN }

@Serializable
data class ReleaseRequest(
    val title: String,
    val releaseType: ReleaseType,
    val releaseDate: LocalDate,
    val artistIds: List<Uuid>,
    val genreIds: List<Uuid>,
    val coverDataUri: String?
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
    val isSaved: Boolean
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
    val isSaved: Boolean,
    val songs: List<SongSummaryResponse>
)
