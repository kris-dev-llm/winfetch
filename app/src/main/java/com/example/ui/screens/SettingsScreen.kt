package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadPriority
import com.example.data.model.SiteArchiveFormat
import com.example.data.prefs.WinFetchSettings
import com.example.engine.StoragePaths
import com.example.ui.components.Win10CheckboxRow
import com.example.ui.theme.LocalWin10Tokens
import com.example.ui.theme.WinMonoFamily
import com.example.ui.theme.WinThemeMode

@Composable
fun SettingsScreen(
    settings: WinFetchSettings,
    onThemeModeChange: (WinThemeMode) -> Unit,
    onMaxConcurrentChange: (Int) -> Unit,
    onDefaultPriorityChange: (DownloadPriority) -> Unit,
    onDefaultSiteFormatChange: (SiteArchiveFormat) -> Unit,
    onDefaultRewriteRefsChange: (Boolean) -> Unit,
    onDefaultExtractChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalWin10Tokens.current
    val context = LocalContext.current
    val downloadsPath = StoragePaths.getDownloadsRoot(context).absolutePath

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "WinFetch Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = tokens.textPrimary
        )

        SettingsBlock(title = "APPEARANCE (WINDOWS 10 THEME)") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WinThemeMode.entries.forEach { mode ->
                    val selected = settings.themeMode == mode
                    Surface(
                        shape = RectangleShape,
                        color = if (selected) tokens.accent else tokens.chromeBackground,
                        border = BorderStroke(1.dp, if (selected) tokens.accent else tokens.border),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("theme_mode_${mode.name.lowercase()}")
                            .clickable { onThemeModeChange(mode) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else tokens.textPrimary
                            )
                        }
                    }
                }
            }
        }

        SettingsBlock(title = "PRIORITY & CONCURRENCY") {
            Text(
                text = "Max parallel transfers (${settings.maxConcurrentDownloads}) — URGENT transfers get express bypass",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..5).forEach { count ->
                    val selected = settings.maxConcurrentDownloads == count
                    Surface(
                        shape = RectangleShape,
                        color = if (selected) tokens.accent else tokens.chromeBackground,
                        border = BorderStroke(1.dp, if (selected) tokens.accent else tokens.border),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onMaxConcurrentChange(count) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else tokens.textPrimary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Default Transfer Priority",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DownloadPriority.entries.forEach { prio ->
                    val selected = settings.defaultPriority == prio
                    Surface(
                        shape = RectangleShape,
                        color = if (selected) tokens.accent else tokens.chromeBackground,
                        border = BorderStroke(1.dp, if (selected) tokens.accent else tokens.border),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onDefaultPriorityChange(prio) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = prio.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else tokens.textPrimary
                            )
                        }
                    }
                }
            }
        }

        SettingsBlock(title = "WEBSITE SAVER & ARCHIVE DEFAULTS") {
            Text(
                text = "Default Site Output Format (ZIP is default)",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SiteArchiveFormat.entries.forEach { fmt ->
                    val selected = settings.defaultSiteFormat == fmt
                    Surface(
                        shape = RectangleShape,
                        color = if (selected) tokens.accent else tokens.chromeBackground,
                        border = BorderStroke(1.dp, if (selected) tokens.accent else tokens.border),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onDefaultSiteFormatChange(fmt) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = fmt.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else tokens.textPrimary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Win10CheckboxRow(
                checked = settings.defaultRewriteSiteRefs,
                onCheckedChange = onDefaultRewriteRefsChange,
                label = "Rewrite site image/CSS refs to local dir",
                subtitle = "Makes saved HTML pages render images cleanly offline"
            )
            Win10CheckboxRow(
                checked = settings.defaultExtractOnDownload,
                onCheckedChange = onDefaultExtractChange,
                label = "Auto-extract archives on download",
                subtitle = "Unpacks .zip, .7z, .tar, and .tar.gz automatically"
            )
        }

        SettingsBlock(title = "STORAGE DIRECTORY") {
            Text(
                text = downloadsPath,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = WinMonoFamily,
                color = tokens.textSecondary
            )
        }
    }
}

@Composable
private fun SettingsBlock(
    title: String,
    content: @Composable () -> Unit
) {
    val tokens = LocalWin10Tokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.cardBackground, RectangleShape)
            .border(1.dp, tokens.border, RectangleShape)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.accent
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
