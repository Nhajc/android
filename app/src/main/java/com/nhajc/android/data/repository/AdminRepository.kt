package com.nhajc.android.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.nhajc.android.data.model.*
import com.nhajc.android.data.network.*
import com.nhajc.android.data.network.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.HttpException

@Singleton
class AdminRepository @Inject constructor(
    private val admin: AdminApi, private val artists: ArtistApi,
    private val genres: GenreApi, private val releases: ReleaseApi,
    @ApplicationContext private val context: Context
) {
    suspend fun findArtists(query: String, page: Int = 0): AdminList<AdminArtist> = adminCall {
        val response = admin.listArtists(query.trim().takeIf { it.isNotEmpty() }, page, 20)
        AdminList(response.content.map { it.admin() }, response.page, response.hasNext)
    }
    suspend fun findReleases(query: String, page: Int = 0): AdminList<AdminRelease> = adminCall {
        val response = admin.listReleases(query.trim().takeIf { it.isNotEmpty() }, page, 20)
        AdminList(response.content.map { it.admin() }, response.page, response.hasNext)
    }
    suspend fun getGenres(page: Int = 0): AdminList<AdminGenre> = adminCall {
        val response = genres.getGenres(page, 100)
        AdminList(response.content.map { AdminGenre(it.id, it.name) }, response.page, response.hasNext)
    }
    suspend fun getArtist(id: Uuid): AdminArtistDetail = adminCall {
        val result = artists.getArtist(id)
        AdminArtistDetail(AdminArtist(result.id, result.name, result.avatarUrl), result.releases.map { it.admin() })
    }
    suspend fun getRelease(id: Uuid): AdminReleaseDetail = adminCall {
        val result = releases.getRelease(id)
        AdminReleaseDetail(AdminRelease(result.id, result.title, result.coverUrl, result.releaseType,
            result.releaseDate.toString(), result.artists.map { it.name }, result.songCount),
            result.songs.map { AdminSong(it.title, it.trackNumber, it.durationSeconds, it.id, it.description, it.artists.map { a -> a.admin() }) },
            result.artists.map { it.admin() })
    }
    suspend fun stageSong(id: Uuid, file: AdminFile, progress: (Float?) -> Unit, processing: () -> Unit): StagedSongResponse = adminCall {
        admin.stageSong(id, file.part(AUDIO_LIMIT, progress, processing))
    }
    suspend fun saveUploads(id: Uuid, uploads: List<UploadQueueItem>, order: List<Uuid>): Unit = adminCall(AdminError.ORDER_CHANGED) {
        admin.saveUploads(id, SaveUploadedSongsRequest(uploads.map {
            UploadedSongDetails(requireNotNull(it.uploadedId), it.title.trim(), it.description.trim().takeIf(String::isNotEmpty), it.artists.map { a -> a.id })
        }, order))
    }
    suspend fun discardUpload(id: Uuid, uploadId: Uuid): Unit = adminCall { admin.discardUpload(id, uploadId) }
    suspend fun reorderSongs(id: Uuid, ids: List<Uuid>): Unit = adminCall(AdminError.ORDER_CHANGED) {
        admin.reorderSongs(id, ReorderSongsRequest(ids))
    }
    suspend fun createArtist(draft: ArtistDraft): AdminArtistDetail = adminCall {
        AdminValidation.name(draft.name)?.let { throw AdminException(it) }
        val id = admin.createArtist(ArtistRequest(draft.name.trim()), draft.image?.part(IMAGE_LIMIT)).id
        // Creation is already confirmed by HTTP 201. Reading the detail is a
        // separate action, so a later connection failure must not invite a duplicate POST.
        AdminArtistDetail(AdminArtist(id, draft.name.trim(), draft.image?.uri?.toString()), emptyList())
    }
    suspend fun createGenre(name: String): Unit = adminCall(AdminError.GENRE_EXISTS) {
        AdminValidation.name(name, 100)?.let { throw AdminException(it) }
        admin.createGenre(GenreRequest(name.trim()))
        Unit
    }
    suspend fun createRelease(draft: ReleaseDraft): AdminReleaseDetail = adminCall {
        AdminValidation.release(draft)?.let { throw AdminException(it) }
        val result = admin.createRelease(ReleaseRequest(draft.title.trim(), draft.type,
            LocalDate.parse(draft.date.trim()), draft.artists.map { it.id }, draft.genreIds.toList()), draft.image?.part(IMAGE_LIMIT))
        AdminReleaseDetail(AdminRelease(result.id, draft.title.trim(), draft.image?.uri?.toString(), draft.type,
            draft.date.trim(), draft.artists.map { it.name }, 0), emptyList(), draft.artists)
    }
    suspend fun uploadSong(id: Uuid, draft: SongDraft, onProgress: (Float?) -> Unit, onProcessing: () -> Unit): Unit =
        adminCall(AdminError.TRACK_EXISTS) {
            AdminValidation.song(draft, emptySet())?.let { throw AdminException(it) }
            admin.uploadSong(id, UploadSongRequest(draft.trackNumber.trim().toInt(), draft.title.trim()),
                requireNotNull(draft.audio).part(AUDIO_LIMIT, onProgress, onProcessing))
            Unit
        }

    suspend fun inspectFile(uri: Uri, image: Boolean): AdminFile = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            var name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { uri.scheme == "file" && it.isNotBlank() }
                ?: if (image) "image" else "audio.flac"
            var size: Long? = null
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) ?: name }
                    cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }?.let { size = cursor.getLong(it).takeIf { n -> n >= 0 } }
                }
            }
            val limit = if (image) IMAGE_LIMIT else AUDIO_LIMIT
            if ((size ?: 0) > limit) throw AdminException(if (image) AdminError.IMAGE_TOO_LARGE else AdminError.AUDIO_TOO_LARGE)
            val header = resolver.openInputStream(uri)?.use { input ->
                val bytes = ByteArray(8)
                var offset = 0
                while (offset < bytes.size) {
                    val read = input.read(bytes, offset, bytes.size - offset)
                    if (read < 0) break
                    offset += read
                }
                bytes.copyOf(offset)
            } ?: throw AdminException(AdminError.FILE_UNAVAILABLE)
            val mime = when {
                image && header.size >= 8 && header.contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) -> "image/png"
                image && header.size >= 3 && header[0] == 0xff.toByte() && header[1] == 0xd8.toByte() && header[2] == 0xff.toByte() -> "image/jpeg"
                !image && header.size >= 4 && header.take(4).toByteArray().contentEquals("fLaC".toByteArray()) -> "audio/flac"
                else -> throw AdminException(if (image) AdminError.INVALID_IMAGE else AdminError.INVALID_AUDIO)
            }
            AdminFile(uri, name, size, mime)
        } catch (exception: AdminException) { throw exception }
        catch (_: IOException) { throw AdminException(AdminError.FILE_UNAVAILABLE) }
        catch (_: SecurityException) { throw AdminException(AdminError.FILE_UNAVAILABLE) }
    }

    private fun AdminFile.part(limit: Long, progress: ((Float?) -> Unit)? = null, processing: (() -> Unit)? = null): MultipartBody.Part {
        val file = this
        val body = object : RequestBody() {
            override fun contentType() = file.mimeType.toMediaType()
            override fun contentLength() = file.size ?: -1
            override fun writeTo(sink: BufferedSink) {
                val input = try { context.contentResolver.openInputStream(file.uri) }
                    catch (_: SecurityException) { throw IOException("Selected file is no longer readable") }
                if (input == null) throw IOException("Selected file is no longer readable")
                input.use {
                    val buffer = ByteArray(64 * 1024)
                    var sent = 0L
                    var lastPercent = -1
                    while (true) {
                        val count = it.read(buffer)
                        if (count < 0) break
                        sent += count
                        if (sent > limit) throw IOException("Selected file exceeds upload limit")
                        sink.write(buffer, 0, count)
                        val percent = file.size?.takeIf { n -> n > 0 }?.let { n -> (sent * 100 / n).toInt().coerceAtMost(100) }
                        if (percent != lastPercent) {
                            progress?.invoke(percent?.div(100f))
                            lastPercent = percent ?: -1
                        }
                    }
                }
                processing?.invoke()
            }
        }
        return MultipartBody.Part.createFormData("file", name.replace('"', '_').replace('\r', '_').replace('\n', '_'), body)
    }
    companion object {
        const val IMAGE_LIMIT = 5L * 1024 * 1024
        const val AUDIO_LIMIT = 200L * 1024 * 1024
    }
}

internal suspend fun <T> adminCall(conflict: AdminError = AdminError.CONFLICT, block: suspend () -> T): T = try {
    block()
} catch (exception: CancellationException) { throw exception }
catch (exception: AdminException) { throw exception }
catch (exception: HttpException) {
    throw AdminException(when (exception.code()) {
        400 -> AdminError.INVALID_INPUT
        401 -> AdminError.UNAUTHORIZED
        403 -> AdminError.FORBIDDEN
        404 -> AdminError.NOT_FOUND
        409 -> conflict
        413 -> AdminError.TOO_LARGE
        422 -> AdminError.UNSUPPORTED_AUDIO
        429 -> AdminError.BUSY
        503 -> AdminError.UNAVAILABLE
        else -> AdminError.SERVER
    })
} catch (_: SerializationException) { throw AdminException(AdminError.SERVER) }
catch (_: IOException) { throw AdminException(AdminError.NETWORK) }

private fun ArtistSummaryResponse.admin() = AdminArtist(id, name, avatarUrl)
private fun ReleaseSummaryResponse.admin() = AdminRelease(id, title, coverUrl, releaseType,
    releaseDate.toString(), artists.map { it.name }, songCount)
