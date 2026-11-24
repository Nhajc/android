package com.nhajc.android.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nhajc.android.data.model.User
import com.nhajc.android.data.model.UserRole
import java.io.IOException
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

data class AuthTokens(val accessToken: String, val refreshToken: String)

class SessionDataStore(private val dataStore: DataStore<Preferences>) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val usernameKey = stringPreferencesKey("username")
    private val emailKey = stringPreferencesKey("email")
    private val roleKey = stringPreferencesKey("role")
    private val avatarUrlKey = stringPreferencesKey("avatar_url")

    private val preferences: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    val tokens: Flow<AuthTokens?> = preferences.map { values ->
        val accessToken = values[accessTokenKey]
        val refreshToken = values[refreshTokenKey]

        if (accessToken == null || refreshToken == null) {
            null
        } else {
            AuthTokens(accessToken, refreshToken)
        }
    }

    val user: Flow<User?> = preferences.map { values ->
        val id = values[userIdKey]
        val username = values[usernameKey]
        val email = values[emailKey]

        if (id == null || username == null || email == null) {
            null
        } else {
            User(
                id = Uuid.parse(id),
                username = username,
                email = email,
                role = UserRole.entries.firstOrNull { it.name == values[roleKey] } ?: UserRole.USER,
                avatarUrl = values[avatarUrlKey]
            )
        }
    }

    suspend fun save(tokens: AuthTokens, user: User) {
        dataStore.edit { values ->
            values.putTokens(tokens)
            values.putUser(user)
        }
    }

    suspend fun saveTokens(tokens: AuthTokens) {
        dataStore.edit { values ->
            values.putTokens(tokens)
        }
    }

    suspend fun saveUser(user: User) {
        dataStore.edit { values ->
            values.putUser(user)
        }
    }

    suspend fun clearTokens() {
        dataStore.edit { values ->
            values.remove(accessTokenKey)
            values.remove(refreshTokenKey)
        }
    }

    private fun MutablePreferences.putTokens(tokens: AuthTokens) {
        this[accessTokenKey] = tokens.accessToken
        this[refreshTokenKey] = tokens.refreshToken
    }

    private fun MutablePreferences.putUser(user: User) {
        this[userIdKey] = user.id.toString()
        this[usernameKey] = user.username
        this[emailKey] = user.email
        this[roleKey] = user.role.name

        if (user.avatarUrl == null) {
            remove(avatarUrlKey)
        } else {
            this[avatarUrlKey] = user.avatarUrl
        }
    }
}
