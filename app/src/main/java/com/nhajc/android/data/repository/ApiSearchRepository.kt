package com.nhajc.android.data.repository

import com.nhajc.android.data.model.SearchCategory
import com.nhajc.android.data.model.SearchItemViewModel
import com.nhajc.android.data.model.SearchPageViewModel
import com.nhajc.android.data.network.SearchApi
import com.nhajc.android.data.network.model.ArtistSummaryResponse
import com.nhajc.android.data.network.model.PlaylistSummaryResponse
import com.nhajc.android.data.network.model.ReleaseSummaryResponse
import com.nhajc.android.data.network.model.SearchItemResponse
import com.nhajc.android.data.network.model.SongResponse
import com.nhajc.android.data.network.model.UnknownItem

class ApiSearchRepository(private val api: SearchApi) : SearchRepository {
    override suspend fun search(
        query: String,
        category: SearchCategory,
        page: Int,
        size: Int
    ): SearchPageViewModel = repositoryCall {
        val keyword = validateSearch(query, page, size)
        val response = when (category) {
            SearchCategory.ALL -> api.search(keyword, page, size)
            SearchCategory.SONG -> api.searchSongs(keyword, page, size)
            SearchCategory.RELEASE -> api.searchReleases(keyword, page, size)
            SearchCategory.PLAYLIST -> api.searchPlaylists(keyword, page, size)
            SearchCategory.ARTIST -> api.searchArtists(keyword, page, size)
        }
        SearchPageViewModel(
            items = response.content.mapNotNull { it.toViewModel() },
            page = response.page,
            hasNext = response.hasNext
        )
    }
}

private fun SearchItemResponse.toViewModel(): SearchItemViewModel? = when (this) {
    is SongResponse -> SearchItemViewModel(
        id = id.toString(), category = SearchCategory.SONG, title = title,
        imageUrl = coverUrl, artists = artists.map { it.name },
        releaseTitle = releaseTitle, durationSeconds = durationSeconds
    )
    is ReleaseSummaryResponse -> SearchItemViewModel(
        id = id.toString(), category = SearchCategory.RELEASE, title = title,
        imageUrl = coverUrl, artists = artists.map { it.name }, songCount = songCount
    )
    is PlaylistSummaryResponse -> SearchItemViewModel(
        id = id.toString(), category = SearchCategory.PLAYLIST, title = title,
        imageUrl = coverUrl, ownerName = owner.username, songCount = songCount
    )
    is ArtistSummaryResponse -> SearchItemViewModel(
        id = id.toString(), category = SearchCategory.ARTIST, title = name, imageUrl = avatarUrl
    )
    UnknownItem -> null
}
