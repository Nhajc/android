package com.nhajc.android.data.network.model

import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SongSummaryResponse(
    val id: Uuid,
    val title: String,
    val durationSeconds: Int,
    val trackNumber: Int,
    val artists: List<ArtistSummaryResponse>,
    val isFavorited: Boolean
)

@Serializable
@SerialName("SONG")
data class SongResponse(
    val id: Uuid,
    val title: String,
    val durationSeconds: Int,
    val trackNumber: Int,
    val artists: List<ArtistSummaryResponse>,
    val isFavorited: Boolean,
    val releaseId: Uuid,
    val releaseTitle: String,
    val coverUrl: String?
) : SearchItemResponse
