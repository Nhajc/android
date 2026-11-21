package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.ArtistRequest
import com.nhajc.android.data.network.model.ArtistResponse
import com.nhajc.android.data.network.model.IdResponse
import kotlin.uuid.Uuid
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ArtistApi {
    @POST("admin/artists")
    suspend fun createArtist(@Body request: ArtistRequest): IdResponse

    @GET("artists/{id}")
    suspend fun getArtist(@Path("id") id: Uuid): ArtistResponse
}
