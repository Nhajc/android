package com.nhajc.android.data.network.model

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistRequest(val title: String, val isPrivate: Boolean, val songIds: List<Uuid>)

@Serializable
@SerialName("PLAYLIST")
data class PlaylistSummaryResponse(
    val id: Uuid,
    val title: String,
    val coverUrl: String?,
    val isPrivate: Boolean,
    val owner: UserSummaryResponse,
    val songCount: Int,
    val lastActivityAt: Instant?
) : SearchItemResponse,
    LibraryItemResponse

@Serializable
data class PlaylistResponse(
    val id: Uuid,
    val title: String,
    val coverUrl: String?,
    val isPrivate: Boolean,
    val owner: UserSummaryResponse,
    val songCount: Int,
    val lastActivityAt: Instant?,
    val songs: List<SongResponse>
)
