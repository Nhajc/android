package com.nhajc.android.data.network.model

import kotlinx.serialization.Serializable

enum class LibrarySort { RECENT, NAME, ARTIST }

@Serializable
sealed interface LibraryItemResponse
