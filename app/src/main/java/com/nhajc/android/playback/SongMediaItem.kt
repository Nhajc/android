package com.nhajc.android.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.nhajc.android.BuildConfig
import com.nhajc.android.data.model.AudioQuality
import com.nhajc.android.data.model.Song
import kotlin.uuid.Uuid

fun Song.asMediaItem(quality: AudioQuality): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artists.joinToString(", ") { it.name })
        .setAlbumTitle(releaseTitle)
        .setArtworkUri(coverUrl?.toUri())
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .build()

    val builder = MediaItem.Builder()
        .setMediaId(id.toString())
        .setMediaMetadata(metadata)

    val fileQuality = fileQuality

    if (isDownloaded && fileQuality != null) {
        builder.setUri(audioUrl(id, fileQuality))
        builder.setCustomCacheKey(downloadId(id, fileQuality))
    } else {
        builder.setUri(audioUrl(id, quality))
    }

    return builder.build()
}

fun audioUrl(songId: Uuid, quality: AudioQuality): String =
    "${BuildConfig.BASE_URL}songs/$songId/audio/${quality.name.lowercase()}"

fun downloadId(songId: Uuid, quality: AudioQuality): String = "$songId/${quality.name}"
