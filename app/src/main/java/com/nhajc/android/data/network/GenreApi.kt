package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.GenreRequest
import com.nhajc.android.data.network.model.GenreResponse
import com.nhajc.android.data.network.model.IdResponse
import com.nhajc.android.data.network.model.PageResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GenreApi {
    @POST("admin/genres")
    suspend fun createGenre(@Body request: GenreRequest): IdResponse

    @GET("genres")
    suspend fun getGenres(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<GenreResponse>
}
