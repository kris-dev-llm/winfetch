package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.FormatUtils
import com.example.ui.FileBrowserEntry
import com.example.ui.components.Win10ActionButton
import com.example.ui.components.Win10TagBadge
import com.example.ui.theme.LocalWin10Tokens
import com.example.ui.theme.WinMonoFamily
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilesBrowserScreen(
    currentDir: File,
    isAtRoot: Boolean,
    entries: List<FileBrowserEntry>,
    previewHtmlFile: File?,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onOpenEntry: (FileBrowserEntry) -> Unit,
    onExtractEntry: (FileBrowserEntry) -> Unit,
    onShareEntry: (FileBrowserEntry) -> Unit,
    onDeleteEntry: (FileBrowserEntry) -> Unit,
    onClosePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalWin10Tokens.current

    BackHandler(enabled = !isAtRoot || previewHtmlFile != null) {
        if (previewHtmlFile != null) {
            onClosePreview()
        } else {
            onNavigateUp()
        }
    }

    if (previewHtmlFile != null) {
        OfflineSitePreviewDialog(
            htmlFile = previewHtmlFile,
            onDismiss = onClosePreview
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Windows 10 Explorer Address & Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.chromeBackground)
                .border(1.dp, tokens.border, RectangleShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateUp,
                enabled = !isAtRoot,
                modifier = Modifier.testTag("files_up_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Up one folder",
                    tint = if (isAtRoot) tokens.textSecondary.copy(alpha = 0.4f) else tokens.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (tokens.isDark) Color(0xFF141414) else Color.White,
                        RectangleShape
                    )
                    .border(1.dp, tokens.border, RectangleShape)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = currentDir.absolutePath.substringAfter("WinFetch", currentDir.name).let {
                        "WinFetch$it"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("files_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh files",
                    tint = tokens.accent
                )
            }
        }

        HorizontalDivider(color = tokens.border, thickness = 1.dp)

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = tokens.textSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This folder is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completed files, saved sites, extracted folders, and torrents appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.file.absolutePath }) { entry ->
                    FileExplorerRow(
                        entry = entry,
                        onOpen = { onOpenEntry(entry) },
                        onExtract = { onExtractEntry(entry) },
                        onShare = { onShareEntry(entry) },
                        onDelete = { onDeleteEntry(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileExplorerRow(
    entry: FileBrowserEntry,
    onOpen: () -> Unit,
    onExtract: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val tokens = LocalWin10Tokens.current
    val icon = when {
        entry.isHtmlSiteEntry -> Icons.Filled.Language
        entry.isDirectory -> Icons.Filled.Folder
        entry.isArchive -> Icons.Filled.FolderZip
        else -> Icons.Filled.InsertDriveFile
    }
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(entry.lastModified))

    Surface(
        shape = RectangleShape,
        color = tokens.cardBackground,
        border = BorderStroke(1.dp, tokens.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = entry.name,
                tint = if (entry.isDirectory || entry.isHtmlSiteEntry) tokens.accent else tokens.textPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (entry.isDirectory) {
                            "${entry.childCount} item(s) • ${FormatUtils.formatBytes(entry.sizeBytes)}"
                        } else {
                            "${FormatUtils.formatBytes(entry.sizeBytes)} • $dateStr"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = WinMonoFamily,
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.isHtmlSiteEntry) {
                        Win10TagBadge(text = "SITE", accent = true)
                    } else if (entry.isArchive) {
                        Win10TagBadge(text = entry.file.extension.uppercase())
                    }
                }
            }

            if (entry.isArchive) {
                IconButton(onClick = onExtract) {
                    Icon(
                        imageVector = Icons.Filled.FolderZip,
                        contentDescription = "Extract ${entry.name}",
                        tint = tokens.accent
                    )
                }
            }
            if (entry.isHtmlSiteEntry) {
                IconButton(onClick = onOpen) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = "Preview site",
                        tint = tokens.accent
                    )
                }
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share ${entry.name}",
                    tint = tokens.accent
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete ${entry.name}",
                    tint = tokens.textSecondary
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OfflineSitePreviewDialog(
    htmlFile: File,
    onDismiss: () -> Unit
) {
    val tokens = LocalWin10Tokens.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RectangleShape,
            color = tokens.cardBackground,
            border = BorderStroke(1.5.dp, tokens.accent),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(620.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.accent)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline Site Preview — ${htmlFile.parentFile?.name ?: htmlFile.name}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close preview", tint = Color.White)
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            settings.allowFileAccess = true
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            loadUrl("file://${htmlFile.absolutePath}")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.chromeBackground)
                        .border(1.dp, tokens.border, RectangleShape)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = htmlFile.absolutePath,
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Win10ActionButton(text = "Close", primary = true, onClick = onDismiss)
                }
            }
        }
    }
}
