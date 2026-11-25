package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.CommentRequest
import com.nhajc.android.data.network.model.CommentResponse
import com.nhajc.android.data.network.model.PageResponse
import kotlin.uuid.Uuid
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CommentApi {
    @GET("songs/{id}/comments")
    suspend fun getComments(
        @Path("id") songId: Uuid,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<CommentResponse>

    @GET("comments/{id}/replies")
    suspend fun getReplies(
        @Path("id") commentId: Uuid,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<CommentResponse>

    @POST("songs/{id}/comments")
    suspend fun createComment(@Path("id") songId: Uuid, @Body request: CommentRequest): CommentResponse

    @POST("comments/{id}/replies")
    suspend fun createReply(@Path("id") commentId: Uuid, @Body request: CommentRequest): CommentResponse

    @DELETE("comments/{id}")
    suspend fun deleteComment(@Path("id") id: Uuid)
}
