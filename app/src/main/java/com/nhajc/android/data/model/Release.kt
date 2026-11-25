package com.nhajc.android.data.model

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

data class Release(
    val id: Uuid,
    val title: String,
    val releaseType: ReleaseType,
    val releaseDate: LocalDate,
    val coverUrl: String?,
    val artists: List<ArtistSummary>,
    val isSaved: Boolean
)

enum class ReleaseType { ALBUM, SINGLE, EP, COMPILATION, UNKNOWN }
