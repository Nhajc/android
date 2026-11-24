package com.nhajc.android.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.data.model.SearchCategory
import com.nhajc.android.data.repository.SearchRepository
import kotlin.uuid.Uuid

@Composable
fun SearchRoute(
    repository: SearchRepository,
    onOpenArtist: (Uuid) -> Unit,
    playbackViewModel: SearchPlaybackViewModel = hiltViewModel()
) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryNames by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val scope = rememberCoroutineScope()
    val controller = remember(repository, scope) {
        SearchController(repository, scope, query, categoryNames.map(SearchCategory::valueOf).toSet())
    }
    val model by controller.model.collectAsStateWithLifecycle()

    LaunchedEffect(controller) { controller.submit() }

    SearchScreen(
        model = model,
        onQueryChange = { query = it; controller.updateQuery(it) },
        onCategoryChange = {
            controller.selectCategory(it)
            categoryNames = controller.model.value.categories.map { category -> category.name }
        },
        onSubmit = controller::submit,
        onRetry = controller::retry,
        onLoadMore = controller::loadMore,
        onSongClick = { item ->
            val songs = model.items.mapNotNull { it.song }
            val index = songs.indexOfFirst { it.id == item.song?.id }

            if (index >= 0) {
                playbackViewModel.play(songs, index)
            }
        },
        onArtistClick = { item -> onOpenArtist(Uuid.parse(item.id)) }
    )
}
