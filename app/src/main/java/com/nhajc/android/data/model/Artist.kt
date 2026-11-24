package com.nhajc.android.data.model

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

data class ArtistSummary(val id: Uuid, val name: String, val avatarUrl: String?)

data class Artist(
    val id: Uuid,
    val name: String,
    val avatarUrl: String?,
    val isFollowed: Boolean,
    val releases: List<ArtistRelease>
)

data class ArtistRelease(
    val id: Uuid,
    val title: String,
    val coverUrl: String?,
    val releaseType: ReleaseType,
    val releaseDate: LocalDate,
    val songCount: Int
)

enum class ArtistError {
    NETWORK,
    SESSION_ENDED,
    NOT_FOUND,
    FORBIDDEN,
    INVALID_RESPONSE,
    SERVER
}

class ArtistException(val error: ArtistError, cause: Throwable? = null) :
    Exception(error.name, cause)
