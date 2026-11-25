package com.nhajc.android.ui.settings

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nhajc.android.R
import com.nhajc.android.data.model.AudioQuality
import com.nhajc.android.data.model.ThemeMode
import com.nhajc.android.data.model.User
import com.nhajc.android.data.model.UserRole
import com.nhajc.android.ui.common.message
import com.nhajc.android.ui.theme.NhajcTheme
import kotlin.uuid.Uuid

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val avatarPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.onAvatarPicked(uri)
            }
        }

    SettingsScreen(
        uiState = uiState,
        onAudioQualityChange = viewModel::onAudioQualityChange,
        onDownloadQualityChange = viewModel::onDownloadQualityChange,
        onThemeModeChange = viewModel::onThemeModeChange,
        onLogout = viewModel::logout,
        onBack = onBack,
        onChangeAvatar = { avatarPicker.launch(arrayOf("image/jpeg", "image/png")) },
        onRemoveAvatar = viewModel::onRemoveAvatar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onAudioQualityChange: (AudioQuality) -> Unit,
    onDownloadQualityChange: (AudioQuality) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onChangeAvatar: () -> Unit,
    onRemoveAvatar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy = uiState.isLoggingOut

    val themeOptions = ThemeMode.entries.map { mode ->
        SettingsDropdownOption(mode, stringResource(themeTitle(mode)))
    }

    val qualityOptions = AudioQuality.entries.map { quality ->
        SettingsDropdownOption(
            quality,
            stringResource(qualityTitle(quality)),
            stringResource(qualityDescription(quality))
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !uiState.isLoggingOut
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SectionTitle(stringResource(R.string.account))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            avatarUrl = uiState.user?.avatarUrl,
                            enabled = !isBusy && !uiState.isUpdatingAvatar,
                            onChangeAvatar = onChangeAvatar,
                            onRemoveAvatar = onRemoveAvatar
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = uiState.user?.username.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = uiState.user?.email.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (uiState.error != null) {
                    Text(
                        text = stringResource(uiState.error.message()),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                SectionTitle(stringResource(R.string.appearance))

                SettingsDropdown(
                    label = stringResource(R.string.theme),
                    selectedValue = uiState.themeMode,
                    options = themeOptions,
                    leadingIcon = Icons.Outlined.Palette,
                    enabled = !isBusy,
                    onValueChange = onThemeModeChange
                )

                SectionTitle(stringResource(R.string.playback))

                SettingsDropdown(
                    label = stringResource(R.string.audio_quality),
                    selectedValue = uiState.audioQuality,
                    options = qualityOptions,
                    leadingIcon = Icons.Outlined.GraphicEq,
                    enabled = !isBusy,
                    onValueChange = onAudioQualityChange
                )

                SectionTitle(stringResource(R.string.downloads))

                SettingsDropdown(
                    label = stringResource(R.string.download_quality),
                    selectedValue = uiState.downloadQuality,
                    options = qualityOptions,
                    leadingIcon = Icons.Outlined.Download,
                    enabled = !isBusy,
                    onValueChange = onDownloadQualityChange
                )

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    enabled = !isBusy,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    if (uiState.isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null
                        )
                    }

                    Text(
                        text = if (uiState.isLoggingOut) {
                            stringResource(R.string.logging_out)
                        } else {
                            stringResource(R.string.log_out)
                        },
                        modifier = Modifier.padding(start = 12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class SettingsDropdownOption<T>(val value: T, val title: String, val description: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsDropdown(
    label: String,
    selectedValue: T?,
    options: List<SettingsDropdownOption<T>>,
    leadingIcon: ImageVector,
    enabled: Boolean,
    onValueChange: (T) -> Unit
) {
    val selectedOption = options.firstOrNull { option ->
        option.value == selectedValue
    }

    val canExpand = enabled && selectedOption != null
    var expanded by remember(canExpand) { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = canExpand && it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption?.title ?: stringResource(R.string.loading_selection),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = canExpand
                )
                .fillMaxWidth(),
            enabled = canExpand,
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(leadingIcon, contentDescription = null)
            },
            trailingIcon = {
                if (selectedOption == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            supportingText = if (selectedOption?.description == null) {
                null
            } else {
                { Text(selectedOption.description) }
            },
            shape = MaterialTheme.shapes.medium
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            for (option in options) {
                val isSelected = option.value == selectedValue

                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                }
                            )

                            if (option.description != null) {
                                Text(
                                    text = option.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false

                        if (!isSelected) {
                            onValueChange(option.value)
                        }
                    },
                    modifier = Modifier.semantics { selected = isSelected },
                    trailingIcon = {
                        Box(modifier = Modifier.size(24.dp)) {
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    enabled: Boolean,
    onChangeAvatar: () -> Unit,
    onRemoveAvatar: () -> Unit
) {
    var showMenu by remember(avatarUrl) { mutableStateOf(false) }

    val hasAvatar = !avatarUrl.isNullOrBlank()
    val description = stringResource(R.string.profile_picture)

    Box {
        Surface(
            onClick = {
                if (hasAvatar) {
                    showMenu = true
                } else {
                    onChangeAvatar()
                }
            },
            enabled = enabled,
            modifier = Modifier
                .size(72.dp)
                .semantics {
                    contentDescription = description
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(18.dp)
                )

                if (hasAvatar) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu && hasAvatar && enabled,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.change_photo)) },
                onClick = {
                    showMenu = false
                    onChangeAvatar()
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_photo)) },
                onClick = {
                    showMenu = false
                    onRemoveAvatar()
                },
                leadingIcon = {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )
}

@StringRes
private fun themeTitle(mode: ThemeMode): Int = when (mode) {
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.SYSTEM -> R.string.theme_system
}

@StringRes
private fun qualityTitle(quality: AudioQuality): Int = when (quality) {
    AudioQuality.LOW -> R.string.quality_low
    AudioQuality.MEDIUM -> R.string.quality_medium
    AudioQuality.HIGH -> R.string.quality_high
    AudioQuality.LOSSLESS -> R.string.quality_lossless
}

@StringRes
private fun qualityDescription(quality: AudioQuality): Int = when (quality) {
    AudioQuality.LOW -> R.string.quality_low_description
    AudioQuality.MEDIUM -> R.string.quality_medium_description
    AudioQuality.HIGH -> R.string.quality_high_description
    AudioQuality.LOSSLESS -> R.string.quality_lossless_description
}

@Preview(
    name = "Settings Light",
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Settings Dark",
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SettingsScreenPreview() {
    var uiState by remember {
        mutableStateOf(
            SettingsUiState(
                user = User(
                    id = Uuid.random(),
                    username = "nhajc",
                    email = "nhajc@example.com",
                    role = UserRole.USER,
                    avatarUrl = null
                ),
                audioQuality = AudioQuality.MEDIUM,
                downloadQuality = AudioQuality.LOSSLESS
            )
        )
    }

    NhajcTheme(darkTheme = isSystemInDarkTheme()) {
        SettingsScreen(
            uiState = uiState,
            onAudioQualityChange = {
                uiState = uiState.copy(audioQuality = it)
            },
            onDownloadQualityChange = {
                uiState = uiState.copy(downloadQuality = it)
            },
            onThemeModeChange = {
                uiState = uiState.copy(themeMode = it)
            },
            onLogout = {},
            onBack = {},
            onChangeAvatar = {},
            onRemoveAvatar = {}
        )
    }
}
