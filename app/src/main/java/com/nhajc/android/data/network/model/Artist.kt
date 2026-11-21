package com.nhajc.android.data.network.model

import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistRequest(val name: String, val avatarDataUri: String?)

@Serializable
@SerialName("ARTIST")
data class ArtistSummaryResponse(val id: Uuid, val name: String, val avatarUrl: String?) :
    SearchItemResponse

@Serializable
data class ArtistResponse(
    val id: Uuid,
    val name: String,
    val avatarUrl: String?,
    val isFollowed: Boolean,
    val releases: List<ReleaseSummaryResponse>
)
