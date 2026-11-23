package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WinFetchApp
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadKind
import com.example.data.model.DownloadPriority
import com.example.data.model.DownloadStatus
import com.example.data.model.SiteArchiveFormat
import com.example.data.prefs.WinFetchSettings
import com.example.engine.ArchiveManager
import com.example.engine.ShareAndExportHelper
import com.example.engine.StoragePaths
import com.example.ui.theme.WinThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class TransferFilterTab(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    SITES("Sites"),
    TORRENTS("Torrents"),
    COMPLETED("Done")
}

data class FileBrowserEntry(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val childCount: Int,
    val lastModified: Long,
    val isArchive: Boolean,
    val isHtmlSiteEntry: Boolean
)

data class NewDownloadDraft(
    val url: String = "",
    val title: String = "",
    val kind: DownloadKind = DownloadKind.DIRECT_FILE,
    val priority: DownloadPriority = DownloadPriority.QUICK,
    val siteFormat: SiteArchiveFormat = SiteArchiveFormat.ZIP,
    val rewriteSiteRefs: Boolean = true,
    val includeSiteScripts: Boolean = false,
    val downloadIntoFolder: Boolean = false,
    val folderName: String = "",
    val extractOnDownload: Boolean = false,
    val safTreeUri: String? = null,
    val safFolderLabel: String? = null
)

class WinFetchViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WinFetchApp
    private val repo = app.downloadRepository
    private val prefsRepo = app.preferencesRepository

    val settings: StateFlow<WinFetchSettings> = prefsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WinFetchSettings()
    )

    val allTasks: StateFlow<List<DownloadTaskEntity>> = repo.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val speedHistory: StateFlow<Map<String, List<Long>>> = repo.speedHistory

    private val _selectedFilter = MutableStateFlow(TransferFilterTab.ALL)
    val selectedFilter: StateFlow<TransferFilterTab> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredTasks: StateFlow<List<DownloadTaskEntity>> = combine(
        allTasks,
        _selectedFilter,
        _searchQuery
    ) { tasks, filter, query ->
        val byTab = when (filter) {
            TransferFilterTab.ALL -> tasks
            TransferFilterTab.ACTIVE -> tasks.filter {
                it.status.isActive || it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.PAUSED
            }
            TransferFilterTab.SITES -> tasks.filter { it.kind == DownloadKind.WEBSITE }
            TransferFilterTab.TORRENTS -> tasks.filter { it.kind == DownloadKind.TORRENT }
            TransferFilterTab.COMPLETED -> tasks.filter { it.status == DownloadStatus.COMPLETED }
        }
        val q = query.trim().lowercase()
        val searched = if (q.isBlank()) byTab else {
            byTab.filter {
                it.title.lowercase().contains(q) ||
                    it.sourceUrl.lowercase().contains(q) ||
                    it.folderName.lowercase().contains(q)
            }
        }
        // Sort active/queued highest priority first, then newest
        searched.sortedWith(
            compareByDescending<DownloadTaskEntity> { it.status.isActive }
                .thenByDescending { if (!it.status.isTerminal) it.priority.rank else 0 }
                .thenByDescending { it.createdAt }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _expandedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedTaskIds: StateFlow<Set<String>> = _expandedTaskIds.asStateFlow()

    private val _isNewDialogOpen = MutableStateFlow(false)
    val isNewDialogOpen: StateFlow<Boolean> = _isNewDialogOpen.asStateFlow()

    private val _draft = MutableStateFlow(NewDownloadDraft())
    val draft: StateFlow<NewDownloadDraft> = _draft.asStateFlow()

    private val _currentBrowseDir = MutableStateFlow(StoragePaths.getDownloadsRoot(application))
    val currentBrowseDir: StateFlow<File> = _currentBrowseDir.asStateFlow()

    private val _browserEntries = MutableStateFlow<List<FileBrowserEntry>>(emptyList())
    val browserEntries: StateFlow<List<FileBrowserEntry>> = _browserEntries.asStateFlow()

    private val _previewHtmlFile = MutableStateFlow<File?>(null)
    val previewHtmlFile: StateFlow<File?> = _previewHtmlFile.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        refreshFiles()
    }

    fun setFilter(tab: TransferFilterTab) {
        _selectedFilter.value = tab
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun toggleTaskDetails(id: String) {
        _expandedTaskIds.value = _expandedTaskIds.value.let { current ->
            if (id in current) current - id else current + id
        }
    }

    fun openNewDownloadDialog(prefillUrl: String? = null) {
        val currentSettings = settings.value
        val trimmed = prefillUrl?.trim().orEmpty()
        val inferredKind = when {
            trimmed.startsWith("magnet:", true) || trimmed.endsWith(".torrent", true) -> DownloadKind.TORRENT
            trimmed.isNotBlank() && !trimmed.substringBefore('?').substringAfterLast('/').contains('.') -> DownloadKind.WEBSITE
            else -> DownloadKind.DIRECT_FILE
        }
        _draft.value = NewDownloadDraft(
            url = trimmed,
            kind = inferredKind,
            priority = currentSettings.defaultPriority,
            siteFormat = currentSettings.defaultSiteFormat,
            rewriteSiteRefs = currentSettings.defaultRewriteSiteRefs,
            extractOnDownload = currentSettings.defaultExtractOnDownload
        )
        _isNewDialogOpen.value = true
    }

    fun closeNewDownloadDialog() {
        _isNewDialogOpen.value = false
    }

    fun updateDraft(transform: (NewDownloadDraft) -> NewDownloadDraft) {
        _draft.value = transform(_draft.value)
    }

    fun submitNewDownload() {
        val d = _draft.value
        if (d.url.isBlank()) {
            _messages.tryEmit("Enter a URL, magnet link, or pick a .torrent file.")
            return
        }
        viewModelScope.launch {
            val task = repo.startNewDownload(
                sourceUrl = d.url,
                title = d.title,
                kind = d.kind,
                priority = d.priority,
                siteFormat = d.siteFormat,
                rewriteSiteRefs = d.rewriteSiteRefs,
                includeSiteScripts = d.includeSiteScripts,
                downloadIntoFolder = d.downloadIntoFolder,
                folderName = d.folderName,
                extractOnDownload = d.extractOnDownload,
                safTreeUri = d.safTreeUri
            )
            _expandedTaskIds.value = _expandedTaskIds.value + task.id
            _isNewDialogOpen.value = false
            _messages.tryEmit("Started ${task.priority.label} transfer: ${task.title}")
        }
    }

    fun pauseTask(id: String) = repo.pause(id)
    fun resumeTask(id: String) = repo.resume(id)
    fun cancelTask(id: String) = repo.cancel(id)
    fun deleteTask(id: String, deleteFiles: Boolean = true) {
        repo.delete(id, deleteFiles)
        refreshFiles()
    }
    fun updateTaskPriority(id: String, priority: DownloadPriority) = repo.updatePriority(id, priority)
    fun pauseAll() = repo.pauseAll()
    fun resumeAll() = repo.resumeAll()
    fun clearCompleted() = repo.clearCompleted()

    fun shareTask(task: DownloadTaskEntity) {
        viewModelScope.launch {
            val targetPath = task.extractedPath ?: task.localPath
            runCatching {
                ShareAndExportHelper.shareLocalTarget(
                    context = app,
                    path = targetPath,
                    fallbackUrl = task.sourceUrl,
                    title = task.title
                )
            }.onFailure {
                _messages.tryEmit("Share failed: ${it.message}")
            }
        }
    }

    fun extractTaskOutput(task: DownloadTaskEntity) {
        val file = File(task.localPath)
        if (!file.exists() || !ArchiveManager.isSupportedArchive(file.name)) {
            _messages.tryEmit("No supported archive (.zip, .7z, .tar) found to extract.")
            return
        }
        viewModelScope.launch {
            _messages.tryEmit("Extracting ${file.name}...")
            repo.extractArchiveFile(file)
                .onSuccess { dest ->
                    refreshFiles()
                    _messages.tryEmit("Extracted to ${dest.name}/")
                }
                .onFailure {
                    _messages.tryEmit("Extract failed: ${it.message}")
                }
        }
    }

    fun exportTaskToSaf(task: DownloadTaskEntity, treeUri: Uri) {
        val target = File(task.extractedPath ?: task.localPath)
        if (!target.exists()) {
            _messages.tryEmit("Local file not available yet.")
            return
        }
        viewModelScope.launch {
            val ok = ShareAndExportHelper.copyToSafTree(app, target, treeUri)
            _messages.tryEmit(if (ok) "Exported ${target.name} to chosen folder." else "Export failed.")
        }
    }

    fun refreshFiles() {
        viewModelScope.launch {
            val dir = _currentBrowseDir.value
            val entries = withContext(Dispatchers.IO) {
                if (!dir.exists()) dir.mkdirs()
                dir.listFiles().orEmpty()
                    .sortedWith(compareByDescending<File> { it.isDirectory }.thenByDescending { it.lastModified() })
                    .map { f ->
                        val isSiteDir = f.isDirectory && File(f, "index.html").exists()
                        FileBrowserEntry(
                            file = f,
                            name = f.name,
                            isDirectory = f.isDirectory,
                            sizeBytes = StoragePaths.computeSize(f),
                            childCount = if (f.isDirectory) f.listFiles()?.size ?: 0 else 1,
                            lastModified = f.lastModified(),
                            isArchive = f.isFile && ArchiveManager.isSupportedArchive(f.name),
                            isHtmlSiteEntry = isSiteDir || f.extension.equals("html", true) || f.extension.equals("htm", true)
                        )
                    }
            }
            _browserEntries.value = entries
        }
    }

    fun navigateIntoDirectory(dir: File) {
        if (dir.isDirectory) {
            _currentBrowseDir.value = dir
            refreshFiles()
        }
    }

    fun navigateUpDirectory(): Boolean {
        val root = StoragePaths.getDownloadsRoot(app)
        val current = _currentBrowseDir.value
        if (current.canonicalPath == root.canonicalPath) return false
        val parent = current.parentFile ?: return false
        _currentBrowseDir.value = if (parent.canonicalPath.startsWith(root.canonicalPath)) parent else root
        refreshFiles()
        return true
    }

    fun isAtDownloadsRoot(): Boolean {
        val root = StoragePaths.getDownloadsRoot(app)
        return _currentBrowseDir.value.canonicalPath == root.canonicalPath
    }

    fun shareFileEntry(entry: FileBrowserEntry) {
        viewModelScope.launch {
            runCatching {
                ShareAndExportHelper.shareLocalTarget(
                    context = app,
                    path = entry.file.absolutePath,
                    fallbackUrl = entry.name,
                    title = entry.name
                )
            }.onFailure {
                _messages.tryEmit("Share failed: ${it.message}")
            }
        }
    }

    fun extractFileEntry(entry: FileBrowserEntry) {
        viewModelScope.launch {
            _messages.tryEmit("Extracting ${entry.name}...")
            repo.extractArchiveFile(entry.file)
                .onSuccess { dest ->
                    refreshFiles()
                    _messages.tryEmit("Extracted to ${dest.name}/")
                }
                .onFailure {
                    _messages.tryEmit("Extract failed: ${it.message}")
                }
        }
    }

    fun deleteFileEntry(entry: FileBrowserEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            entry.file.deleteRecursively()
            refreshFiles()
            _messages.tryEmit("Deleted ${entry.name}")
        }
    }

    fun openFileEntry(entry: FileBrowserEntry) {
        val htmlTarget = when {
            entry.isDirectory && File(entry.file, "index.html").exists() -> File(entry.file, "index.html")
            entry.file.extension.equals("html", true) || entry.file.extension.equals("htm", true) -> entry.file
            else -> null
        }
        if (htmlTarget != null) {
            _previewHtmlFile.value = htmlTarget
        } else if (entry.isDirectory) {
            navigateIntoDirectory(entry.file)
        } else {
            ShareAndExportHelper.openFileExternally(app, entry.file)
        }
    }

    fun openTaskOutput(task: DownloadTaskEntity) {
        val target = File(task.extractedPath ?: task.localPath)
        if (!target.exists()) {
            _messages.tryEmit("Output not on disk yet.")
            return
        }
        val indexHtml = if (target.isDirectory) File(target, "index.html") else null
        when {
            indexHtml != null && indexHtml.exists() -> _previewHtmlFile.value = indexHtml
            target.extension.equals("html", true) -> _previewHtmlFile.value = target
            target.isDirectory -> navigateIntoDirectory(target)
            else -> ShareAndExportHelper.openFileExternally(app, target)
        }
    }

    fun closeHtmlPreview() {
        _previewHtmlFile.value = null
    }

    fun setThemeMode(mode: WinThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setMaxConcurrent(count: Int) {
        viewModelScope.launch { prefsRepo.setMaxConcurrent(count) }
    }

    fun setDefaultPriority(priority: DownloadPriority) {
        viewModelScope.launch { prefsRepo.setDefaultPriority(priority) }
    }

    fun setDefaultSiteFormat(format: SiteArchiveFormat) {
        viewModelScope.launch { prefsRepo.setDefaultSiteFormat(format) }
    }

    fun setDefaultRewriteRefs(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDefaultRewriteRefs(enabled) }
    }

    fun setDefaultExtract(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDefaultExtract(enabled) }
    }
}
