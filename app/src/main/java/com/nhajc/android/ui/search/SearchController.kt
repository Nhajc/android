package com.nhajc.android.ui.search

import com.nhajc.android.data.model.SearchCategory
import com.nhajc.android.data.model.SearchError
import com.nhajc.android.data.model.SearchViewModel
import com.nhajc.android.data.repository.SearchRepository
import com.nhajc.android.data.repository.SearchRepositoryException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// A plain state holder, not an AndroidX ViewModel. The view-model itself is a data class.
class SearchController(
    private val repository: SearchRepository,
    private val scope: CoroutineScope,
    query: String = "",
    categories: Set<SearchCategory> = emptySet(),
    private val debounceMillis: Long = 350
) {
    private val mutableModel = MutableStateFlow(
        SearchViewModel(query = query, categories = categories)
    )
    val model = mutableModel.asStateFlow()
    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        mutableModel.value = model.value.copy(query = query)
        search(debounceMillis)
    }

    fun selectCategory(category: SearchCategory) {
        val selected = model.value.categories
        mutableModel.value = model.value.copy(
            categories = if (category in selected) selected - category else selected + category
        )
        search(0)
    }

    fun submit() = search(0)

    fun retry() {
        if (model.value.items.isNotEmpty() && model.value.hasNext) loadMore() else submit()
    }

    private fun search(waitMillis: Long) {
        searchJob?.cancel()
        val snapshot = model.value
        val query = snapshot.query.trim()
        val valid = query.isNotEmpty() && query.length <= 100
        mutableModel.value = snapshot.copy(
            items = emptyList(),
            isLoading = valid,
            isLoadingMore = false,
            hasSearched = query.isNotEmpty(),
            page = 0,
            hasNext = false,
            error = if (query.length > 100) SearchError.INVALID_QUERY else null
        )
        if (!valid) return
        searchJob = scope.launch {
            try {
                delay(waitMillis)
                val result = repository.search(query, snapshot.categories)
                currentCoroutineContext().ensureActive()
                mutableModel.value = model.value.copy(
                    items = result.items,
                    page = result.page,
                    hasNext = result.hasNext,
                    isLoading = false,
                    error = null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                mutableModel.value = model.value.copy(isLoading = false, error = exception.reason())
            }
        }
    }

    fun loadMore() {
        val snapshot = model.value
        if (snapshot.isLoading || snapshot.isLoadingMore || !snapshot.hasNext) return
        mutableModel.value = snapshot.copy(isLoadingMore = true, error = null)
        searchJob = scope.launch {
            try {
                val result = repository.search(
                    snapshot.query.trim(),
                    snapshot.categories,
                    snapshot.page + 1
                )
                currentCoroutineContext().ensureActive()
                mutableModel.value = model.value.copy(
                    items = (snapshot.items + result.items).distinctBy { it.category to it.id },
                    page = result.page,
                    hasNext = result.hasNext,
                    isLoadingMore = false,
                    error = null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                mutableModel.value =
                    model.value.copy(isLoadingMore = false, error = exception.reason())
            }
        }
    }
}

private fun Exception.reason(): SearchError =
    (this as? SearchRepositoryException)?.reason ?: SearchError.UNKNOWN
