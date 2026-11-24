package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.IdResponse
import com.nhajc.android.data.network.model.ReleaseRequest
import com.nhajc.android.data.network.model.ReleaseResponse
import com.nhajc.android.data.network.model.ReleaseSummaryResponse
import com.nhajc.android.data.network.model.PageResponse
import com.nhajc.android.data.network.model.ReorderSongsRequest
import kotlin.uuid.Uuid
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PUT
import retrofit2.http.Body

interface ReleaseApi {
    @PUT("admin/releases/{id}/songs/order")
    suspend fun reorderSongs(@Path("id") id: Uuid, @Body request: ReorderSongsRequest)

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

    @GET("releases/{id}")
    suspend fun getRelease(@Path("id") id: Uuid): ReleaseResponse
}
