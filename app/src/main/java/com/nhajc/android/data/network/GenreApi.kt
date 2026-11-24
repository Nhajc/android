package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.GenreResponse
import com.nhajc.android.data.network.model.PageResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GenreApi {
    @GET("genres")
    suspend fun getGenres(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<GenreResponse>
}
