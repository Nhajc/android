package com.nhajc.android.data.network.model

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class CommentRequest(val content: String)

@Serializable
data class CommentResponse(
    val id: Uuid,
    val content: String,
    val commentedAt: Instant,
    val user: UserSummaryResponse,
    val replyCount: Int
)
