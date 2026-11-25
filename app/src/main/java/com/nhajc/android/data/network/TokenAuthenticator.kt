package com.nhajc.android.data.network

import com.nhajc.android.data.datastore.AuthTokens
import com.nhajc.android.data.datastore.SessionDataStore
import com.nhajc.android.data.network.model.RefreshTokenRequest
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException

class TokenAuthenticator @Inject constructor(private val session: SessionDataStore, private val authApi: AuthApi) :
    Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val sentToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        if (sentToken == null || response.priorResponse != null) {
            return null
        }

        synchronized(lock) {
            val tokens = runBlocking { session.tokens.first() } ?: return null

            if (tokens.accessToken != sentToken) {
                return response.request.withToken(tokens.accessToken)
            }

            try {
                val refreshed = runBlocking {
                    val result = authApi.refresh(RefreshTokenRequest(tokens.refreshToken))
                    session.saveTokens(AuthTokens(result.accessToken, result.refreshToken))
                    result
                }

                return response.request.withToken(refreshed.accessToken)
            } catch (exception: HttpException) {
                if (exception.code() == 401) {
                    runBlocking { session.clearTokens() }
                }

                return null
            } catch (exception: IOException) {
                return null
            }
        }
    }
}
