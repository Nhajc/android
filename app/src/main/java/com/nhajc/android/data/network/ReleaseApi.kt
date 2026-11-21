package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.IdResponse
import com.nhajc.android.data.network.model.ReleaseRequest
import com.nhajc.android.data.network.model.ReleaseResponse
import kotlin.uuid.Uuid
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ReleaseApi {
    @POST("admin/releases")
    suspend fun createRelease(@Body request: ReleaseRequest): IdResponse

    @GET("releases/{id}")
    suspend fun getRelease(@Path("id") id: Uuid): ReleaseResponse
}
