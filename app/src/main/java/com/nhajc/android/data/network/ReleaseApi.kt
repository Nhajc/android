package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.ReleaseResponse
import kotlin.uuid.Uuid
import retrofit2.http.GET
import retrofit2.http.Path

interface ReleaseApi {
    @GET("releases/{id}")
    suspend fun getRelease(@Path("id") id: Uuid): ReleaseResponse
}
