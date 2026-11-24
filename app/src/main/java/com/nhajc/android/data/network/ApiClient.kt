package com.nhajc.android.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

internal class ApiClient(address: String) {
    val baseUrl: String = normalizeApiUrl(address)
    @Volatile private var accessToken: String? = null

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
            accessToken?.let { request.header("Authorization", "Bearer $it") }
            chain.proceed(request.build())
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
        .build()

    val auth: AuthApi = retrofit.create(AuthApi::class.java)
    val search: SearchApi = retrofit.create(SearchApi::class.java)

    fun authorize(token: String) { accessToken = token }
}

internal fun normalizeApiUrl(address: String): String {
    val url = requireNotNull(address.trim().toHttpUrlOrNull())
    require(url.username.isEmpty() && url.password.isEmpty())
    require(url.query == null && url.fragment == null)
    val path = url.encodedPath.trimEnd('/')
    return url.newBuilder()
        .encodedPath(if (path.isEmpty()) "/api/v1/" else "$path/")
        .build().toString()
}
