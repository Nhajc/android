package com.nhajc.android.data.network.model

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class GenreRequest(val name: String)

@Serializable
data class GenreResponse(val id: Uuid, val name: String, val slug: String)
