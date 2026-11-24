package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.IdResponse
import com.nhajc.android.data.network.model.UploadSongRequest
import kotlin.uuid.Uuid
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Body
import com.nhajc.android.data.network.model.StagedSongResponse
import com.nhajc.android.data.network.model.SaveUploadedSongsRequest

interface SongApi {
    @Multipart
    @POST("admin/releases/{id}/song-uploads")
    suspend fun stageSong(@Path("id") releaseId: Uuid, @Part audio: MultipartBody.Part): StagedSongResponse
    @PUT("admin/releases/{id}/song-uploads")
    suspend fun saveUploads(@Path("id") releaseId: Uuid, @Body request: SaveUploadedSongsRequest)
    @DELETE("admin/releases/{id}/song-uploads/{uploadId}")
    suspend fun discardUpload(@Path("id") releaseId: Uuid, @Path("uploadId") uploadId: Uuid)
    @Multipart
    @POST("admin/releases/{id}/songs")
    suspend fun uploadSong(
        @Path("id") releaseId: Uuid,
        @Part("data") request: UploadSongRequest,
        @Part audio: MultipartBody.Part
    ): IdResponse
}
