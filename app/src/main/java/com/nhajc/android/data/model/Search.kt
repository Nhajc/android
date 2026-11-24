package com.nhajc.android.data.model

enum class SearchCategory { ALL, SONG, RELEASE, PLAYLIST, ARTIST }

enum class SearchSource { SAMPLE, SERVER }

enum class SearchError {
    NETWORK, UNAUTHORIZED, INVALID_QUERY, INVALID_ADDRESS, INVALID_RESPONSE, SERVER, UNKNOWN
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
    val songCount: Int? = null
)

data class SearchPageViewModel(
    val items: List<SearchItemViewModel>,
    val page: Int,
    val hasNext: Boolean
)

data class SearchConnectionViewModel(
    val source: SearchSource = SearchSource.SAMPLE,
    val serverUrl: String = "http://10.0.2.2:8080/api/v1/",
    val username: String? = null
)

data class SearchViewModel(
    val query: String = "",
    val category: SearchCategory = SearchCategory.ALL,
    val items: List<SearchItemViewModel> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val page: Int = 0,
    val hasNext: Boolean = false,
    val error: SearchError? = null,
    val connection: SearchConnectionViewModel = SearchConnectionViewModel(),
    val isConnecting: Boolean = false,
    val connectionError: SearchError? = null
)
