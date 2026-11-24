package com.nhajc.android.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nhajc.android.data.model.SearchCategory
import com.nhajc.android.data.model.SearchError
import com.nhajc.android.data.model.SearchItemViewModel
import com.nhajc.android.data.model.SearchSource
import com.nhajc.android.data.model.SearchViewModel
import com.nhajc.android.ui.theme.NhajcTheme

// Pure view: all data arrives as presentation models; no DTO, API or entity imports.
@Composable
fun SearchScreen(
    model: SearchViewModel,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (SearchCategory) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onConnectionClick: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(modifier = Modifier.fillMaxSize().imePadding()) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("NHAJC", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Tìm kiếm", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onConnectionClick) {
                    Icon(Icons.Default.Tune, contentDescription = "Chọn nguồn dữ liệu")
                }
            }
            AssistChip(
                onClick = onConnectionClick,
                label = {
                    Text(
                        if (model.connection.source == SearchSource.SAMPLE) "Đang dùng dữ liệu mẫu"
                        else "Backend · ${model.connection.username.orEmpty()}",
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.CloudQueue, null, Modifier.size(16.dp))
                }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = model.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Bài hát, album, playlist, nghệ sĩ") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (model.query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, "Xóa từ khóa")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                isError = model.query.trim().length > 100,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onSubmit() })
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchCategory.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = model.category == category,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category.label()) }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    model.isLoading -> item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Đang tìm kiếm…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    !model.hasSearched -> item { SearchWelcome(onQueryChange) }
                    else -> {
                        if (model.items.isNotEmpty()) {
                            item {
                                Text("Kết quả cho “${model.query.trim()}”",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text("${model.items.size} kết quả đã tải",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                            }
                            items(model.items, key = { "${it.category}:${it.id}" }) { item ->
                                SearchResultRow(item)
                            }
                        }
                        if (model.error != null) item {
                            SearchMessage(
                                title = "Chưa thể tìm kiếm",
                                description = model.error.message(),
                                onRetry = if (model.error == SearchError.INVALID_QUERY) null else onRetry,
                                onConnectionClick = if (model.error == SearchError.UNAUTHORIZED) onConnectionClick else null
                            )
                        }
                        else if (model.items.isEmpty()) item {
                            SearchMessage("Không có kết quả", "Thử một tên khác hoặc chọn loại nội dung khác.")
                        }
                        if (model.hasNext && model.error == null) item {
                            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                                if (model.isLoadingMore) CircularProgressIndicator(Modifier.size(28.dp))
                                else OutlinedButton(onClick = onLoadMore) { Text("Xem thêm kết quả") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchWelcome(onSuggestionClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Headphones, null, Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                Text("Bạn muốn nghe gì?", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text("Tìm một giai điệu quen, một album yêu thích, hay nghệ sĩ bạn đang muốn nghe.",
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Thử tìm theo tên", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            listOf("Lạc trôi", "Sơn Tùng M-TP", "Chill").forEach { suggestion ->
                TextButton(onClick = { onSuggestionClick(suggestion) }) {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                    Text(suggestion, Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: SearchItemViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(64.dp)
                .clip(if (item.category == SearchCategory.ARTIST) CircleShape else RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.category.icon(), null, Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer)
            if (!item.imageUrl.isNullOrBlank()) AsyncImage(
                model = item.imageUrl, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.category.label(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            Text(item.title, style = MaterialTheme.typography.titleSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val detail = when (item.category) {
                SearchCategory.SONG -> item.artists.joinToString(", ")
                SearchCategory.RELEASE -> listOfNotNull(
                    item.artists.joinToString(", ").takeIf { it.isNotBlank() },
                    item.songCount?.let { "$it bài hát" }
                ).joinToString(" · ")
                SearchCategory.PLAYLIST -> listOfNotNull(item.ownerName,
                    item.songCount?.let { "$it bài hát" }).joinToString(" · ")
                else -> ""
            }
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        item.durationSeconds?.let { duration ->
            Text("${duration / 60}:${(duration % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchMessage(
    title: String,
    description: String,
    onRetry: (() -> Unit)? = null,
    onConnectionClick: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(description, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (onConnectionClick != null) Button(onClick = onConnectionClick) { Text("Đăng nhập lại") }
        else if (onRetry != null) OutlinedButton(onClick = onRetry) { Text("Thử lại") }
    }
}

internal fun SearchCategory.label(): String = when (this) {
    SearchCategory.ALL -> "Tất cả"
    SearchCategory.SONG -> "Bài hát"
    SearchCategory.RELEASE -> "Album"
    SearchCategory.PLAYLIST -> "Playlist"
    SearchCategory.ARTIST -> "Nghệ sĩ"
}

private fun SearchCategory.icon(): ImageVector = when (this) {
    SearchCategory.ALL, SearchCategory.SONG -> Icons.Default.MusicNote
    SearchCategory.RELEASE -> Icons.Default.Album
    SearchCategory.PLAYLIST -> Icons.AutoMirrored.Filled.QueueMusic
    SearchCategory.ARTIST -> Icons.Default.Person
}

internal fun SearchError.message(): String = when (this) {
    SearchError.NETWORK -> "Không kết nối được server. Kiểm tra kết nối mạng và địa chỉ backend."
    SearchError.UNAUTHORIZED -> "Phiên đăng nhập đã hết hạn hoặc không có quyền truy cập. Hãy đăng nhập lại."
    SearchError.INVALID_QUERY -> "Từ khóa cần có từ 1 đến 100 ký tự."
    SearchError.INVALID_ADDRESS -> "Nhập địa chỉ HTTP hoặc HTTPS hợp lệ, ví dụ http://10.0.2.2:8080/api/v1/."
    SearchError.INVALID_RESPONSE -> "Dữ liệu server trả về chưa đúng định dạng ứng dụng hỗ trợ."
    SearchError.SERVER -> "Server chưa xử lý được yêu cầu. Vui lòng thử lại sau."
    SearchError.UNKNOWN -> "Có lỗi xảy ra. Vui lòng thử lại."
}

@Preview(showBackground = true)
@Composable
private fun SearchWelcomePreview() {
    NhajcTheme { SearchScreen(SearchViewModel(), {}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun SearchResultsPreview() {
    NhajcTheme {
        SearchScreen(
            SearchViewModel(query = "Lạc", hasSearched = true, items = listOf(
                SearchItemViewModel("1", SearchCategory.SONG, "Lạc trôi",
                    artists = listOf("Sơn Tùng M-TP"), durationSeconds = 235),
                SearchItemViewModel("2", SearchCategory.PLAYLIST, "Lạc vào giai điệu",
                    ownerName = "Nhajc", songCount = 18)
            )), {}, {}, {}, {}, {}, {}
        )
    }
}
