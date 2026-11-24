package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.ArtistRequest
import com.nhajc.android.data.network.model.ArtistSummaryResponse
import com.nhajc.android.data.network.model.GenreRequest
import com.nhajc.android.data.network.model.IdResponse
import com.nhajc.android.data.network.model.PageResponse
import com.nhajc.android.data.network.model.ReleaseRequest
import com.nhajc.android.data.network.model.ReleaseSummaryResponse
import com.nhajc.android.data.network.model.ReorderSongsRequest
import com.nhajc.android.data.network.model.SaveUploadedSongsRequest
import com.nhajc.android.data.network.model.StagedSongResponse
import com.nhajc.android.data.network.model.UploadSongRequest
import kotlin.uuid.Uuid
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {
    @GET("admin/artists")
    suspend fun listArtists(
        @Query("q") query: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<ArtistSummaryResponse>

    @Multipart
    @POST("admin/artists")
    suspend fun createArtist(
        @Part("data") request: ArtistRequest,
        @Part avatar: MultipartBody.Part?
    ): IdResponse

    @POST("admin/genres")
    suspend fun createGenre(@Body request: GenreRequest): IdResponse

    @GET("admin/releases")
    suspend fun listReleases(
        @Query("q") query: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<ReleaseSummaryResponse>

    @Multipart
    @POST("admin/releases")
    suspend fun createRelease(
        @Part("data") request: ReleaseRequest,
        @Part cover: MultipartBody.Part?
    ): IdResponse

    @PUT("admin/releases/{id}/songs/order")
    suspend fun reorderSongs(@Path("id") id: Uuid, @Body request: ReorderSongsRequest)

    @Multipart
    @POST("admin/releases/{id}/songs")
    suspend fun uploadSong(
        @Path("id") releaseId: Uuid,
        @Part("data") request: UploadSongRequest,
        @Part audio: MultipartBody.Part
    ): IdResponse

    @Multipart
    @POST("admin/releases/{id}/song-uploads")
    suspend fun stageSong(
        @Path("id") releaseId: Uuid,
        @Part audio: MultipartBody.Part
    ): StagedSongResponse

    @PUT("admin/releases/{id}/song-uploads")
    suspend fun saveUploads(@Path("id") releaseId: Uuid, @Body request: SaveUploadedSongsRequest)

    @DELETE("admin/releases/{id}/song-uploads/{uploadId}")
    suspend fun discardUpload(@Path("id") releaseId: Uuid, @Path("uploadId") uploadId: Uuid)
}
