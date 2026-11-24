package com.nhajc.android.ui.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhajc.android.data.model.*
import com.nhajc.android.data.repository.AdminRepository
import com.nhajc.android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminField { ARTIST_NAME, GENRE_NAME, RELEASE_TITLE, RELEASE_DATE }
sealed interface AdminEvent {
    data class Navigate(val page: AdminPage) : AdminEvent
    data class Query(val value: String, val picker: Boolean = false) : AdminEvent
    data class Field(val field: AdminField, val value: String) : AdminEvent
    data class Type(val value: ReleaseType) : AdminEvent
    data class ToggleGenre(val id: Uuid) : AdminEvent
    data class AddArtist(val artist: AdminArtist) : AdminEvent
    data class RemoveArtist(val id: Uuid) : AdminEvent
    data class MoveArtist(val id: Uuid, val offset: Int) : AdminEvent
    data class OpenArtist(val id: Uuid) : AdminEvent
    data class OpenRelease(val id: Uuid) : AdminEvent
    data class FileSelected(val uri: Uri, val image: Boolean) : AdminEvent
    data class AudioFilesSelected(val uris: List<Uri>) : AdminEvent
    data class QueueTitle(val id: Uuid, val title: String) : AdminEvent
    data class RemoveQueued(val id: Uuid) : AdminEvent
    data class RetryQueued(val id: Uuid) : AdminEvent
    data class MoveSong(val id: Uuid, val targetId: Uuid) : AdminEvent
    data class QueueDescription(val id: Uuid, val value: String) : AdminEvent
    data class EditUpload(val id: Uuid?) : AdminEvent
    data class AddSongArtist(val artist: AdminArtist) : AdminEvent
    data class RemoveSongArtist(val id: Uuid) : AdminEvent
    data class MoveSongArtist(val id: Uuid, val offset: Int) : AdminEvent
    data object ReviewUploads : AdminEvent
    data object SaveUploads : AdminEvent
    data object DiscardUploads : AdminEvent
    data object KeepUploads : AdminEvent
    data object StartQueue : AdminEvent
    data object BeginReorder : AdminEvent
    data object CancelReorder : AdminEvent
    data object SaveOrder : AdminEvent
    data object RemoveFile : AdminEvent
    data object Submit : AdminEvent
    data object More : AdminEvent
    data object MoreGenres : AdminEvent
    data object MoreArtists : AdminEvent
    data object Retry : AdminEvent
}

