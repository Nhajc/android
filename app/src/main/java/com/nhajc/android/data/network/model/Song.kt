package com.nhajc.android.data.network.model

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadSongRequest(val trackNumber: Int?, val title: String)

@Serializable
data class ReorderSongsRequest(val songIds: List<Uuid>)

@Serializable
data class StagedSongResponse(val id: Uuid, val durationSeconds: Int, val expiresAt: Instant)
@Serializable
data class UploadedSongDetails(val id: Uuid, val title: String, val description: String?, val artistIds: List<Uuid>)
@Serializable
data class SaveUploadedSongsRequest(val songs: List<UploadedSongDetails>, val songIds: List<Uuid>)

@Serializable
data class SongSummaryResponse(
    val id: Uuid,
    val title: String,
    val durationSeconds: Int,
    val trackNumber: Int,
    val artists: List<ArtistSummaryResponse>,
    val favoritedAt: Instant?,
    val description: String? = null
)

@Serializable
@SerialName("SONG")
data class SongResponse(
    val id: Uuid,
    val title: String,
    val durationSeconds: Int,
    val trackNumber: Int,
    val artists: List<ArtistSummaryResponse>,
    val favoritedAt: Instant?,
    val releaseId: Uuid,
    val releaseTitle: String,
    val coverUrl: String?
) : SearchItemResponse
