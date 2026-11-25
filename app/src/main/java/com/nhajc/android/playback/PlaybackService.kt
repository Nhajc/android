package com.nhajc.android.playback

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.nhajc.android.data.model.QueuePosition
import com.nhajc.android.data.repository.QueueRepository
import com.nhajc.android.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {
    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var downloadCache: Cache

    @Inject
    lateinit var queueRepository: QueueRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val dataSourceFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(okHttpClient))
            .setCacheWriteDataSinkFactory(null)

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(PositionSaver(player))

        session = MediaLibrarySession.Builder(this, player, SessionCallback()).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player

        if (player != null) {
            savePosition(player)
        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        val current = session

        if (current != null) {
            current.player.release()
            current.release()
        }

        session = null
        scope.cancel()

        super.onDestroy()
    }

    private fun savePosition(player: Player) {
        val position = QueuePosition(player.currentMediaItemIndex, player.currentPosition)

        scope.launch { queueRepository.savePosition(position) }
    }

    private inner class PositionSaver(private val player: Player) : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            savePosition(player)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) {
                savePosition(player)
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                savePosition(player)
            }
        }
    }

    private inner class SessionCallback : MediaLibrarySession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean
        ): ListenableFuture<MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaItemsWithStartPosition>()

            scope.launch {
                val saved = queueRepository.saved()
                val quality = settingsRepository.audioQuality.first()

                if (saved.songs.isEmpty()) {
                    future.setException(IllegalStateException("No saved queue"))
                } else {
                    val items = saved.songs.map { it.asMediaItem(quality) }

                    future.set(
                        MediaItemsWithStartPosition(
                            items,
                            saved.position.index,
                            saved.position.positionMs
                        )
                    )
                }
            }

            return future
        }
    }
}
