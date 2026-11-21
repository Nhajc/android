package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.GenreRequest
import com.nhajc.android.data.network.model.GenreResponse
import com.nhajc.android.data.network.model.IdResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GenreApi {
    @POST("admin/genres")
    suspend fun createGenre(@Body request: GenreRequest): IdResponse

    @GET("genres")
    suspend fun getGenres(): List<GenreResponse>
}
