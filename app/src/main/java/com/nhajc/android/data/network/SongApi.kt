package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.IdResponse
import com.nhajc.android.data.network.model.UploadSongRequest
import kotlin.uuid.Uuid
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface SongApi {
    @Multipart
    @POST("admin/releases/{id}/songs")
    suspend fun uploadSong(
        @Path("id") releaseId: Uuid,
        @Part("data") request: UploadSongRequest,
        @Part audio: MultipartBody.Part
    ): IdResponse
}
