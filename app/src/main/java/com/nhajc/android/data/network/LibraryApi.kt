package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.ArtistSummaryResponse
import com.nhajc.android.data.network.model.LibraryItemResponse
import com.nhajc.android.data.network.model.LibrarySort
import com.nhajc.android.data.network.model.PageResponse
import com.nhajc.android.data.network.model.PlaylistSummaryResponse
import com.nhajc.android.data.network.model.ReleaseSummaryResponse
import com.nhajc.android.data.network.model.SongResponse
import kotlin.uuid.Uuid
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface LibraryApi {
    @GET("library")
    suspend fun getLibrary(
        @Query("q") q: String?,
        @Query("sort") sort: LibrarySort,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<LibraryItemResponse>

    @GET("library/releases")
    suspend fun getReleases(
        @Query("q") q: String?,
        @Query("sort") sort: LibrarySort,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<ReleaseSummaryResponse>

    @GET("library/playlists")
    suspend fun getPlaylists(
        @Query("q") q: String?,
        @Query("owned") owned: Boolean,
        @Query("sort") sort: LibrarySort,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<PlaylistSummaryResponse>

    @GET("library/artists")
    suspend fun getArtists(
        @Query("q") q: String?,
        @Query("sort") sort: LibrarySort,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<ArtistSummaryResponse>

    @GET("library/songs")
    suspend fun getSongs(
        @Query("q") q: String?,
        @Query("sort") sort: LibrarySort,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<SongResponse>

    @PUT("library/releases/{id}")
    suspend fun saveRelease(@Path("id") id: Uuid)

    @DELETE("library/releases/{id}")
    suspend fun unsaveRelease(@Path("id") id: Uuid)

    @PUT("library/playlists/{id}")
    suspend fun savePlaylist(@Path("id") id: Uuid)

    @DELETE("library/playlists/{id}")
    suspend fun unsavePlaylist(@Path("id") id: Uuid)

    @PUT("library/artists/{id}")
    suspend fun followArtist(@Path("id") id: Uuid)

    @DELETE("library/artists/{id}")
    suspend fun unfollowArtist(@Path("id") id: Uuid)

    @PUT("library/songs/{id}")
    suspend fun favoriteSong(@Path("id") id: Uuid)

    @DELETE("library/songs/{id}")
    suspend fun unfavoriteSong(@Path("id") id: Uuid)
}
