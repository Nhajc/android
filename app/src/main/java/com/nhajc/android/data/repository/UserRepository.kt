package com.nhajc.android.data.repository

import android.content.Context
import android.net.Uri
import com.nhajc.android.data.database.AppDatabase
import com.nhajc.android.data.datastore.AuthTokens
import com.nhajc.android.data.datastore.SessionDataStore
import com.nhajc.android.data.model.AppError
import com.nhajc.android.data.model.AppException
import com.nhajc.android.data.model.User
import com.nhajc.android.data.network.AuthApi
import com.nhajc.android.data.network.UserApi
import com.nhajc.android.data.network.model.AuthResponse
import com.nhajc.android.data.network.model.LoginRequest
import com.nhajc.android.data.network.model.RefreshTokenRequest
import com.nhajc.android.data.network.model.RegisterRequest
import com.nhajc.android.data.network.model.UpdateProfileRequest
import com.nhajc.android.data.network.model.UserResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

@Singleton
class UserRepository @Inject constructor(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val session: SessionDataStore,
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) {
    val isLoggedIn: Flow<Boolean> = session.tokens.map { tokens -> tokens != null }

    val user: Flow<User?> = session.user

    suspend fun login(usernameOrEmail: String, password: String) {
        val response = call(unauthorized = AppError.INVALID_CREDENTIALS) {
            authApi.login(LoginRequest(usernameOrEmail, password))
        }

        start(response)
    }

    suspend fun register(username: String, email: String, password: String) {
        val response = call {
            authApi.register(RegisterRequest(username, email, password))
        }

        start(response)
    }

    suspend fun refreshUser() {
        val response = call { userApi.getMe() }

        session.saveUser(response.asModel())
    }

    suspend fun updateAvatar(uri: Uri?) {
        val avatar = if (uri == null) {
            null
        } else {
            readAvatar(uri)
        }

        val response = call(badRequest = AppError.INVALID_AVATAR) {
            userApi.updateMe(UpdateProfileRequest, avatar)
        }

        session.saveUser(response.asModel())
    }

    suspend fun logout() {
        val tokens = session.tokens.first() ?: return

        try {
            call { authApi.logout(RefreshTokenRequest(tokens.refreshToken)) }
        } catch (_: AppException) {
        }

        session.clearTokens()
    }

    private suspend fun start(response: AuthResponse) {
        val previousUser = session.user.first()

        if (previousUser != null && previousUser.id != response.user.id) {
            database.clearAllTables()
        }

        session.save(
            AuthTokens(response.accessToken, response.refreshToken),
            response.user.asModel()
        )
    }

    private suspend fun readAvatar(uri: Uri): MultipartBody.Part = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val type = resolver.getType(uri) ?: throw AppException(AppError.INVALID_AVATAR)
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw AppException(AppError.INVALID_AVATAR)

        MultipartBody.Part.createFormData("file", "avatar", bytes.toRequestBody(type.toMediaType()))
    }

    private suspend fun <T> call(
        unauthorized: AppError = AppError.SESSION_ENDED,
        badRequest: AppError = AppError.INVALID_INPUT,
        block: suspend () -> T
    ): T {
        try {
            return block()
        } catch (exception: HttpException) {
            val error = when (exception.code()) {
                400 -> badRequest
                401 -> unauthorized
                409 -> AppError.USER_EXISTS
                413 -> AppError.INVALID_AVATAR
                else -> AppError.SERVER
            }

            throw AppException(error, exception)
        } catch (exception: IOException) {
            throw AppException(AppError.NETWORK, exception)
        }
    }
}

private fun UserResponse.asModel() = User(
    id = id,
    username = username,
    email = email,
    role = role,
    avatarUrl = avatarUrl
)
