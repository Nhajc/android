package com.nhajc.android.data.repository

import com.nhajc.android.data.model.Artist
import com.nhajc.android.data.model.ArtistError
import com.nhajc.android.data.model.ArtistException
import com.nhajc.android.data.model.ArtistRelease
import com.nhajc.android.data.network.ArtistApi
import com.nhajc.android.data.network.LibraryApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

@Singleton
class ArtistRepository @Inject constructor(
    private val artistApi: ArtistApi,
    private val libraryApi: LibraryApi
) {
    suspend fun getArtist(id: Uuid): Artist {
        val response = call { artistApi.getArtist(id) }
        val releases = mutableListOf<ArtistRelease>()

        for (release in response.releases) {
            releases.add(
                ArtistRelease(
                    id = release.id,
                    title = release.title,
                    coverUrl = release.coverUrl,
                    releaseType = release.releaseType,
                    releaseDate = release.releaseDate,
                    songCount = release.songCount
                )
            )
        }

        releases.sortByDescending { it.releaseDate }

        return Artist(
            id = response.id,
            name = response.name,
            avatarUrl = response.avatarUrl,
            isFollowed = response.followedAt != null,
            releases = releases
        )
    }

    suspend fun setFollowed(id: Uuid, followed: Boolean) {
        call {
            if (followed) {
                libraryApi.followArtist(id)
            } else {
                libraryApi.unfollowArtist(id)
            }
        }
    }

    private suspend fun <T> call(block: suspend () -> T): T {
        try {
            return block()
        } catch (exception: HttpException) {
            val error = when (exception.code()) {
                401 -> ArtistError.SESSION_ENDED
                403 -> ArtistError.FORBIDDEN
                404 -> ArtistError.NOT_FOUND
                else -> ArtistError.SERVER
            }

            throw ArtistException(error, exception)
        } catch (exception: SerializationException) {
            throw ArtistException(ArtistError.INVALID_RESPONSE, exception)
        } catch (exception: IOException) {
            throw ArtistException(ArtistError.NETWORK, exception)
        }
    }
}
