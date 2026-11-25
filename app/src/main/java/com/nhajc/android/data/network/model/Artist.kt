package com.nhajc.android.data.network.model

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistRequest(val name: String)

@Serializable
@SerialName("ARTIST")
data class ArtistSummaryResponse(val id: Uuid, val name: String, val avatarUrl: String?, val followedAt: Instant?) :
    SearchItemResponse

@Serializable
data class ArtistResponse(
    val id: Uuid,
    val name: String,
    val avatarUrl: String?,
    val followedAt: Instant?,
    val releases: List<ReleaseSummaryResponse>
)
