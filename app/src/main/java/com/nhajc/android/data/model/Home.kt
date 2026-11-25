package com.nhajc.android.data.model

data class HomeContent(
    val releases: List<Release> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val artists: List<ArtistSummary> = emptyList(),
    val favorites: List<Song> = emptyList()
) {
    val isEmpty: Boolean
        get() = releases.isEmpty() && playlists.isEmpty() && artists.isEmpty() && favorites.isEmpty()
}
