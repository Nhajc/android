package com.nhajc.android.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nhajc.android.data.model.Song
import com.nhajc.android.data.repository.QueueRepository
import com.nhajc.android.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext context: Context,
    private val queueRepository: QueueRepository,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val mutablePlayer = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = mutablePlayer.asStateFlow()

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()

        future.addListener(
            { onConnected(future.get()) },
            ContextCompat.getMainExecutor(context)
        )
    }

    suspend fun play(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) {
            return
        }

        val player = player.filterNotNull().first()
        val quality = settingsRepository.audioQuality.first()

        queueRepository.replace(songs)

        val items = queueRepository.songs().map { it.asMediaItem(quality) }

        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        player.play()
    }

    suspend fun shuffle(songs: List<Song>) {
        if (songs.isEmpty()) {
            return
        }

        val player = player.filterNotNull().first()

        player.shuffleModeEnabled = true
        play(songs, songs.indices.random())
    }

    private fun onConnected(controller: MediaController) {
        mutablePlayer.value = controller

        if (controller.mediaItemCount == 0) {
            scope.launch { restore(controller) }
        }
    }

    private suspend fun restore(player: Player) {
        val saved = queueRepository.saved()

        if (saved.songs.isEmpty()) {
            return
        }

        val quality = settingsRepository.audioQuality.first()
        val items = saved.songs.map { it.asMediaItem(quality) }

        player.setMediaItems(items, saved.position.index, saved.position.positionMs)
    }
}
