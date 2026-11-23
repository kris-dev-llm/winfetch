package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.engine.FormatUtils
import com.example.ui.WinFetchViewModel
import com.example.ui.components.NewDownloadDialog
import com.example.ui.screens.FilesBrowserScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransfersScreen
import com.example.ui.theme.LocalWin10Tokens
import com.example.ui.theme.WinFetchTheme
import com.example.ui.theme.WinMonoFamily

private const val ROUTE_TRANSFERS = "transfers"
private const val ROUTE_FILES = "files"
private const val ROUTE_SETTINGS = "settings"

private data class WinNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val WinNavDestinations = listOf(
    WinNavDestination(ROUTE_TRANSFERS, "Transfers", Icons.Filled.Download, "nav_transfers"),
    WinNavDestination(ROUTE_FILES, "Files", Icons.Filled.Folder, "nav_files"),
    WinNavDestination(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings, "nav_settings")
)

class MainActivity : ComponentActivity() {
    private val viewModel: WinFetchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            WinFetchTheme(themeMode = settings.themeMode) {
                WinFetchShell(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    viewModel.openNewDownloadDialog(sharedText)
                }
            }
            Intent.ACTION_VIEW -> {
                val uriStr = intent.data?.toString()
                if (!uriStr.isNullOrBlank()) {
                    viewModel.openNewDownloadDialog(uriStr)
                }
            }
        }
    }
}

