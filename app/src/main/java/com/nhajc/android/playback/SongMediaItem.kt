package com.nhajc.android.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.nhajc.android.BuildConfig
import com.nhajc.android.data.model.AudioQuality
import com.nhajc.android.data.model.Song

fun Song.asMediaItem(quality: AudioQuality): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artists.joinToString(", ") { it.name })
        .setAlbumTitle(releaseTitle)
        .setArtworkUri(coverUrl?.toUri())
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .build()

    val audioUrl = "${BuildConfig.BASE_URL}songs/$id/audio/${quality.name.lowercase()}"

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(audioUrl)
        .setMediaMetadata(metadata)
        .build()
}
