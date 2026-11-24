package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.UpdateProfileRequest
import com.nhajc.android.data.network.model.UserResponse
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part

interface UserApi {
    @GET("users/me")
    suspend fun getMe(): UserResponse

    @Multipart
    @PUT("users/me")
    suspend fun updateMe(
        @Part("data") request: UpdateProfileRequest,
        @Part avatar: MultipartBody.Part?
    ): UserResponse
}