@HiltViewModel
class AdminViewModel @Inject constructor(private val repository: AdminRepository, users: UserRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminUiState())
    val state = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var pickerJob: Job? = null
    private var detailJob: Job? = null
    private var genreJob: Job? = null
    private var operation: Job? = null
    private var actor: Uuid? = null
    private var releaseParent = AdminPage.RELEASES

    init {
        viewModelScope.launch {
            users.user.collect { user ->
                val allowed = user?.role == UserRole.ADMIN
                if (actor != user?.id || state.value.isAdmin != allowed) {
                    searchJob?.cancel(); pickerJob?.cancel(); detailJob?.cancel(); genreJob?.cancel(); operation?.cancel()
                    mutableState.value = AdminUiState(checkingAccess = false, isAdmin = allowed)
                    actor = user?.id
                } else mutableState.update { it.copy(checkingAccess = false) }
            }
        }
    }

    fun handle(event: AdminEvent) {
        if (!state.value.isAdmin || state.value.busy) return
        when (event) {
            is AdminEvent.Navigate -> navigate(event.page)
            is AdminEvent.AudioFilesSelected -> selectAudioFiles(event.uris)
            is AdminEvent.QueueTitle -> mutableState.update { state -> state.copy(uploadQueue = state.uploadQueue.map {
                if (it.id == event.id && it.status in listOf(UploadStatus.WAITING, UploadStatus.DONE)) it.copy(title = event.title) else it
            }) }
            is AdminEvent.RemoveQueued -> mutableState.update { it.copy(uploadQueue = it.uploadQueue.filterNot { item -> item.id == event.id }) }
            is AdminEvent.RetryQueued -> mutableState.update { it.copy(uploadQueue = it.uploadQueue.map { item ->
                if (item.id == event.id && item.status == UploadStatus.FAILED) item.copy(status = UploadStatus.WAITING, error = null, progress = null) else item
            }) }
            AdminEvent.StartQueue -> startQueue()
            is AdminEvent.QueueDescription -> editUpload { if (it.id == event.id) it.copy(description = event.value) else it }
            is AdminEvent.EditUpload -> { mutableState.update { it.copy(editingUpload = event.id, pickerQuery = "", pickerArtists = emptyList()) }; if (event.id != null) search("", true, 0) }
            is AdminEvent.AddSongArtist -> editUpload { if (it.artists.size < 20 && it.artists.none { a -> a.id == event.artist.id }) it.copy(artists = it.artists + event.artist) else it }
            is AdminEvent.RemoveSongArtist -> editUpload { it.copy(artists = it.artists.filterNot { a -> a.id == event.id }) }
            is AdminEvent.MoveSongArtist -> editUpload { item ->
                val credits = item.artists.toMutableList()
                val from = credits.indexOfFirst { it.id == event.id }; val to = from + event.offset
                if (from in credits.indices && to in credits.indices) credits.add(to, credits.removeAt(from))
                item.copy(artists = credits)
            }
            AdminEvent.ReviewUploads -> reviewUploads()
            AdminEvent.SaveUploads -> saveUploads()
            AdminEvent.DiscardUploads -> discardUploads()
            AdminEvent.KeepUploads -> mutableState.update { it.copy(confirmDiscard = false) }
            AdminEvent.BeginReorder -> mutableState.update { it.copy(songOrder = it.release?.songs?.map { song -> song.id }, saved = false, error = null) }
            AdminEvent.CancelReorder -> mutableState.update { it.copy(songOrder = null, error = null) }
            is AdminEvent.MoveSong -> mutableState.update { state ->
                val reviewing = state.page == AdminPage.REVIEW_UPLOADS
                val order = (if (reviewing) state.reviewOrder else state.songOrder)?.toMutableList() ?: return@update state
                val from = order.indexOf(event.id)
                val to = order.indexOf(event.targetId)
                if (from >= 0 && to >= 0 && from != to) order.add(to, order.removeAt(from))
                if (reviewing) state.copy(reviewOrder = order) else state.copy(songOrder = order)
            }
            AdminEvent.SaveOrder -> saveOrder()
            is AdminEvent.Query -> query(event.value, event.picker)
            is AdminEvent.Field -> mutableState.update { s ->
                when (event.field) {
                    AdminField.ARTIST_NAME -> s.copy(artistDraft = s.artistDraft.copy(name = event.value), error = null)
                    AdminField.GENRE_NAME -> s.copy(genreName = event.value, error = null)
                    AdminField.RELEASE_TITLE -> s.copy(releaseDraft = s.releaseDraft.copy(title = event.value), error = null)
                    AdminField.RELEASE_DATE -> s.copy(releaseDraft = s.releaseDraft.copy(date = event.value), error = null)
                }
            }
            is AdminEvent.Type -> mutableState.update { it.copy(releaseDraft = it.releaseDraft.copy(type = event.value), error = null) }
            is AdminEvent.ToggleGenre -> mutableState.update {
                val selected = it.releaseDraft.genreIds
                val ids = if (event.id in selected) selected - event.id else selected + event.id
                if (ids.size > 20) it.copy(error = AdminError.TOO_MANY_SELECTIONS)
                else it.copy(releaseDraft = it.releaseDraft.copy(genreIds = ids), error = null)
            }
            is AdminEvent.AddArtist -> mutableState.update {
                val selected = it.releaseDraft.artists
                when {
                    selected.any { artist -> artist.id == event.artist.id } -> it
                    selected.size == 20 -> it.copy(error = AdminError.TOO_MANY_SELECTIONS)
                    else -> it.copy(releaseDraft = it.releaseDraft.copy(artists = selected + event.artist), error = null)
                }
            }
            is AdminEvent.RemoveArtist -> mutableState.update {
                it.copy(releaseDraft = it.releaseDraft.copy(artists = it.releaseDraft.artists.filterNot { a -> a.id == event.id }), error = null)
            }
            is AdminEvent.MoveArtist -> mutableState.update {
                val selected = it.releaseDraft.artists.toMutableList()
                val from = selected.indexOfFirst { a -> a.id == event.id }
                val to = from + event.offset
                if (from in selected.indices && to in selected.indices) {
                    val artist = selected.removeAt(from); selected.add(to, artist)
                }
                it.copy(releaseDraft = it.releaseDraft.copy(artists = selected))
            }
            is AdminEvent.OpenArtist -> openArtist(event.id)
            is AdminEvent.OpenRelease -> openRelease(event.id)
            is AdminEvent.FileSelected -> runOperation {
                val file = repository.inspectFile(event.uri, event.image)
                mutableState.update {
                    when (it.page) {
                        AdminPage.NEW_ARTIST -> it.copy(artistDraft = it.artistDraft.copy(image = file))
                        AdminPage.NEW_RELEASE -> it.copy(releaseDraft = it.releaseDraft.copy(image = file))
                        else -> it
                    }
                }
            }
            AdminEvent.RemoveFile -> mutableState.update {
                it.copy(artistDraft = it.artistDraft.copy(image = null), releaseDraft = it.releaseDraft.copy(image = null), error = null)
            }
            AdminEvent.Submit -> submit()
            AdminEvent.More -> search(state.value.query, false, state.value.listPage + 1)
            AdminEvent.MoreArtists -> search(state.value.pickerQuery, true, state.value.pickerPage + 1)
            AdminEvent.MoreGenres -> loadGenres(state.value.genrePage + 1)
            AdminEvent.Retry -> when (state.value.page) {
                AdminPage.GENRES -> loadGenres(0)
                AdminPage.NEW_RELEASE -> {
                    loadGenres(0)
                    search(state.value.pickerQuery, true, 0)
                }
                AdminPage.ARTIST -> detailId?.let(::openArtist)
                AdminPage.RELEASE -> detailId?.let(::openRelease)
                AdminPage.REVIEW_UPLOADS -> refreshReview()
                else -> search(state.value.query, false, 0)
            }
        }
    }

    fun back(): Boolean {
        val s = state.value
        if (s.busy) return true
        if (s.page == AdminPage.REVIEW_UPLOADS) {
            mutableState.update { it.copy(page = AdminPage.BATCH_UPLOAD, editingUpload = null) }; return true
        }
        if (s.page == AdminPage.BATCH_UPLOAD && s.uploadQueue.any { it.uploadedId != null }) {
            mutableState.update { it.copy(confirmDiscard = true) }; return true
        }
        if (s.songOrder != null) {
            mutableState.update { it.copy(songOrder = null, error = null) }
            return true
        }
        if (s.page == AdminPage.HOME || !s.isAdmin) return false
        detailJob?.cancel(); pickerJob?.cancel(); searchJob?.cancel(); genreJob?.cancel()
        val page = when (s.page) {
            AdminPage.NEW_ARTIST, AdminPage.ARTIST -> AdminPage.ARTISTS
            AdminPage.NEW_GENRE -> AdminPage.GENRES
            AdminPage.NEW_RELEASE -> AdminPage.RELEASES
            AdminPage.RELEASE -> releaseParent
            AdminPage.BATCH_UPLOAD, AdminPage.REVIEW_UPLOADS -> AdminPage.RELEASE
            else -> AdminPage.HOME
        }
        mutableState.update { it.copy(page = page, error = null, saved = false, loading = false, loadingPicker = false, loadingGenres = false, songOrder = null) }
        if (page == AdminPage.RELEASE) state.value.release?.release?.id?.let(::openRelease)
        if (page == AdminPage.ARTISTS || page == AdminPage.RELEASES) search(state.value.query, false, 0)
        return true
    }

    private fun navigate(page: AdminPage) {
        searchJob?.cancel(); pickerJob?.cancel(); detailJob?.cancel()
        mutableState.update { s -> s.copy(page = page, error = null, saved = false, loading = false, songOrder = null,
            uploadQueue = if (page == AdminPage.BATCH_UPLOAD) emptyList() else s.uploadQueue,
            artistDraft = if (page == AdminPage.NEW_ARTIST) ArtistDraft() else s.artistDraft,
            genreName = if (page == AdminPage.NEW_GENRE) "" else s.genreName,
            releaseDraft = if (page == AdminPage.NEW_RELEASE) ReleaseDraft() else s.releaseDraft,
            pickerQuery = "", pickerArtists = emptyList(), loadingPicker = false,
            query = if (page == AdminPage.ARTISTS || page == AdminPage.RELEASES) "" else s.query,
            artists = if (page == AdminPage.ARTISTS) emptyList() else s.artists,
            releases = if (page == AdminPage.RELEASES) emptyList() else s.releases,
            hasNext = false,
            reviewOrder = if (page == AdminPage.BATCH_UPLOAD) emptyList() else s.reviewOrder,
            editingUpload = null, confirmDiscard = false
        ) }
        if (page == AdminPage.GENRES || page == AdminPage.NEW_RELEASE) loadGenres(0)
        if (page == AdminPage.ARTISTS || page == AdminPage.RELEASES) search("", false, 0)
        if (page == AdminPage.NEW_RELEASE) search("", true, 0)
    }

    private fun query(value: String, picker: Boolean) {
        if (picker) mutableState.update { it.copy(pickerQuery = value, pickerArtists = emptyList(), morePickerArtists = false, error = null) }
        else mutableState.update { it.copy(query = value, artists = emptyList(), releases = emptyList(), hasNext = false, error = null) }
        search(value, picker, 0, 350)
    }

    private fun search(input: String, picker: Boolean, page: Int, wait: Long = 0) {
        val keyword = input.trim()
        if (picker) pickerJob?.cancel() else searchJob?.cancel()
        if (keyword.length > 100) {
            mutableState.update { it.copy(loading = false, loadingPicker = false,
                error = if (keyword.length > 100) AdminError.INVALID_INPUT else null) }
            return
        }
        val artists = picker || state.value.page == AdminPage.ARTISTS
        if (!picker && state.value.page !in listOf(AdminPage.ARTISTS, AdminPage.RELEASES)) return
        val job = viewModelScope.launch {
            mutableState.update { if (picker) it.copy(loadingPicker = true, error = null) else it.copy(loading = true, error = null) }
            try {
                delay(wait)
                if (artists) {
                    val result = repository.findArtists(keyword, page)
                    currentCoroutineContext().ensureActive()
                    mutableState.update {
                        if (picker) it.copy(pickerArtists = ((if (page == 0) emptyList() else it.pickerArtists) + result.items).distinctBy { a -> a.id },
                            pickerPage = result.page, morePickerArtists = result.hasNext)
                        else it.copy(artists = ((if (page == 0) emptyList() else it.artists) + result.items).distinctBy { a -> a.id }, listPage = result.page, hasNext = result.hasNext)
                    }
                } else {
                    val result = repository.findReleases(keyword, page)
                    currentCoroutineContext().ensureActive()
                    mutableState.update { it.copy(releases = ((if (page == 0) emptyList() else it.releases) + result.items).distinctBy { r -> r.id },
                        listPage = result.page, hasNext = result.hasNext) }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(error = e.adminError()) } }
            finally { if (currentCoroutineContext().isActive) mutableState.update { if (picker) it.copy(loadingPicker = false) else it.copy(loading = false) } }
        }
        if (picker) pickerJob = job else searchJob = job
    }

    private fun loadGenres(page: Int) {
        genreJob?.cancel()
        genreJob = viewModelScope.launch {
            mutableState.update { it.copy(loadingGenres = true, error = null) }
            try {
                val result = repository.getGenres(page)
                mutableState.update { it.copy(genres = ((if (page == 0) emptyList() else it.genres) + result.items).distinctBy { g -> g.id },
                    genrePage = result.page, moreGenres = result.hasNext) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(error = e.adminError()) } }
            finally { if (currentCoroutineContext().isActive) mutableState.update { it.copy(loadingGenres = false) } }
        }
    }

    private var detailId: Uuid? = null
    private fun openArtist(id: Uuid) {
        detailId = id
        searchJob?.cancel()
        detailJob?.cancel()
        mutableState.update { it.copy(page = AdminPage.ARTIST, artist = null, loading = true, error = null, saved = false) }
        detailJob = viewModelScope.launch {
            try { val result = repository.getArtist(id); mutableState.update { it.copy(artist = result) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(error = e.adminError()) } }
            finally { if (currentCoroutineContext().isActive) mutableState.update { it.copy(loading = false) } }
        }
    }
    private fun openRelease(id: Uuid) {
        if (state.value.page !in listOf(AdminPage.RELEASE, AdminPage.REVIEW_UPLOADS, AdminPage.BATCH_UPLOAD)) releaseParent = if (state.value.page == AdminPage.ARTIST) AdminPage.ARTIST else AdminPage.RELEASES
        detailId = id
        searchJob?.cancel()
        detailJob?.cancel()
        mutableState.update { it.copy(page = AdminPage.RELEASE, release = null, loading = true, error = null, saved = false, songOrder = null) }
        detailJob = viewModelScope.launch {
            try { val result = repository.getRelease(id); mutableState.update { it.copy(release = result) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(error = e.adminError()) } }
            finally { if (currentCoroutineContext().isActive) mutableState.update { it.copy(loading = false) } }
        }
    }

    private fun submit() {
        val snapshot = state.value
        val invalid = when (snapshot.page) {
            AdminPage.NEW_ARTIST -> AdminValidation.name(snapshot.artistDraft.name)
            AdminPage.NEW_GENRE -> AdminValidation.name(snapshot.genreName, 100)
            AdminPage.NEW_RELEASE -> AdminValidation.release(snapshot.releaseDraft)
            else -> return
        }
        if (invalid != null) { mutableState.update { it.copy(error = invalid) }; return }
        runOperation {
            when (snapshot.page) {
                AdminPage.NEW_ARTIST -> {
                    val result = repository.createArtist(snapshot.artistDraft)
                    detailId = result.artist.id
                    mutableState.update { it.copy(page = AdminPage.ARTIST, artist = result, artistDraft = ArtistDraft(), saved = true) }
                }
                AdminPage.NEW_GENRE -> {
                    repository.createGenre(snapshot.genreName)
                    mutableState.update { it.copy(page = AdminPage.GENRES, genreName = "", saved = true) }
                    loadGenres(0)
                }
                AdminPage.NEW_RELEASE -> {
                    val result = repository.createRelease(snapshot.releaseDraft)
                    detailId = result.release.id
                    releaseParent = AdminPage.RELEASES
                    mutableState.update { it.copy(page = AdminPage.RELEASE, release = result, releaseDraft = ReleaseDraft(), saved = true) }
                }
                else -> Unit
            }
        }
    }

    private fun runOperation(action: suspend () -> Unit) {
        if (state.value.busy || !state.value.isAdmin) return
        mutableState.update { it.copy(busy = true, error = null, saved = false, uploadProgress = 0f, processingAudio = false) }
        operation = viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(error = e.adminError()) } }
            finally { mutableState.update { it.copy(busy = false, processingAudio = false, uploadProgress = null) } }
        }
    }

    private fun selectAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty() || state.value.page != AdminPage.BATCH_UPLOAD) return
        runOperation {
            val existing = state.value.uploadQueue.map { it.file.uri }.toSet()
            val selected = mutableListOf<UploadQueueItem>()
            var error: AdminError? = null
            for (uri in uris.distinct().filterNot { it in existing }) {
                try {
                    val file = repository.inspectFile(uri, false)
                    val title = file.name.substringBeforeLast('.').trim().ifBlank { file.name }.take(255)
                    selected += UploadQueueItem(file = file, title = title, artists = state.value.release?.artistCredits.orEmpty())
                } catch (exception: CancellationException) { throw exception }
                catch (exception: Exception) { error = exception.adminError() }
            }
            mutableState.update { it.copy(uploadQueue = it.uploadQueue + selected, error = error) }
        }
    }

    private fun startQueue() {
        val snapshot = state.value
        val releaseId = snapshot.release?.release?.id ?: return
        val waiting = snapshot.uploadQueue.filter { it.status == UploadStatus.WAITING }
        if (waiting.isEmpty()) return
        if (waiting.any { AdminValidation.name(it.title) != null }) {
            mutableState.update { it.copy(error = AdminError.INVALID_INPUT) }; return
        }
        mutableState.update { it.copy(queueRunning = true) }
        runOperation {
            try {
                UploadQueueRunner().run(waiting, { item, progress, processing ->
                    repository.stageSong(releaseId, item.file, progress, processing)
                }, { item -> mutableState.update { state -> state.copy(uploadQueue = state.uploadQueue.map { if (it.id == item.id) item else it }) } },
                    { item, prepared -> item.copy(status = UploadStatus.DONE, progress = 1f, uploadedId = prepared.id, durationSeconds = prepared.durationSeconds) })
                // Refresh the baseline before reviewing; staged audio is not in this list.
                try {
                    val refreshed = repository.getRelease(releaseId)
                    mutableState.update { it.copy(release = refreshed) }
                    if (state.value.uploadQueue.all { it.status == UploadStatus.DONE }) reviewUploads()
                } catch (exception: CancellationException) { throw exception }
                catch (exception: Exception) { mutableState.update { it.copy(error = exception.adminError()) } }
            } finally { mutableState.update { it.copy(queueRunning = false) } }
        }
    }

    private fun editUpload(change: (UploadQueueItem) -> UploadQueueItem) {
        mutableState.update { state -> state.copy(uploadQueue = state.uploadQueue.map {
            if (it.id == state.editingUpload) change(it) else it
        }, error = null) }
    }

    private fun reviewUploads() {
        val snapshot = state.value
        val uploaded = snapshot.uploadQueue.filter { it.uploadedId != null }
        if (uploaded.isEmpty()) return
        val ids = snapshot.release?.songs.orEmpty().map { it.id } + uploaded.map { requireNotNull(it.uploadedId) }
        val order = snapshot.reviewOrder.filter { it in ids } + ids.filterNot { it in snapshot.reviewOrder }
        mutableState.update { it.copy(page = AdminPage.REVIEW_UPLOADS, reviewOrder = order,
            editingUpload = uploaded.first().id, error = null) }
        search("", true, 0)
    }

    private fun saveUploads() {
        val snapshot = state.value
        val releaseId = snapshot.release?.release?.id ?: return
        val uploaded = snapshot.uploadQueue.filter { it.uploadedId != null }
        if (uploaded.isEmpty() || uploaded.any { AdminValidation.name(it.title) != null || it.description.length > 4000 || it.artists.isEmpty() || it.artists.size > 20 }) {
            mutableState.update { it.copy(error = AdminError.INVALID_INPUT) }; return
        }
        runOperation {
            repository.saveUploads(releaseId, uploaded, snapshot.reviewOrder)
            // Clear consumed IDs before refreshing so a lost GET never repeats publication.
            mutableState.update { it.copy(page = AdminPage.RELEASE, uploadQueue = emptyList(), reviewOrder = emptyList(), editingUpload = null, saved = true) }
            val refreshed = repository.getRelease(releaseId)
            mutableState.update { it.copy(release = refreshed) }
        }
    }

    private fun refreshReview() {
        val id = state.value.release?.release?.id ?: return
        runOperation {
            val refreshed = repository.getRelease(id)
            val uploadedIds = state.value.uploadQueue.mapNotNull { it.uploadedId }
            if (uploadedIds.isNotEmpty() && refreshed.songs.map { it.id }.containsAll(uploadedIds)) {
                mutableState.update { it.copy(page = AdminPage.RELEASE, release = refreshed, uploadQueue = emptyList(), reviewOrder = emptyList(), editingUpload = null, saved = true) }
            } else {
                mutableState.update { it.copy(release = refreshed) }
                reviewUploads()
            }
        }
    }

    private fun discardUploads() {
        val snapshot = state.value
        val releaseId = snapshot.release?.release?.id ?: return
        runOperation {
            snapshot.uploadQueue.mapNotNull { it.uploadedId }.forEach { repository.discardUpload(releaseId, it) }
            mutableState.update { it.copy(page = AdminPage.RELEASE, uploadQueue = emptyList(), reviewOrder = emptyList(), editingUpload = null, confirmDiscard = false) }
        }
    }

    private fun saveOrder() {
        val snapshot = state.value
        val ids = snapshot.songOrder ?: return
        val release = snapshot.release ?: return
        runOperation {
            repository.reorderSongs(release.release.id, ids)
            val byId = release.songs.associateBy { it.id }
            mutableState.update { it.copy(songOrder = null, saved = true,
                release = release.copy(songs = ids.mapIndexed { index, id -> requireNotNull(byId[id]).copy(trackNumber = index + 1) })) }
        }
    }
}

private fun Exception.adminError() = (this as? AdminException)?.error ?: AdminError.SERVER
