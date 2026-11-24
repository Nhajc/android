package com.nhajc.android.data.network

import com.nhajc.android.data.datastore.SessionDataStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class TokenInterceptor @Inject constructor(private val session: SessionDataStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val tokens = runBlocking { session.tokens.first() }

        if (tokens == null) {
            return chain.proceed(chain.request())
        }

        return chain.proceed(chain.request().withToken(tokens.accessToken))
    }
}

internal fun Request.withToken(accessToken: String): Request = newBuilder()
    .header("Authorization", "Bearer $accessToken")
    .build()
