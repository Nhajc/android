package com.nhajc.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhajc.android.data.model.Song
import com.nhajc.android.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SearchPlaybackViewModel @Inject constructor(private val playbackController: PlaybackController) : ViewModel() {
    fun play(songs: List<Song>, startIndex: Int) {
        viewModelScope.launch { playbackController.play(songs, startIndex) }
    }
}
