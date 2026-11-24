package com.nhajc.android.data.repository

import android.net.Uri
import com.nhajc.android.data.model.AccountError
import com.nhajc.android.data.model.AccountViewModel

interface AccountRepository {
    suspend fun login(usernameOrEmail: String, password: String)

    suspend fun register(username: String, email: String, password: String)

    suspend fun getAccount(): AccountViewModel

    suspend fun updateAvatar(uri: Uri?): AccountViewModel

    suspend fun logout()
}

class AccountRepositoryException(
    val reason: AccountError,
    cause: Throwable? = null
) : Exception(reason.name, cause)

internal fun Exception.accountError(): AccountError {
    if (this is AccountRepositoryException) {
        return reason
    }

    return AccountError.UNKNOWN
}