package com.nhajc.android.data.network

import com.nhajc.android.data.network.model.LibraryItemResponse
import com.nhajc.android.data.network.model.SearchItemResponse
import com.nhajc.android.data.network.model.UnknownItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    serializersModule = SerializersModule {
        polymorphicDefaultDeserializer(SearchItemResponse::class) { UnknownItem.serializer() }
        polymorphicDefaultDeserializer(LibraryItemResponse::class) { UnknownItem.serializer() }
    }
}
