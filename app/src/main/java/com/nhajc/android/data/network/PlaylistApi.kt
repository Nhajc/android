package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.PlaylistRequest
import com.nhajc.android.data.network.model.PlaylistResponse
import kotlin.uuid.Uuid
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PlaylistApi {
    @GET("playlists/{id}")
    suspend fun getPlaylist(@Path("id") id: Uuid): PlaylistResponse

    @POST("playlists")
    suspend fun createPlaylist(@Body request: PlaylistRequest): PlaylistResponse

    @PUT("playlists/{id}")
    suspend fun updatePlaylist(@Path("id") id: Uuid, @Body request: PlaylistRequest)

    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: Uuid)

    @PUT("playlists/{id}/songs/{songId}")
    suspend fun addSong(@Path("id") id: Uuid, @Path("songId") songId: Uuid)

    @DELETE("playlists/{id}/songs/{songId}")
    suspend fun removeSong(@Path("id") id: Uuid, @Path("songId") songId: Uuid)

    @PUT("playlists/{id}/songs")
    suspend fun reorderSongs(@Path("id") id: Uuid, @Body songIds: List<Uuid>)
}
