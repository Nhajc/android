package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.IdResponse
import com.nhajc.android.data.network.model.ReleaseRequest
import com.nhajc.android.data.network.model.ReleaseResponse
import kotlin.uuid.Uuid
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ReleaseApi {
    @Multipart
    @POST("admin/releases")
    suspend fun createRelease(
        @Part("data") request: ReleaseRequest,
        @Part cover: MultipartBody.Part?
    ): IdResponse

    @GET("releases/{id}")
    suspend fun getRelease(@Path("id") id: Uuid): ReleaseResponse
}
