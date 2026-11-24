package com.nhajc.android.data.model

import android.net.Uri
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

enum class AdminPage { HOME, ARTISTS, ARTIST, GENRES, RELEASES, NEW_ARTIST, NEW_GENRE, NEW_RELEASE, RELEASE, BATCH_UPLOAD, REVIEW_UPLOADS }
enum class AdminError {
    INVALID_INPUT, INVALID_DATE, ARTISTS_REQUIRED, TOO_MANY_SELECTIONS, INVALID_TRACK, TRACK_EXISTS,
    GENRE_EXISTS, INVALID_IMAGE, INVALID_AUDIO, IMAGE_TOO_LARGE, AUDIO_TOO_LARGE, FILE_UNAVAILABLE,
    NETWORK, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, TOO_LARGE, UNSUPPORTED_AUDIO, BUSY, UNAVAILABLE, SERVER, ORDER_CHANGED
}
data class AdminException(val error: AdminError) : Exception(error.name)
data class AdminFile(val uri: Uri, val name: String, val size: Long?, val mimeType: String)
data class AdminArtist(val id: Uuid, val name: String, val avatarUrl: String?)
data class AdminGenre(val id: Uuid, val name: String)
data class AdminRelease(val id: Uuid, val title: String, val coverUrl: String?, val type: ReleaseType,
    val date: String, val artists: List<String>, val songCount: Int)
data class AdminSong(val title: String, val trackNumber: Int, val durationSeconds: Int, val id: Uuid,
    val description: String? = null, val artists: List<AdminArtist> = emptyList())
data class AdminReleaseDetail(val release: AdminRelease, val songs: List<AdminSong>, val artistCredits: List<AdminArtist> = emptyList())
data class AdminArtistDetail(val artist: AdminArtist, val releases: List<AdminRelease>)
data class AdminList<T>(val items: List<T>, val page: Int, val hasNext: Boolean)
data class ArtistDraft(val name: String = "", val image: AdminFile? = null)
data class ReleaseDraft(val title: String = "", val type: ReleaseType = ReleaseType.ALBUM,
    val date: String = "", val artists: List<AdminArtist> = emptyList(),
    val genreIds: Set<Uuid> = emptySet(), val image: AdminFile? = null)
data class SongDraft(val title: String = "", val trackNumber: String = "", val audio: AdminFile? = null, val autoTrack: Boolean = true)
data class AdminUiState(
    val checkingAccess: Boolean = true, val isAdmin: Boolean = false, val page: AdminPage = AdminPage.HOME,
    val query: String = "", val artists: List<AdminArtist> = emptyList(), val releases: List<AdminRelease> = emptyList(),
    val listPage: Int = 0, val hasNext: Boolean = false, val loading: Boolean = false,
    val genres: List<AdminGenre> = emptyList(), val genrePage: Int = -1, val moreGenres: Boolean = false,
    val loadingGenres: Boolean = false, val pickerQuery: String = "", val pickerArtists: List<AdminArtist> = emptyList(),
    val pickerPage: Int = 0, val morePickerArtists: Boolean = false, val loadingPicker: Boolean = false,
    val artist: AdminArtistDetail? = null, val release: AdminReleaseDetail? = null,
    val artistDraft: ArtistDraft = ArtistDraft(), val genreName: String = "",
    val releaseDraft: ReleaseDraft = ReleaseDraft(), val songDraft: SongDraft = SongDraft(),
    val busy: Boolean = false, val uploadProgress: Float? = null, val processingAudio: Boolean = false,
    val error: AdminError? = null, val saved: Boolean = false,
    val uploadQueue: List<UploadQueueItem> = emptyList(), val queueRunning: Boolean = false,
    val songOrder: List<Uuid>? = null, val reviewOrder: List<Uuid> = emptyList(),
    val editingUpload: Uuid? = null, val confirmDiscard: Boolean = false
)

enum class UploadStatus { WAITING, UPLOADING, PROCESSING, DONE, FAILED }
data class UploadQueueItem(
    val id: Uuid = Uuid.random(), val file: AdminFile, val title: String,
    val status: UploadStatus = UploadStatus.WAITING, val progress: Float? = null,
    val error: AdminError? = null, val uploadedId: Uuid? = null, val durationSeconds: Int? = null,
    val description: String = "", val artists: List<AdminArtist> = emptyList()
)

object AdminValidation {
    fun name(value: String, max: Int = 255): AdminError? =
        if (value.trim().isEmpty() || value.trim().length > max) AdminError.INVALID_INPUT else null

    fun release(draft: ReleaseDraft): AdminError? {
        name(draft.title)?.let { return it }
        if (draft.type == ReleaseType.UNKNOWN) return AdminError.INVALID_INPUT
        try { LocalDate.parse(draft.date.trim()) } catch (_: IllegalArgumentException) { return AdminError.INVALID_DATE }
        if (draft.artists.isEmpty()) return AdminError.ARTISTS_REQUIRED
        if (draft.artists.size > 20 || draft.genreIds.size > 20) return AdminError.TOO_MANY_SELECTIONS
        return null
    }

    fun song(draft: SongDraft, takenTracks: Set<Int>): AdminError? {
        name(draft.title)?.let { return it }
        if (!draft.autoTrack) {
            val track = draft.trackNumber.trim().toIntOrNull() ?: return AdminError.INVALID_TRACK
            if (track < 1) return AdminError.INVALID_TRACK
            if (track in takenTracks) return AdminError.TRACK_EXISTS
        }
        if (draft.audio == null) return AdminError.INVALID_AUDIO
        return null
    }
}
