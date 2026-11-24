package com.nhajc.android.data.model

enum class LibrarySort { RECENT, NAME, ARTIST, DOWNLOADED }

enum class ArtistSort { RECENT, NAME }

enum class DownloadSort { RECENT, NAME, ARTIST }

enum class LibraryTab { RELEASES, PLAYLISTS, ARTISTS, FAVORITES }

enum class LibraryView { LIST, GRID, COVERS }

data class LibrarySettings(
    val tab: LibraryTab = LibraryTab.RELEASES,
    val view: LibraryView = LibraryView.LIST,
    val releaseSort: LibrarySort = LibrarySort.RECENT,
    val playlistSort: LibrarySort = LibrarySort.RECENT,
    val artistSort: ArtistSort = ArtistSort.RECENT,
    val favoriteSort: LibrarySort = LibrarySort.RECENT,
    val downloadSort: DownloadSort = DownloadSort.RECENT
)
