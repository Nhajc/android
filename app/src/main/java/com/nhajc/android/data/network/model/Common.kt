package com.nhajc.android.data.network.model

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class PageResponse<T>(val content: List<T>, val hasNext: Boolean)

@Serializable
data class IdResponse(val id: Uuid)

@Serializable
data class ProblemDetail(
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null
)
