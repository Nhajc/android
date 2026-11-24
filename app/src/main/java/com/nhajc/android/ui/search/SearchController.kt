package com.nhajc.android.ui.search

import com.nhajc.android.data.model.SearchCategory
import com.nhajc.android.data.model.SearchError
import com.nhajc.android.data.model.SearchViewModel
import com.nhajc.android.data.repository.SearchRepositoryException
import com.nhajc.android.data.repository.SearchSession
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
    private val session: SearchSession,
    private val scope: CoroutineScope,
    query: String = "",
    category: SearchCategory = SearchCategory.ALL,
    private val debounceMillis: Long = 350
) {
    private val mutableModel = MutableStateFlow(
        SearchViewModel(query = query, category = category, connection = session.connection)
    )
    val model = mutableModel.asStateFlow()
    private var searchJob: Job? = null
    private var connectionJob: Job? = null

    fun updateQuery(query: String) {
        mutableModel.value = model.value.copy(query = query)
        search(debounceMillis)
    }

    fun selectCategory(category: SearchCategory) {
        if (category == model.value.category) return
        mutableModel.value = model.value.copy(category = category)
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
            items = emptyList(), isLoading = valid, isLoadingMore = false,
            hasSearched = query.isNotEmpty(), page = 0, hasNext = false,
            error = if (query.length > 100) SearchError.INVALID_QUERY else null
        )
        if (!valid) return
        val repository = session.repository
        searchJob = scope.launch {
            try {
                delay(waitMillis)
                val result = repository.search(query, snapshot.category)
                currentCoroutineContext().ensureActive()
                mutableModel.value = model.value.copy(
                    items = result.items, page = result.page, hasNext = result.hasNext,
                    isLoading = false, error = null
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
        val repository = session.repository
        searchJob = scope.launch {
            try {
                val result = repository.search(snapshot.query.trim(), snapshot.category, snapshot.page + 1)
                currentCoroutineContext().ensureActive()
                mutableModel.value = model.value.copy(
                    items = (snapshot.items + result.items).distinctBy { it.category to it.id },
                    page = result.page, hasNext = result.hasNext,
                    isLoadingMore = false, error = null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                mutableModel.value = model.value.copy(isLoadingMore = false, error = exception.reason())
            }
        }
    }

    fun useSample() {
        cancelConnection()
        session.useSample()
        mutableModel.value = model.value.copy(connection = session.connection)
        search(0)
    }

    fun connect(address: String, username: String, password: String, onConnected: () -> Unit) {
        connectionJob?.cancel()
        mutableModel.value = model.value.copy(isConnecting = true, connectionError = null)
        connectionJob = scope.launch {
            try {
                session.connect(address, username, password)
                mutableModel.value = model.value.copy(
                    connection = session.connection, isConnecting = false, connectionError = null
                )
                search(0)
                onConnected()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                mutableModel.value = model.value.copy(
                    isConnecting = false, connectionError = exception.reason()
                )
            }
        }
    }

    fun cancelConnection() {
        connectionJob?.cancel()
        mutableModel.value = model.value.copy(isConnecting = false, connectionError = null)
    }
}

private fun Exception.reason(): SearchError =
    (this as? SearchRepositoryException)?.reason ?: SearchError.UNKNOWN
