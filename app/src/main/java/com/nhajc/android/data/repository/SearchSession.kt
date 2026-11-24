package com.nhajc.android.data.repository

import com.nhajc.android.data.model.SearchConnectionViewModel
import com.nhajc.android.data.model.SearchError
import com.nhajc.android.data.model.SearchSource
import com.nhajc.android.data.network.ApiClient
import com.nhajc.android.data.network.model.LoginRequest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

// Application-scoped, in-memory session: no password or token is written to disk.
class SearchSession(initialRepository: SearchRepository = SampleSearchRepository()) {
    var repository: SearchRepository = initialRepository
        private set
    var connection = SearchConnectionViewModel()
        private set

    fun useSample() {
        repository = SampleSearchRepository()
        connection = connection.copy(source = SearchSource.SAMPLE, username = null)
    }

    suspend fun connect(address: String, username: String, password: String) = repositoryCall {
        val client = try {
            ApiClient(address)
        } catch (exception: IllegalArgumentException) {
            throw SearchRepositoryException(SearchError.INVALID_ADDRESS, exception)
        }
        val auth = client.auth.login(LoginRequest(username.trim(), password))
        currentCoroutineContext().ensureActive()
        client.authorize(auth.accessToken)
        repository = ApiSearchRepository(client.search)
        connection = SearchConnectionViewModel(SearchSource.SERVER, client.baseUrl, auth.user.username)
    }
}
