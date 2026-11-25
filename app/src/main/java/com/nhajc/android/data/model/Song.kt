package com.nhajc.android.data.model

import kotlin.uuid.Uuid

data class Song(
    val id: Uuid,
    val title: String,
    val trackNumber: Int,
    val durationSeconds: Int,
    val artists: List<ArtistSummary>,
    val releaseId: Uuid,
    val releaseTitle: String,
    val coverUrl: String?,
    val isFavorite: Boolean,
    val fileQuality: AudioQuality?,
    val isDownloaded: Boolean
)