@Composable
private fun WinFetchShell(viewModel: WinFetchViewModel) {
    val context = LocalContext.current
    val tokens = LocalWin10Tokens.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TRANSFERS

    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val speedHistory by viewModel.speedHistory.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val expandedIds by viewModel.expandedTaskIds.collectAsStateWithLifecycle()
    val isNewDialogOpen by viewModel.isNewDialogOpen.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentBrowseDir by viewModel.currentBrowseDir.collectAsStateWithLifecycle()
    val browserEntries by viewModel.browserEntries.collectAsStateWithLifecycle()
    val previewHtmlFile by viewModel.previewHtmlFile.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.submitNewDownload()
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(allTasks) {
        if (currentRoute == ROUTE_FILES) {
            viewModel.refreshFiles()
        }
    }

    val activeCount = allTasks.count { it.status.isActive }
    val totalSpeed = allTasks.filter { it.status.isActive }.sumOf { it.speedBytesPerSec }

    if (isNewDialogOpen) {
        NewDownloadDialog(
            draft = draft,
            onDraftChange = viewModel::updateDraft,
            onDismiss = viewModel::closeNewDownloadDialog,
            onSubmit = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.submitNewDownload()
                }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RectangleShape,
                        containerColor = tokens.accentPressed,
                        contentColor = Color.White
                    )
                }
            },
            topBar = {
                Win10TitleBar(
                    activeCount = activeCount,
                    totalSpeed = totalSpeed,
                    onNewDownload = { viewModel.openNewDownloadDialog() }
                )
            },
            bottomBar = {
                if (!isWide) {
                    Win10BottomNavBar(
                        currentRoute = currentRoute,
                        onSelectRoute = { route ->
                            if (route == ROUTE_FILES) viewModel.refreshFiles()
                            navController.navigate(route) {
                                popUpTo(ROUTE_TRANSFERS) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWide) {
                    Win10NavigationPane(
                        currentRoute = currentRoute,
                        onSelectRoute = { route ->
                            if (route == ROUTE_FILES) viewModel.refreshFiles()
                            navController.navigate(route) {
                                popUpTo(ROUTE_TRANSFERS) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    VerticalDivider(color = tokens.border, thickness = 1.dp)
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_TRANSFERS,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_TRANSFERS) {
                        TransfersScreen(
                            tasks = filteredTasks,
                            speedHistory = speedHistory,
                            selectedFilter = selectedFilter,
                            searchQuery = searchQuery,
                            expandedIds = expandedIds,
                            onSelectFilter = viewModel::setFilter,
                            onSearchChange = viewModel::setSearchQuery,
                            onNewDownload = { viewModel.openNewDownloadDialog() },
                            onPauseAll = viewModel::pauseAll,
                            onResumeAll = viewModel::resumeAll,
                            onClearCompleted = viewModel::clearCompleted,
                            onToggleDetails = viewModel::toggleTaskDetails,
                            onPauseTask = viewModel::pauseTask,
                            onResumeTask = viewModel::resumeTask,
                            onCancelTask = viewModel::cancelTask,
                            onDeleteTask = { id -> viewModel.deleteTask(id, deleteFiles = true) },
                            onUpdatePriority = viewModel::updateTaskPriority,
                            onShareTask = viewModel::shareTask,
                            onExtractTask = viewModel::extractTaskOutput,
                            onOpenTask = viewModel::openTaskOutput,
                            onExportTaskToSaf = viewModel::exportTaskToSaf
                        )
                    }
                    composable(ROUTE_FILES) {
                        FilesBrowserScreen(
                            currentDir = currentBrowseDir,
                            isAtRoot = viewModel.isAtDownloadsRoot(),
                            entries = browserEntries,
                            previewHtmlFile = previewHtmlFile,
                            onNavigateUp = { viewModel.navigateUpDirectory() },
                            onRefresh = viewModel::refreshFiles,
                            onOpenEntry = viewModel::openFileEntry,
                            onExtractEntry = viewModel::extractFileEntry,
                            onShareEntry = viewModel::shareFileEntry,
                            onDeleteEntry = viewModel::deleteFileEntry,
                            onClosePreview = viewModel::closeHtmlPreview
                        )
                    }
                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            onThemeModeChange = viewModel::setThemeMode,
                            onMaxConcurrentChange = viewModel::setMaxConcurrent,
                            onDefaultPriorityChange = viewModel::setDefaultPriority,
                            onDefaultSiteFormatChange = viewModel::setDefaultSiteFormat,
                            onDefaultRewriteRefsChange = viewModel::setDefaultRewriteRefs,
                            onDefaultExtractChange = viewModel::setDefaultExtract
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Win10TitleBar(
    activeCount: Int,
    totalSpeed: Long,
    onNewDownload: () -> Unit
) {
    val tokens = LocalWin10Tokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.accent)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color.White.copy(alpha = 0.16f), RectangleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "WinFetch logo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WinFetch",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (activeCount > 0) {
                        "$activeCount active • ${FormatUtils.formatSpeed(totalSpeed)}"
                    } else {
                        "Windows 10 Transfer Manager"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = WinMonoFamily,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RectangleShape,
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("titlebar_new_button")
                    .clickable(onClick = onNewDownload)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New Download",
                        tint = Color.White
                    )
                }
            }
        }
        HorizontalDivider(color = tokens.accentPressed, thickness = 1.dp)
    }
}

@Composable
private fun Win10BottomNavBar(
    currentRoute: String,
    onSelectRoute: (String) -> Unit
) {
    val tokens = LocalWin10Tokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.chromeBackground)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = tokens.border, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WinNavDestinations.forEach { dest ->
                val selected = currentRoute == dest.route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .testTag(dest.testTag)
                        .clickable { onSelectRoute(dest.route) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (selected) tokens.accent else Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label,
                        tint = if (selected) tokens.accent else tokens.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) tokens.accent else tokens.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun Win10NavigationPane(
    currentRoute: String,
    onSelectRoute: (String) -> Unit
) {
    val tokens = LocalWin10Tokens.current
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(tokens.chromeBackground)
            .navigationBarsPadding()
            .padding(top = 8.dp)
    ) {
        WinNavDestinations.forEach { dest ->
            val selected = currentRoute == dest.route
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .background(if (selected) tokens.accent.copy(alpha = 0.14f) else Color.Transparent)
                    .testTag(dest.testTag)
                    .clickable { onSelectRoute(dest.route) }
                    .padding(vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(if (selected) tokens.accent else Color.Transparent)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = dest.icon,
                    contentDescription = dest.label,
                    tint = if (selected) tokens.accent else tokens.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = dest.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) tokens.accent else tokens.textPrimary
                )
            }
        }
    }
}
