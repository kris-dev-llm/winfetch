package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadKind
import com.example.data.model.DownloadPriority
import com.example.data.model.DownloadStatus
import com.example.engine.ArchiveManager
import com.example.engine.FormatUtils
import com.example.ui.TransferFilterTab
import com.example.ui.components.Win10ActionButton
import com.example.ui.components.Win10PriorityBadge
import com.example.ui.components.Win10ProgressBar
import com.example.ui.components.Win10SpeedGraph
import com.example.ui.components.Win10TagBadge
import com.example.ui.theme.LocalWin10Tokens
import com.example.ui.theme.WinMonoFamily
import java.io.File

@Composable
fun TransfersScreen(
    tasks: List<DownloadTaskEntity>,
    speedHistory: Map<String, List<Long>>,
    selectedFilter: TransferFilterTab,
    searchQuery: String,
    expandedIds: Set<String>,
    onSelectFilter: (TransferFilterTab) -> Unit,
    onSearchChange: (String) -> Unit,
    onNewDownload: () -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onClearCompleted: () -> Unit,
    onToggleDetails: (String) -> Unit,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onUpdatePriority: (String, DownloadPriority) -> Unit,
    onShareTask: (DownloadTaskEntity) -> Unit,
    onExtractTask: (DownloadTaskEntity) -> Unit,
    onOpenTask: (DownloadTaskEntity) -> Unit,
    onExportTaskToSaf: (DownloadTaskEntity, android.net.Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalWin10Tokens.current
    val context = LocalContext.current
    var pendingExportTask by remember { mutableStateOf<DownloadTaskEntity?>(null) }

    val safExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val task = pendingExportTask
        pendingExportTask = null
        if (uri != null && task != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            onExportTaskToSaf(task, uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Windows 10 Command Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.chromeBackground)
                .border(1.dp, tokens.border, RectangleShape)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Win10ActionButton(
                text = "+ New Transfer",
                primary = true,
                onClick = onNewDownload,
                testTag = "command_new_download"
            )
            Win10ActionButton(
                text = "Pause All",
                onClick = onPauseAll,
                testTag = "command_pause_all"
            )
            Win10ActionButton(
                text = "Resume All",
                onClick = onResumeAll,
                testTag = "command_resume_all"
            )
            Win10ActionButton(
                text = "Clear Done",
                onClick = onClearCompleted,
                testTag = "command_clear_done"
            )
        }

        // Pivot Tabs + Search Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.cardBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransferFilterTab.entries.forEach { tab ->
                    val selected = tab == selectedFilter
                    Column(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("filter_tab_${tab.name.lowercase()}")
                            .clickable { onSelectFilter(tab) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) tokens.accent else tokens.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(2.5.dp)
                                .background(if (selected) tokens.accent else Color.Transparent)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Filter transfers by name, URL, or folder...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RectangleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tokens.accent,
                    unfocusedBorderColor = tokens.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_transfers_input")
            )
        }

        HorizontalDivider(color = tokens.border, thickness = 1.dp)

        if (tasks.isEmpty()) {
            EmptyTransfersState(
                onNewDownload = onNewDownload,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Win10TransferCard(
                        task = task,
                        speedSamples = speedHistory[task.id].orEmpty(),
                        expanded = task.id in expandedIds,
                        onToggleDetails = { onToggleDetails(task.id) },
                        onPause = { onPauseTask(task.id) },
                        onResume = { onResumeTask(task.id) },
                        onCancel = { onCancelTask(task.id) },
                        onDelete = { onDeleteTask(task.id) },
                        onCyclePriority = {
                            val next = when (task.priority) {
                                DownloadPriority.CASUAL -> DownloadPriority.QUICK
                                DownloadPriority.QUICK -> DownloadPriority.URGENT
                                DownloadPriority.URGENT -> DownloadPriority.CASUAL
                            }
                            onUpdatePriority(task.id, next)
                        },
                        onSetPriority = { prio -> onUpdatePriority(task.id, prio) },
                        onShare = { onShareTask(task) },
                        onExtract = { onExtractTask(task) },
                        onOpen = { onOpenTask(task) },
                        onExport = {
                            pendingExportTask = task
                            safExportLauncher.launch(null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Win10TransferCard(
    task: DownloadTaskEntity,
    speedSamples: List<Long>,
    expanded: Boolean,
    onToggleDetails: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onCyclePriority: () -> Unit,
    onSetPriority: (DownloadPriority) -> Unit,
    onShare: () -> Unit,
    onExtract: () -> Unit,
    onOpen: () -> Unit,
    onExport: () -> Unit
) {
    val tokens = LocalWin10Tokens.current
    val kindIcon = when (task.kind) {
        DownloadKind.DIRECT_FILE -> Icons.Filled.InsertDriveFile
        DownloadKind.FOLDER_BUNDLE -> Icons.Filled.Folder
        DownloadKind.WEBSITE -> Icons.Filled.Language
        DownloadKind.TORRENT -> Icons.Filled.Hub
    }

    val isIndeterminate = task.status == DownloadStatus.CONNECTING ||
        task.status == DownloadStatus.PACKAGING ||
        task.status == DownloadStatus.EXTRACTING ||
        (task.status == DownloadStatus.DOWNLOADING && task.totalBytes <= 0L && task.assetsTotal <= 0)

    val percentText = when {
        task.status == DownloadStatus.COMPLETED -> "100%"
        task.totalBytes > 0L || (task.kind == DownloadKind.WEBSITE && task.assetsTotal > 0) ->
            "${(task.progressFraction * 100).toInt().coerceIn(0, 100)}%"
        else -> "--"
    }

    val canExtract = task.status == DownloadStatus.COMPLETED &&
        task.localPath.isNotBlank() &&
        File(task.localPath).let { it.isFile && ArchiveManager.isSupportedArchive(it.name) }

    Surface(
        shape = RectangleShape,
        color = tokens.cardBackground,
        border = BorderStroke(
            width = if (task.priority == DownloadPriority.URGENT && !task.status.isTerminal) 1.5.dp else 1.dp,
            color = if (task.priority == DownloadPriority.URGENT && !task.status.isTerminal) tokens.urgentColor else tokens.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transfer_card_${task.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Icon + Title + Priority Badge + Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(tokens.accent.copy(alpha = 0.12f), RectangleShape)
                        .border(1.dp, tokens.accent.copy(alpha = 0.35f), RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = kindIcon,
                        contentDescription = task.kind.label,
                        tint = tokens.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.sourceUrl.lines().firstOrNull().orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = WinMonoFamily,
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Win10PriorityBadge(
                    priority = task.priority,
                    onClick = onCyclePriority,
                    modifier = Modifier.testTag("badge_priority_${task.id}")
                )
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("share_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share ${task.title}",
                        tint = tokens.accent
                    )
                }
            }

            // Metadata Tags Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Win10TagBadge(text = task.kind.label.uppercase(), accent = true)
                if (task.kind == DownloadKind.WEBSITE) {
                    Win10TagBadge(text = task.siteArchiveFormat.label)
                    if (task.rewriteSiteRefs) Win10TagBadge(text = "REFS->DIR")
                }
                if (task.downloadIntoFolder) {
                    Win10TagBadge(text = "FOLDER")
                }
                if (task.extractOnDownload) {
                    Win10TagBadge(text = "AUTO-EXTRACT")
                }
                if (task.kind == DownloadKind.TORRENT && (task.peersConnected > 0 || task.seedsFound > 0)) {
                    Win10TagBadge(text = "${task.peersConnected} PEERS / ${task.seedsFound} SEEDS")
                }
            }

            // Windows 10 Progress Bar
            Win10ProgressBar(
                progress = task.progressFraction,
                status = task.status,
                indeterminate = isIndeterminate
            )

            // Telemetry Row: Status + % + Bytes + Speed + ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${task.status.label} ($percentText)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when (task.status) {
                        DownloadStatus.FAILED -> tokens.errorRed
                        DownloadStatus.PAUSED -> tokens.pausedAmber
                        DownloadStatus.COMPLETED -> tokens.transferGreen
                        else -> tokens.textPrimary
                    }
                )
                Text(
                    text = buildString {
                        append(FormatUtils.formatBytes(task.bytesDownloaded))
                        if (task.totalBytes > 0L) {
                            append(" / ")
                            append(FormatUtils.formatBytes(task.totalBytes))
                        }
                        if (task.status.isActive && task.speedBytesPerSec > 0L) {
                            append(" • ")
                            append(FormatUtils.formatSpeed(task.speedBytesPerSec))
                        }
                        if (task.status == DownloadStatus.DOWNLOADING && task.etaSeconds > 0L) {
                            append(" • ")
                            append(FormatUtils.formatEta(task.etaSeconds))
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (task.statusDetail.isNotBlank()) {
                Text(
                    text = task.statusDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.status == DownloadStatus.FAILED) tokens.errorRed else tokens.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Primary Inline Controls + "More details" Windows 10 toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.status.isActive || task.status == DownloadStatus.QUEUED) {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = tokens.textPrimary)
                        }
                    } else if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED || task.status == DownloadStatus.CANCELED) {
                        IconButton(onClick = onResume) {
                            Icon(
                                imageVector = if (task.status == DownloadStatus.FAILED) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
                                contentDescription = "Resume",
                                tint = tokens.accent
                            )
                        }
                    }
                    if (task.status == DownloadStatus.COMPLETED) {
                        IconButton(onClick = onOpen) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = "Open", tint = tokens.accent)
                        }
                    }
                    if (canExtract) {
                        IconButton(onClick = onExtract) {
                            Icon(Icons.Filled.FolderZip, contentDescription = "Extract archive", tint = tokens.textPrimary)
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = tokens.textSecondary)
                    }
                }

                // Windows 10 "More details" expander
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_details_${task.id}")
                        .clickable(onClick = onToggleDetails)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Fewer details" else "More details",
                        tint = tokens.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (expanded) "Fewer details" else "More details",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.accent
                    )
                }
            }

            // Expanded Windows 10 Transfer Graph & Priority/Path Details
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Win10SpeedGraph(
                        samples = speedSamples,
                        currentSpeedBytes = task.speedBytesPerSec,
                        status = task.status
                    )

                    if (!task.status.isTerminal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Priority:",
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.textSecondary
                            )
                            DownloadPriority.entries.forEach { prio ->
                                val selected = task.priority == prio
                                Surface(
                                    shape = RectangleShape,
                                    color = if (selected) tokens.accent else tokens.chromeBackground,
                                    border = BorderStroke(1.dp, if (selected) tokens.accent else tokens.border),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 40.dp)
                                        .clickable { onSetPriority(prio) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = prio.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) Color.White else tokens.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (task.localPath.isNotBlank()) {
                        Text(
                            text = "Path: ${task.localPath}",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textSecondary
                        )
                    }
                    if (!task.extractedPath.isNullOrBlank()) {
                        Text(
                            text = "Extracted: ${task.extractedPath}",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.transferGreen
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (task.status == DownloadStatus.COMPLETED) {
                            Win10ActionButton(text = "Share", primary = true, onClick = onShare)
                            Win10ActionButton(text = "Export Folder", onClick = onExport)
                            if (canExtract) {
                                Win10ActionButton(text = "Extract Now", onClick = onExtract)
                            }
                        } else if (!task.status.isTerminal) {
                            Win10ActionButton(text = "Cancel", onClick = onCancel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTransfersState(
    onNewDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalWin10Tokens.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(tokens.accent.copy(alpha = 0.12f), RectangleShape)
                .border(1.dp, tokens.accent, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Archive,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "No Active Transfers",
            style = MaterialTheme.typography.headlineSmall,
            color = tokens.textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Download files directly, organize into folders, save offline websites (ZIP, DIR, or 7Z), or fetch torrents with Casual, Quick, or URGENT priority.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Win10ActionButton(
            text = "+ Start New Transfer",
            primary = true,
            onClick = onNewDownload,
            testTag = "empty_new_download_button"
        )
    }
}
