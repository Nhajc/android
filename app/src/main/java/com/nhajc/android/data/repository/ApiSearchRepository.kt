package com.nhajc.android.data.repository

import com.nhajc.android.data.model.ArtistSummary
import com.nhajc.android.data.model.SearchCategory
import com.nhajc.android.data.model.SearchItemViewModel
import com.nhajc.android.data.model.SearchPageViewModel
import com.nhajc.android.data.model.Song
import com.nhajc.android.data.network.SearchApi
import com.nhajc.android.data.network.model.ArtistSummaryResponse
import com.nhajc.android.data.network.model.PlaylistSummaryResponse
import com.nhajc.android.data.network.model.ReleaseSummaryResponse
import com.nhajc.android.data.network.model.SearchItemResponse
import com.nhajc.android.data.network.model.SongResponse
import com.nhajc.android.data.network.model.UnknownItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ApiSearchRepository(private val api: SearchApi) : SearchRepository {
    override suspend fun search(
        query: String,
        categories: Set<SearchCategory>,
        page: Int,
        size: Int
    ): SearchPageViewModel = repositoryCall {
        val keyword = validateSearch(query, page, size)
        if (categories.isEmpty()) {
            val response = api.search(keyword, page, size)
            return@repositoryCall SearchPageViewModel(
                response.content.mapNotNull { it.toViewModel() },
                response.page,
                response.hasNext
            )
        }
        // Each selected category advances one backend page at a time. Keep every
        // returned item so combining filters never drops hits between pages.
        val pages = coroutineScope {
            SearchCategory.entries.filter { it in categories }.map { category ->
                async {
                    val response = when (category) {
                        SearchCategory.SONG -> api.searchSongs(keyword, page, size)
                        SearchCategory.RELEASE -> api.searchReleases(keyword, page, size)
                        SearchCategory.PLAYLIST -> api.searchPlaylists(keyword, page, size)
                        SearchCategory.ARTIST -> api.searchArtists(keyword, page, size)
                    }
                    SearchPageViewModel(
                        response.content.mapNotNull { it.toViewModel() },
                        response.page,
                        response.hasNext
                    )
                }
            }.awaitAll()
        }
        SearchPageViewModel(
            items = pages.flatMap { it.items },
            page = page,
            hasNext = pages.any { it.hasNext }
        )
    }
}

private fun SearchItemResponse.toViewModel(): SearchItemViewModel? = when (this) {
    is SongResponse -> SearchItemViewModel(
        id = id.toString(),
        category = SearchCategory.SONG,
        title = title,
        imageUrl = coverUrl,
        artists = artists.map { it.name },
        releaseTitle = releaseTitle,
        durationSeconds = durationSeconds,
        song = Song(
            id = id, title = title, trackNumber = trackNumber, durationSeconds = durationSeconds,
            artists = artists.map { ArtistSummary(it.id, it.name, it.avatarUrl) },
            releaseId = releaseId, releaseTitle = releaseTitle, coverUrl = coverUrl,
            isFavorite = favoritedAt != null
        )
    )

    is ReleaseSummaryResponse -> SearchItemViewModel(
        id = id.toString(),
        category = SearchCategory.RELEASE,
        title = title,
        imageUrl = coverUrl,
        artists = artists.map { it.name },
        songCount = songCount
    )

    is PlaylistSummaryResponse -> SearchItemViewModel(
        id = id.toString(),
        category = SearchCategory.PLAYLIST,
        title = title,
        imageUrl = coverUrl,
        ownerName = owner.username,
        songCount = songCount
    )

    is ArtistSummaryResponse -> SearchItemViewModel(
        id = id.toString(),
        category = SearchCategory.ARTIST,
        title = name,
        imageUrl = avatarUrl
    )

    UnknownItem -> null
}
