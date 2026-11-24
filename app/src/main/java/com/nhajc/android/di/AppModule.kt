package com.nhajc.android.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nhajc.android.BuildConfig
import com.nhajc.android.data.database.AppDatabase
import com.nhajc.android.data.datastore.SessionDataStore
import com.nhajc.android.data.datastore.sessionDataStore
import com.nhajc.android.data.network.AuthApi
import com.nhajc.android.data.network.TokenAuthenticator
import com.nhajc.android.data.network.TokenInterceptor
import com.nhajc.android.data.network.UserApi
import com.nhajc.android.data.network.networkJson
import com.nhajc.android.data.repository.SettingsRepository
import com.nhajc.android.data.repository.settingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @Named("base")
    fun baseClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun apiClient(
        @Named("base") base: OkHttpClient,
        interceptor: TokenInterceptor,
        authenticator: TokenAuthenticator
    ): OkHttpClient = base.newBuilder()
        .addInterceptor(interceptor)
        .authenticator(authenticator)
        .build()

    @Provides
    @Singleton
    fun authApi(@Named("base") client: OkHttpClient): AuthApi = retrofit(client).create()

    @Provides
    @Singleton
    fun apiRetrofit(client: OkHttpClient): Retrofit = retrofit(client)

    @Provides
    @Singleton
    fun userApi(retrofit: Retrofit): UserApi = retrofit.create()

    @Provides
    @Singleton
    fun sessionDataStore(@ApplicationContext context: Context): SessionDataStore =
        SessionDataStore(context.sessionDataStore)

    @Provides
    @Singleton
    fun settingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context.settingsDataStore)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "nhajc.db")
            .setDriver(BundledSQLiteDriver())
            .build()
}

private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.BASE_URL)
    .client(client)
    .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
    .build()
