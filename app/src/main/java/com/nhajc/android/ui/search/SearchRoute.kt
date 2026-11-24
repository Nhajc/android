package com.nhajc.android.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhajc.android.data.model.SearchCategory
import com.nhajc.android.data.repository.SearchSession

@Composable
fun SearchRoute(session: SearchSession) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf(SearchCategory.ALL.name) }
    var showConnection by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val controller = remember(session, scope) {
        SearchController(session, scope, query, SearchCategory.valueOf(categoryName))
    }
    val model by controller.model.collectAsStateWithLifecycle()

    LaunchedEffect(controller) { controller.submit() }

    SearchScreen(
        model = model,
        onQueryChange = { query = it; controller.updateQuery(it) },
        onCategoryChange = { categoryName = it.name; controller.selectCategory(it) },
        onSubmit = controller::submit,
        onRetry = controller::retry,
        onLoadMore = controller::loadMore,
        onConnectionClick = { showConnection = true }
    )

    if (showConnection) {
        SearchConnectionDialog(
            model = model,
            onDismiss = { controller.cancelConnection(); showConnection = false },
            onUseSample = { controller.useSample(); showConnection = false },
            onConnect = { address, username, password ->
                controller.connect(address, username, password) { showConnection = false }
            }
        )
    }
}
