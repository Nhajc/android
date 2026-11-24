package com.nhajc.android.data.model

import kotlin.uuid.Uuid

data class Playlist(
    val id: Uuid,
    val title: String,
    val coverUrl: String?,
    val isPrivate: Boolean,
    val owner: UserSummary
)
