package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DownloadKind
import com.example.data.model.DownloadPriority
import com.example.data.model.SiteArchiveFormat
import com.example.data.model.SpeedUnit
import com.example.ui.NewDownloadDraft
import com.example.ui.theme.LocalWin10Tokens
import com.example.ui.theme.WinMonoFamily
import java.io.File

@Composable
fun NewDownloadDialog(
    draft: NewDownloadDraft,
    availableFolders: List<File>,
    onDraftChange: ((NewDownloadDraft) -> NewDownloadDraft) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalWin10Tokens.current

    val torrentFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected.torrent"
            onDraftChange {
                it.copy(
                    url = uri.toString(),
                    title = if (it.title.isBlank()) displayName else it.title,
                    kind = DownloadKind.TORRENT
                )
            }
        }
    }

    val safFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val label = treeUri.lastPathSegment?.substringAfterLast(':') ?: "Selected Folder"
            onDraftChange {
                it.copy(
                    safTreeUri = treeUri.toString(),
                    safFolderLabel = label
                )
            }
        }
    }

    val isGitMode = draft.kind == DownloadKind.GIT_DOWNLOAD || draft.kind == DownloadKind.GIT_UPLOAD

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RectangleShape,
            color = tokens.cardBackground,
            border = BorderStroke(1.5.dp, tokens.accent),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 700.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Windows 10 Blue Window Title Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.accent)
                        .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (draft.kind == DownloadKind.GIT_UPLOAD) Icons.Filled.CloudUpload else Icons.Filled.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (draft.kind == DownloadKind.GIT_UPLOAD) "New Git Push (Upload)" else "New Transfer",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_new_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Transfer Type Selector Tabs (File / Folder / Site / Torrent / Git)
                    Text(
                        text = "TRANSFER MODE",
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        ModeTile(
                            label = "File",
                            icon = Icons.Filled.Download,
                            selected = draft.kind == DownloadKind.DIRECT_FILE,
                            onClick = { onDraftChange { it.copy(kind = DownloadKind.DIRECT_FILE) } },
                            modifier = Modifier.weight(1f),
                            testTag = "mode_file"
                        )
                        ModeTile(
                            label = "Folder",
                            icon = Icons.Filled.CreateNewFolder,
                            selected = draft.kind == DownloadKind.FOLDER_BUNDLE,
                            onClick = {
                                onDraftChange {
                                    it.copy(
                                        kind = DownloadKind.FOLDER_BUNDLE,
                                        downloadIntoFolder = true
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "mode_folder"
                        )
                        ModeTile(
                            label = "Site",
                            icon = Icons.Filled.Language,
                            selected = draft.kind == DownloadKind.WEBSITE,
                            onClick = { onDraftChange { it.copy(kind = DownloadKind.WEBSITE) } },
                            modifier = Modifier.weight(1f),
                            testTag = "mode_site"
                        )
                        ModeTile(
                            label = "Torrent",
                            icon = Icons.Filled.Hub,
                            selected = draft.kind == DownloadKind.TORRENT,
                            onClick = { onDraftChange { it.copy(kind = DownloadKind.TORRENT) } },
                            modifier = Modifier.weight(1f),
                            testTag = "mode_torrent"
                        )
                        ModeTile(
                            label = "Git",
                            icon = Icons.Filled.AccountTree,
                            selected = isGitMode,
                            onClick = {
                                onDraftChange {
                                    it.copy(
                                        kind = if (isGitMode) it.kind else DownloadKind.GIT_DOWNLOAD,
                                        downloadIntoFolder = true
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "mode_git"
                        )
                    }

                    // Git Direction Toggle (Download Clone/Pull vs Upload Commit/Push)
                    if (isGitMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RectangleShape,
                                color = if (draft.kind == DownloadKind.GIT_DOWNLOAD) tokens.accent else tokens.chromeBackground,
                                border = BorderStroke(1.dp, if (draft.kind == DownloadKind.GIT_DOWNLOAD) tokens.accent else tokens.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("git_direction_download")
                                    .clickable { onDraftChange { it.copy(kind = DownloadKind.GIT_DOWNLOAD) } }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Download (Clone / Pull)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (draft.kind == DownloadKind.GIT_DOWNLOAD) Color.White else tokens.textPrimary
                                    )
                                }
                            }
                            Surface(
                                shape = RectangleShape,
                                color = if (draft.kind == DownloadKind.GIT_UPLOAD) tokens.accent else tokens.chromeBackground,
                                border = BorderStroke(1.dp, if (draft.kind == DownloadKind.GIT_UPLOAD) tokens.accent else tokens.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("git_direction_upload")
                                    .clickable { onDraftChange { it.copy(kind = DownloadKind.GIT_UPLOAD) } }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Upload (Commit & Push)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (draft.kind == DownloadKind.GIT_UPLOAD) Color.White else tokens.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Source URL / Remote Git URL Input
                    val urlLabel = when (draft.kind) {
                        DownloadKind.DIRECT_FILE -> "Direct File URL (https://...)"
                        DownloadKind.FOLDER_BUNDLE -> "File URL(s) — one URL per line for folder batch"
                        DownloadKind.WEBSITE -> "Website URL (https://example.com)"
                        DownloadKind.TORRENT -> "Magnet URI, .torrent URL, or pick .torrent file"
                        DownloadKind.GIT_DOWNLOAD -> "Remote Git Repo URL (https://github.com/user/repo.git)"
                        DownloadKind.GIT_UPLOAD -> "Target Remote Git URL (https://github.com/user/repo.git)"
                    }
                    OutlinedTextField(
                        value = draft.url,
                        onValueChange = { newVal -> onDraftChange { it.copy(url = newVal) } },
                        label = { Text(urlLabel) },
                        singleLine = draft.kind != DownloadKind.FOLDER_BUNDLE,
                        maxLines = if (draft.kind == DownloadKind.FOLDER_BUNDLE) 4 else 2,
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.accent,
                            unfocusedBorderColor = tokens.border
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = WinMonoFamily),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Win10ActionButton(
                            text = "Paste Clipboard",
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = cm.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                if (!clip.isNullOrBlank()) {
                                    onDraftChange { it.copy(url = clip) }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "paste_clipboard_button"
                        )
                        if (draft.kind == DownloadKind.TORRENT) {
                            Win10ActionButton(
                                text = "Browse .torrent",
                                onClick = {
                                    torrentFilePicker.launch(arrayOf("application/x-bittorrent", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "pick_torrent_file_button"
                            )
                        }
                    }

                    // Git Specific Options (Download OR Upload)
                    if (isGitMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(tokens.chromeBackground, RectangleShape)
                                .border(1.dp, tokens.border, RectangleShape)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (draft.kind == DownloadKind.GIT_UPLOAD) "GIT UPLOAD (STAGE, COMMIT & PUSH)" else "GIT CLONE / PULL OPTIONS",
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.accent
                            )

                            if (draft.kind == DownloadKind.GIT_UPLOAD) {
                                Text(
                                    text = "Source Folder to Upload:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.textSecondary
                                )
                                if (availableFolders.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        availableFolders.forEach { folder ->
                                            val selected = draft.localSourcePath == folder.absolutePath
                                            Surface(
                                                shape = RectangleShape,
                                                color = if (selected) tokens.accent else tokens.cardBackground,
                                                border = BorderStroke(1.dp, if (selected) tokens.accent else tokens.border),
                                                modifier = Modifier
                                                    .defaultMinSize(minHeight = 40.dp)
                                                    .clickable {
                                                        onDraftChange {
                                                            it.copy(
                                                                localSourcePath = folder.absolutePath,
                                                                folderName = folder.name,
                                                                safTreeUri = null,
                                                                safFolderLabel = null
                                                            )
                                                        }
                                                    }
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = folder.name,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (selected) Color.White else tokens.textPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Win10ActionButton(
                                        text = "Pick Device Folder (SAF)",
                                        onClick = { safFolderPicker.launch(null) },
                                        modifier = Modifier.weight(1f),
                                        testTag = "git_pick_saf_folder"
                                    )
                                    val chosenLabel = draft.safFolderLabel ?: File(draft.localSourcePath).name.takeIf { draft.localSourcePath.isNotBlank() }
                                    if (chosenLabel != null) {
                                        Win10TagBadge(text = chosenLabel, accent = true)
                                    }
                                }

                                OutlinedTextField(
                                    value = draft.gitCommitMessage,
                                    onValueChange = { v -> onDraftChange { it.copy(gitCommitMessage = v) } },
                                    label = { Text("Commit Message") },
                                    singleLine = true,
                                    shape = RectangleShape,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            OutlinedTextField(
                                value = draft.gitBranch,
                                onValueChange = { v -> onDraftChange { it.copy(gitBranch = v) } },
                                label = { Text("Branch (optional, e.g. main)") },
                                singleLine = true,
                                shape = RectangleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("git_branch_input")
                            )

                            if (draft.kind == DownloadKind.GIT_DOWNLOAD) {
                                Win10CheckboxRow(
                                    checked = draft.gitShallow,
                                    onCheckedChange = { c -> onDraftChange { it.copy(gitShallow = c) } },
                                    label = "Shallow clone (--depth 1)",
                                    subtitle = "Downloads latest snapshot faster"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = draft.gitUsername,
                                    onValueChange = { v -> onDraftChange { it.copy(gitUsername = v) } },
                                    label = { Text("Username (optional)") },
                                    singleLine = true,
                                    shape = RectangleShape,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = draft.gitToken,
                                    onValueChange = { v -> onDraftChange { it.copy(gitToken = v) } },
                                    label = { Text("PAT / Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    shape = RectangleShape,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Priority Selector (Casual, Quick, URGENT)
                    Text(
                        text = "PRIORITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DownloadPriority.entries.forEach { prio ->
                            val selected = draft.priority == prio
                            val color = when (prio) {
                                DownloadPriority.URGENT -> tokens.urgentColor
                                DownloadPriority.QUICK -> tokens.quickColor
                                DownloadPriority.CASUAL -> tokens.casualColor
                            }
                            Surface(
                                shape = RectangleShape,
                                color = if (selected) color else tokens.chromeBackground,
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) color else tokens.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("priority_${prio.name.lowercase()}")
                                    .clickable { onDraftChange { it.copy(priority = prio) } }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = prio.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (prio == DownloadPriority.URGENT || selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else tokens.textPrimary
                                    )
                                    Text(
                                        text = when (prio) {
                                            DownloadPriority.CASUAL -> "Background"
                                            DownloadPriority.QUICK -> "Standard"
                                            DownloadPriority.URGENT -> "Express"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.White.copy(alpha = 0.88f) else tokens.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Site Mirror Format & Reference Rewriting Options
                    if (draft.kind == DownloadKind.WEBSITE) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(tokens.chromeBackground, RectangleShape)
                                .border(1.dp, tokens.border, RectangleShape)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SITE ARCHIVE FORMAT (DEFAULT: ZIP)",
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.textSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SiteArchiveFormat.entries.forEach { fmt ->
                                    val selected = draft.siteFormat == fmt
                                    Surface(
                                        shape = RectangleShape,
                                        color = if (selected) tokens.accent else tokens.cardBackground,
                                        border = BorderStroke(1.dp, if (selected) tokens.accent else tokens.border),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("site_format_${fmt.name.lowercase()}")
                                            .clickable { onDraftChange { it.copy(siteFormat = fmt) } }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (fmt == SiteArchiveFormat.ZIP) "ZIP (Default)" else fmt.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) Color.White else tokens.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Win10CheckboxRow(
                                checked = draft.rewriteSiteRefs,
                                onCheckedChange = { checked ->
                                    onDraftChange { it.copy(rewriteSiteRefs = checked) }
                                },
                                label = "Rewrite image & CSS refs to local dir",
                                subtitle = "Updates HTML <img src> and CSS url() to relative ./images/ paths",
                                testTag = "checkbox_rewrite_refs"
                            )

                            Win10CheckboxRow(
                                checked = draft.includeSiteScripts,
                                onCheckedChange = { checked ->
                                    onDraftChange { it.copy(includeSiteScripts = checked) }
                                },
                                label = "Bundle external <script> files",
                                subtitle = "Downloads linked JS files into ./js/ alongside images",
                                testTag = "checkbox_include_scripts"
                            )
                        }
                    }

                    // Per-Transfer Speed Cap Override (Kb / KB / KiB)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(tokens.chromeBackground, RectangleShape)
                            .border(1.dp, tokens.border, RectangleShape)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Win10CheckboxRow(
                            checked = draft.customSpeedLimitEnabled,
                            onCheckedChange = { c -> onDraftChange { it.copy(customSpeedLimitEnabled = c) } },
                            label = "Custom speed limit for this transfer",
                            subtitle = "Supports Kb (bits), KB (bytes), KiB — overrides global cap",
                            testTag = "checkbox_custom_speed_limit"
                        )
                        if (draft.customSpeedLimitEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = draft.customSpeedLimitValue,
                                    onValueChange = { v ->
                                        onDraftChange { it.copy(customSpeedLimitValue = v.filter { ch -> ch.isDigit() }.take(7)) }
                                    },
                                    label = { Text("Rate") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RectangleShape,
                                    modifier = Modifier.weight(1.2f)
                                )
                                SpeedUnit.primaryUnits.forEach { u ->
                                    val selected = draft.customSpeedLimitUnit == u
                                    Surface(
                                        shape = RectangleShape,
                                        color = if (selected) tokens.accent else tokens.cardBackground,
                                        border = BorderStroke(1.dp, if (selected) tokens.accent else tokens.border),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .clickable { onDraftChange { it.copy(customSpeedLimitUnit = u) } }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = u.shortLabel,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) Color.White else tokens.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Folder & Extract Options (for download modes)
                    if (draft.kind != DownloadKind.GIT_UPLOAD) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(tokens.chromeBackground, RectangleShape)
                                .border(1.dp, tokens.border, RectangleShape)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Win10CheckboxRow(
                                checked = draft.downloadIntoFolder || draft.kind == DownloadKind.FOLDER_BUNDLE || draft.kind == DownloadKind.GIT_DOWNLOAD,
                                onCheckedChange = { checked ->
                                    onDraftChange { it.copy(downloadIntoFolder = checked) }
                                },
                                label = "Download into dedicated folder",
                                subtitle = "Organizes output inside a named subfolder",
                                testTag = "checkbox_folder"
                            )

                            if (draft.downloadIntoFolder || draft.kind == DownloadKind.FOLDER_BUNDLE || draft.kind == DownloadKind.WEBSITE || draft.kind == DownloadKind.GIT_DOWNLOAD) {
                                OutlinedTextField(
                                    value = draft.folderName,
                                    onValueChange = { valStr -> onDraftChange { it.copy(folderName = valStr) } },
                                    label = { Text("Folder / Output Name (optional)") },
                                    singleLine = true,
                                    shape = RectangleShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("folder_name_input")
                                )
                            }

                            if (draft.kind != DownloadKind.GIT_DOWNLOAD) {
                                Win10CheckboxRow(
                                    checked = draft.extractOnDownload,
                                    onCheckedChange = { checked ->
                                        onDraftChange { it.copy(extractOnDownload = checked) }
                                    },
                                    label = "Extract on downloaded",
                                    subtitle = "Automatically unpack .zip, .7z, .tar, or .tar.gz when finished",
                                    testTag = "checkbox_extract"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Win10ActionButton(
                                    text = "Also Copy to Device Folder",
                                    onClick = { safFolderPicker.launch(null) },
                                    modifier = Modifier.weight(1f),
                                    testTag = "pick_saf_folder_button"
                                )
                                if (draft.safFolderLabel != null) {
                                    Win10TagBadge(
                                        text = draft.safFolderLabel,
                                        accent = true
                                    )
                                }
                            }
                        }
                    }
                }

                // Dialog Footer Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.chromeBackground)
                        .border(1.dp, tokens.border, RectangleShape)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Win10ActionButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        testTag = "cancel_new_download"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Win10ActionButton(
                        text = if (draft.kind == DownloadKind.GIT_UPLOAD) "Commit & Push" else "Start Transfer",
                        primary = true,
                        onClick = onSubmit,
                        testTag = "submit_new_download"
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val tokens = LocalWin10Tokens.current
    Surface(
        shape = RectangleShape,
        color = if (selected) tokens.accent else tokens.chromeBackground,
        border = BorderStroke(1.dp, if (selected) tokens.accentPressed else tokens.border),
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else tokens.textPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else tokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
