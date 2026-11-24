package com.nhajc.android.data.repository

import com.nhajc.android.data.model.SearchCategory
import com.nhajc.android.data.model.SearchItemViewModel
import com.nhajc.android.data.model.SearchPageViewModel
import java.util.Locale

// This source lets the view be developed and previewed without a backend.
class SampleSearchRepository : SearchRepository {
    override suspend fun search(
        query: String,
        category: SearchCategory,
        page: Int,
        size: Int
    ): SearchPageViewModel {
        val keyword = validateSearch(query, page, size).lowercase(Locale.ROOT)
        val matches = sampleItems.filter { item ->
            (category == SearchCategory.ALL || item.category == category) &&
                item.title.lowercase(Locale.ROOT).contains(keyword)
        }.sortedWith(
            compareBy<SearchItemViewModel> {
                val title = it.title.lowercase(Locale.ROOT)
                when {
                    title == keyword -> 0
                    title.startsWith(keyword) -> 1
                    else -> 2
                }
            }.thenBy { it.title.lowercase(Locale.ROOT) }.thenBy { it.id }
        )
        val offset = (page.toLong() * size).coerceAtMost(matches.size.toLong()).toInt()
        val end = (offset + size).coerceAtMost(matches.size)
        return SearchPageViewModel(matches.subList(offset, end), page, end < matches.size)
    }

    private val sampleItems = listOf(
        SearchItemViewModel("song-1", SearchCategory.SONG, "Lạc trôi",
            artists = listOf("Sơn Tùng M-TP"), releaseTitle = "Lạc trôi", durationSeconds = 235),
        SearchItemViewModel("song-2", SearchCategory.SONG, "Chúng ta của hiện tại",
            artists = listOf("Sơn Tùng M-TP"), durationSeconds = 301),
        SearchItemViewModel("song-3", SearchCategory.SONG, "Có em chờ",
            artists = listOf("MIN"), durationSeconds = 219),
        SearchItemViewModel("song-4", SearchCategory.SONG, "Bước qua mùa cô đơn",
            artists = listOf("Vũ."), durationSeconds = 278),
        SearchItemViewModel("song-5", SearchCategory.SONG, "Chill cùng âm nhạc",
            artists = listOf("Nghệ sĩ mẫu"), durationSeconds = 192),
        SearchItemViewModel("release-1", SearchCategory.RELEASE, "Lạc trôi",
            artists = listOf("Sơn Tùng M-TP"), songCount = 1),
        SearchItemViewModel("release-2", SearchCategory.RELEASE, "Một vạn năm",
            artists = listOf("Vũ."), songCount = 10),
        SearchItemViewModel("playlist-1", SearchCategory.PLAYLIST, "Chill cuối ngày",
            ownerName = "Nhajc", songCount = 24),
        SearchItemViewModel("playlist-2", SearchCategory.PLAYLIST, "Lạc vào giai điệu",
            ownerName = "Nhajc", songCount = 18),
        SearchItemViewModel("artist-1", SearchCategory.ARTIST, "Sơn Tùng M-TP"),
        SearchItemViewModel("artist-2", SearchCategory.ARTIST, "MIN"),
        SearchItemViewModel("artist-3", SearchCategory.ARTIST, "Vũ.")
    )
}
