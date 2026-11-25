package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.ArtistResponse
import kotlin.uuid.Uuid
import retrofit2.http.GET
import retrofit2.http.Path

interface ArtistApi {
    @GET("artists/{id}")
    suspend fun getArtist(@Path("id") id: Uuid): ArtistResponse
}
