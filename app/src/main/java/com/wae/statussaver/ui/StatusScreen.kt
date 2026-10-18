package com.wae.statussaver.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wae.statussaver.BuildConfig
import com.wae.statussaver.R
import com.wae.statussaver.data.StatusItem
import com.wae.statussaver.vm.StatusUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaeRoot(
    state: StatusUiState,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onSave: (StatusItem) -> Unit,
    onSaveAll: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onToggleAutoSave: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAllFilesSettings: () -> Unit,
    onResetIndex: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val msg = state.message
        if (msg != null) {
            snackbar.showSnackbar(msg)
            onMessageShown()
        }
    }

    if (!state.hasAccess) {
        PermissionGate(onRequest = onRequestPermissions)
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.tab == 0 && state.photos.isNotEmpty() || state.tab == 1 && state.videos.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onSaveAll,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_save_all)) }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = state.tab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = state.tab == 0,
                    onClick = { onSelectTab(0) },
                    text = { Text(stringResource(R.string.tab_images)) }
                )
                Tab(
                    selected = state.tab == 1,
                    onClick = { onSelectTab(1) },
                    text = { Text(stringResource(R.string.tab_videos)) }
                )
            }

            val items = if (state.tab == 0) state.photos else state.videos

            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                state.dirsFound == 0 -> EmptyState(
                    text = stringResource(R.string.health_no_dirs),
                    onOpenWhatsApp = onOpenWhatsApp
                )

                items.isEmpty() -> EmptyState(
                    text = stringResource(R.string.empty_state),
                    onOpenWhatsApp = onOpenWhatsApp
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items, key = { it.file.absolutePath }) { item ->
                        Thumb(item = item, onClick = { onSave(item) })
                    }
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SettingsSheet(
                state = state,
                onToggleAutoSave = onToggleAutoSave,
                onOpenBatterySettings = onOpenBatterySettings,
                onOpenAllFilesSettings = onOpenAllFilesSettings,
                onResetIndex = onResetIndex
            )
        }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.permission_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.permission_message),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest) { Text(stringResource(R.string.allow_access)) }
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.privacy_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(text: String, onOpenWhatsApp: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onOpenWhatsApp) {
            Text(stringResource(R.string.open_whatsapp))
        }
    }
}

@Composable
private fun Thumb(item: StatusItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = item.file,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (item.isVideo) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
            )
        }
        if (item.saved) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    state: StatusUiState,
    onToggleAutoSave: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAllFilesSettings: () -> Unit,
    onResetIndex: () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {

        SettingSwitch(
            title = stringResource(R.string.settings_auto_save),
            subtitle = stringResource(R.string.settings_auto_save_desc),
            checked = state.autoSave,
            onToggle = onToggleAutoSave
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        SettingRow(
            title = stringResource(R.string.settings_battery),
            subtitle = stringResource(R.string.settings_battery_desc),
            value = stringResource(
                if (state.batteryOk) R.string.settings_battery_ok
                else R.string.settings_battery_needed
            ),
            onClick = onOpenBatterySettings
        )

        if (state.needsAllFiles) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingRow(
                title = stringResource(R.string.settings_all_files),
                subtitle = stringResource(R.string.settings_all_files_desc),
                value = null,
                onClick = onOpenAllFilesSettings
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        SettingRow(
            title = stringResource(R.string.settings_clear_index),
            subtitle = stringResource(R.string.settings_clear_index_desc),
            value = null,
            onClick = onResetIndex
        )

        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    value: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
