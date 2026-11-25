package com.nhajc.android.data.repository

import com.nhajc.android.data.database.AppDatabase
import com.nhajc.android.data.database.model.asModel
import com.nhajc.android.data.model.HomeContent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class HomeRepository @Inject constructor(
    database: AppDatabase,
    private val libraryRepository: LibraryRepository
) {
    private val library = database.libraryDao()

    // Read the local library so cached content remains visible without a connection.
    val content: Flow<HomeContent> = combine(
        library.homeReleases(8), library.homePlaylists(8),
        library.homeArtists(8), library.homeFavorites(6)
    ) { releases, playlists, artists, favorites ->
        HomeContent(
            releases = releases.map { it.asModel() },
            playlists = playlists.map { it.asModel() },
            artists = artists.map { it.asModel() },
            favorites = favorites.map { it.asModel() }
        )
    }

    suspend fun refresh() = libraryRepository.sync()
}
