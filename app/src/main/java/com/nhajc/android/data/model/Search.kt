package com.nhajc.android.data.model

enum class SearchCategory { RELEASE, PLAYLIST, ARTIST, SONG }

enum class SearchError {
    NETWORK,
    UNAUTHORIZED,
    INVALID_QUERY,
    INVALID_RESPONSE,
    SERVER,
    UNKNOWN
}

// Presentation models deliberately contain no network DTOs or database entities.
data class SearchItemViewModel(
    val id: String,
    val category: SearchCategory,
    val title: String,
    val imageUrl: String? = null,
    val artists: List<String> = emptyList(),
    val ownerName: String? = null,
    val releaseTitle: String? = null,
    val durationSeconds: Int? = null,
    val songCount: Int? = null,
    val song: Song? = null
)

data class SearchPageViewModel(
    val items: List<SearchItemViewModel>,
    val page: Int,
    val hasNext: Boolean
)

data class SearchViewModel(
    val query: String = "",
    val categories: Set<SearchCategory> = emptySet(),
    val items: List<SearchItemViewModel> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val page: Int = 0,
    val hasNext: Boolean = false,
    val error: SearchError? = null
)
