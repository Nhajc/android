package com.nhajc.android.data.repository

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.room3.withWriteTransaction
import com.nhajc.android.data.database.AppDatabase
import com.nhajc.android.data.database.model.DownloadChoice
import com.nhajc.android.data.database.model.SongFile
import com.nhajc.android.data.model.AudioQuality
import com.nhajc.android.download.SongDownloadService
import com.nhajc.android.playback.audioUrl
import com.nhajc.android.playback.downloadId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val PROGRESS_INTERVAL_MS = 500L

@OptIn(UnstableApi::class)
@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: DownloadManager,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val songDao = database.songDao()
    private val releaseDao = database.releaseDao()
    private val playlistDao = database.playlistDao()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val failedSongs = mutableSetOf<Uuid>()

    val progress: Flow<Map<Uuid, Float>> = flow {
        while (true) {
            emit(currentProgress())
            delay(PROGRESS_INTERVAL_MS)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.Main)

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        downloadManager.addListener(StateListener())

        scope.launch {
            settingsRepository.downloadQuality
                .flatMapLatest { quality ->
                    songDao.observeSongsToDownload(quality).map { songs -> quality to songs }
                }
                .distinctUntilChanged()
                .collect { (quality, songs) -> addDownloads(songs, quality) }
        }

        scope.launch {
            songDao.observeSongsToRemove()
                .distinctUntilChanged()
                .collect { songs -> removeDownloads(songs) }
        }
    }

    suspend fun setSongDownloaded(id: Uuid, isDownloaded: Boolean) {
        val choice = if (isDownloaded) {
            DownloadChoice.ALWAYS
        } else {
            DownloadChoice.NEVER
        }

        failedSongs.clear()
        songDao.setDownloadChoice(id, choice)
    }

    suspend fun setReleaseDownloaded(id: Uuid, isDownloaded: Boolean) {
        failedSongs.clear()

        database.withWriteTransaction {
            if (isDownloaded) {
                releaseDao.setDownloadedAt(id, Clock.System.now())
                songDao.replaceReleaseChoices(id, DownloadChoice.NEVER, DownloadChoice.INHERIT)
            } else {
                releaseDao.setDownloadedAt(id, null)
                songDao.replaceReleaseChoices(id, DownloadChoice.ALWAYS, DownloadChoice.INHERIT)
            }
        }
    }

    suspend fun setPlaylistDownloaded(id: Uuid, isDownloaded: Boolean) {
        failedSongs.clear()

        database.withWriteTransaction {
            if (isDownloaded) {
                playlistDao.setDownloadedAt(id, Clock.System.now())
                songDao.replacePlaylistChoices(id, DownloadChoice.NEVER, DownloadChoice.INHERIT)
            } else {
                playlistDao.setDownloadedAt(id, null)
                songDao.replacePlaylistChoices(id, DownloadChoice.ALWAYS, DownloadChoice.INHERIT)
            }
        }
    }

    private suspend fun addDownloads(songs: List<SongFile>, quality: AudioQuality) {
        val running = downloadManager.currentDownloads.map { it.request.id }.toSet()
        val missing = songs.filter { song ->
            song.id !in failedSongs && downloadId(song.id, quality) !in running
        }

        if (missing.isEmpty()) {
            return
        }

        database.withWriteTransaction {
            for (song in missing) {
                if (song.fileQuality == null) {
                    songDao.setFileStarted(song.id, quality)
                }
            }
        }

        for (song in missing) {
            val id = downloadId(song.id, quality)
            val request = DownloadRequest.Builder(id, audioUrl(song.id, quality).toUri())
                .setCustomCacheKey(id)
                .build()

            sendAdd(request)
        }
    }

    private suspend fun removeDownloads(songs: List<SongFile>) {
        if (songs.isEmpty()) {
            return
        }

        val ids = songs.map { it.id }.toSet()

        for (song in songs) {
            val fileQuality = song.fileQuality

            if (fileQuality != null) {
                sendRemove(downloadId(song.id, fileQuality))
            }
        }

        for (download in downloadManager.currentDownloads) {
            val songId = download.songId()

            if (songId in ids) {
                sendRemove(download.request.id)
            }
        }

        database.withWriteTransaction {
            for (id in ids) {
                songDao.clearFile(id)
            }
        }
    }

    private suspend fun onCompleted(songId: Uuid, quality: AudioQuality) {
        val oldQuality = songDao.fileQuality(songId) ?: return

        songDao.setFileDownloaded(songId, quality, Clock.System.now())

        if (oldQuality != quality) {
            sendRemove(downloadId(songId, oldQuality))
        }
    }

    private suspend fun onFailed(songId: Uuid, quality: AudioQuality) {
        failedSongs += songId

        sendRemove(downloadId(songId, quality))
        songDao.clearUnfinishedFile(songId, quality)
    }

    private fun sendAdd(request: DownloadRequest) {
        try {
            DownloadService.sendAddDownload(
                context,
                SongDownloadService::class.java,
                request,
                false
            )
        } catch (exception: IllegalStateException) {
            downloadManager.addDownload(request)
        }
    }

    private fun sendRemove(id: String) {
        try {
            DownloadService.sendRemoveDownload(
                context,
                SongDownloadService::class.java,
                id,
                false
            )
        } catch (exception: IllegalStateException) {
            downloadManager.removeDownload(id)
        }
    }

    private fun currentProgress(): Map<Uuid, Float> {
        val progress = mutableMapOf<Uuid, Float>()

        for (download in downloadManager.currentDownloads) {
            val songId = download.songId()

            if (songId == null || download.state == Download.STATE_REMOVING) {
                continue
            }

            progress[songId] = (download.percentDownloaded / 100f).coerceIn(0f, 1f)
        }

        return progress
    }

    private inner class StateListener : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            val songId = download.songId() ?: return
            val quality = download.quality() ?: return

            when (download.state) {
                Download.STATE_COMPLETED -> scope.launch { onCompleted(songId, quality) }
                Download.STATE_FAILED -> scope.launch { onFailed(songId, quality) }
            }
        }
    }
}

@OptIn(UnstableApi::class)
private fun Download.songId(): Uuid? = Uuid.parseOrNull(request.id.substringBefore('/'))

@OptIn(UnstableApi::class)
private fun Download.quality(): AudioQuality? =
    AudioQuality.entries.firstOrNull { it.name == request.id.substringAfter('/') }
