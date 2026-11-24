package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.ArtistRequest
import com.nhajc.android.data.network.model.ArtistResponse
import com.nhajc.android.data.network.model.IdResponse
import kotlin.uuid.Uuid
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ArtistApi {
    @Multipart
    @POST("admin/artists")
    suspend fun createArtist(
        @Part("data") request: ArtistRequest,
        @Part avatar: MultipartBody.Part?
    ): IdResponse

    @GET("artists/{id}")
    suspend fun getArtist(@Path("id") id: Uuid): ArtistResponse
}
