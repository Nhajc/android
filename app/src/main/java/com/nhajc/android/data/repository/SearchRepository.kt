package com.nhajc.android.data.repository

import com.nhajc.android.data.model.SearchCategory
import com.nhajc.android.data.model.SearchError
import com.nhajc.android.data.model.SearchPageViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

interface SearchRepository {
    suspend fun search(
        query: String,
        category: SearchCategory = SearchCategory.ALL,
        page: Int = 0,
        size: Int = 20
    ): SearchPageViewModel
}

class SearchRepositoryException(val reason: SearchError, cause: Throwable? = null) :
    Exception(reason.name, cause)

internal fun validateSearch(query: String, page: Int, size: Int): String {
    val trimmed = query.trim()
    if (trimmed.isEmpty() || trimmed.length > 100 || page < 0 || size !in 1..100) {
        throw SearchRepositoryException(SearchError.INVALID_QUERY)
    }
    return trimmed
}

internal suspend fun <T> repositoryCall(block: suspend () -> T): T = try {
    block()
} catch (exception: CancellationException) {
    throw exception
} catch (exception: SearchRepositoryException) {
    throw exception
} catch (exception: HttpException) {
    val reason = when (exception.code()) {
        401, 403 -> SearchError.UNAUTHORIZED
        400 -> SearchError.INVALID_QUERY
        else -> SearchError.SERVER
    }
    throw SearchRepositoryException(reason, exception)
} catch (exception: SerializationException) {
    throw SearchRepositoryException(SearchError.INVALID_RESPONSE, exception)
} catch (exception: IOException) {
    throw SearchRepositoryException(SearchError.NETWORK, exception)
}
