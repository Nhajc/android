package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.PlaylistRequest
import com.nhajc.android.data.network.model.PlaylistResponse
import kotlin.uuid.Uuid
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface PlaylistApi {
    @GET("playlists/{id}")
    suspend fun getPlaylist(@Path("id") id: Uuid): PlaylistResponse

    @Multipart
    @POST("playlists")
    suspend fun createPlaylist(
        @Part("data") request: PlaylistRequest,
        @Part cover: MultipartBody.Part?
    ): PlaylistResponse

    @Multipart
    @PUT("playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") id: Uuid,
        @Part("data") request: PlaylistRequest,
        @Part cover: MultipartBody.Part?
    )

    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: Uuid)

    @PUT("playlists/{id}/songs/{songId}")
    suspend fun addSong(@Path("id") id: Uuid, @Path("songId") songId: Uuid)
}
