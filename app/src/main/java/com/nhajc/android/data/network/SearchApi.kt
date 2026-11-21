package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.ArtistSummaryResponse
import com.nhajc.android.data.network.model.PageResponse
import com.nhajc.android.data.network.model.PlaylistSummaryResponse
import com.nhajc.android.data.network.model.ReleaseSummaryResponse
import com.nhajc.android.data.network.model.SearchItemResponse
import com.nhajc.android.data.network.model.SongResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {
    @GET("search")
    suspend fun search(
        @Query("q") q: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<SearchItemResponse>

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("q") q: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<SongResponse>

    @GET("search/releases")
    suspend fun searchReleases(
        @Query("q") q: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<ReleaseSummaryResponse>

    @GET("search/playlists")
    suspend fun searchPlaylists(
        @Query("q") q: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<PlaylistSummaryResponse>

    @GET("search/artists")
    suspend fun searchArtists(
        @Query("q") q: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<ArtistSummaryResponse>
}
