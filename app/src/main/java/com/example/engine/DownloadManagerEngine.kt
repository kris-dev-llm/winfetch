package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.data.local.DownloadTaskDao
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadKind
import com.example.data.model.DownloadPriority
import com.example.data.model.DownloadStatus
import com.example.data.model.SiteArchiveFormat
import com.example.data.prefs.AppPreferencesRepository
import com.example.engine.torrent.TorrentDownloader
import com.example.service.DownloadForegroundService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManagerEngine(
    private val context: Context,
    private val dao: DownloadTaskDao,
    private val prefsRepo: AppPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val directDownloader = DirectDownloader(httpClient)
    private val siteMirrorDownloader = SiteMirrorDownloader(httpClient)
    private val torrentDownloader = TorrentDownloader(context, httpClient)

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val scheduleMutex = Mutex()

    // Rolling 32-sample speed history per transfer ID for the Windows 10 Transfer Graph
    private val _speedHistory = MutableStateFlow<Map<String, List<Long>>>(emptyMap())
    val speedHistory: StateFlow<Map<String, List<Long>>> = _speedHistory.asStateFlow()

    init {
        // Recover any interrupted active transfers from previous process run as PAUSED
        scope.launch {
            val all = dao.getAllTasksOnce()
            all.filter { it.status.isActive }.forEach { stuck ->
                dao.updateStatus(stuck.id, DownloadStatus.PAUSED, "Interrupted — tap Resume")
            }
        }
    }

    suspend fun enqueueNewDownload(
        sourceUrl: String,
        title: String,
        kind: DownloadKind,
        priority: DownloadPriority,
        siteFormat: SiteArchiveFormat = SiteArchiveFormat.ZIP,
        rewriteSiteRefs: Boolean = true,
        includeSiteScripts: Boolean = false,
        downloadIntoFolder: Boolean = false,
        folderName: String = "",
        extractOnDownload: Boolean = false,
        safTreeUri: String? = null
    ): DownloadTaskEntity {
        val resolvedTitle = title.ifBlank {
            deriveDefaultTitle(sourceUrl, kind)
        }
        val entity = DownloadTaskEntity(
            id = UUID.randomUUID().toString(),
            title = resolvedTitle,
            sourceUrl = sourceUrl.trim(),
            kind = kind,
            priority = priority,
            status = DownloadStatus.QUEUED,
            siteArchiveFormat = siteFormat,
            rewriteSiteRefs = rewriteSiteRefs,
            includeSiteScripts = includeSiteScripts,
            downloadIntoFolder = downloadIntoFolder || kind == DownloadKind.FOLDER_BUNDLE,
            folderName = folderName.trim(),
            extractOnDownload = extractOnDownload,
            safTreeUri = safTreeUri,
            statusDetail = "Queued (${priority.label})"
        )
        dao.insertTask(entity)
        triggerScheduler()
        return entity
    }

    fun pauseDownload(id: String) {
        scope.launch {
            activeJobs.remove(id)?.cancel()
            dao.updateStatus(id, DownloadStatus.PAUSED, "Paused by user")
            triggerScheduler()
        }
    }

    fun resumeDownload(id: String) {
        scope.launch {
            val task = dao.getTaskById(id) ?: return@launch
            if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED || task.status == DownloadStatus.CANCELED) {
                dao.updateTask(
                    task.copy(
                        status = DownloadStatus.QUEUED,
                        errorMessage = null,
                        speedBytesPerSec = 0L,
                        statusDetail = "Queued (${task.priority.label})"
                    )
                )
                triggerScheduler()
            }
        }
    }

    fun cancelDownload(id: String) {
        scope.launch {
            activeJobs.remove(id)?.cancel()
            dao.updateStatus(id, DownloadStatus.CANCELED, "Canceled")
            triggerScheduler()
        }
    }

    fun deleteDownload(id: String, deleteLocalFiles: Boolean) {
        scope.launch {
            activeJobs.remove(id)?.cancel()
            val task = dao.getTaskById(id)
            if (deleteLocalFiles && task != null) {
                if (task.localPath.isNotBlank()) {
                    runCatching { File(task.localPath).deleteRecursively() }
                }
                if (!task.extractedPath.isNullOrBlank()) {
                    runCatching { File(task.extractedPath).deleteRecursively() }
                }
            }
            dao.deleteById(id)
            _speedHistory.update { it - id }
            triggerScheduler()
        }
    }

    fun updatePriority(id: String, newPriority: DownloadPriority) {
        scope.launch {
            dao.updatePriority(id, newPriority)
            val task = dao.getTaskById(id)
            if (task != null && task.status == DownloadStatus.QUEUED) {
                dao.updateStatus(id, DownloadStatus.QUEUED, "Queued (${newPriority.label})")
            }
            triggerScheduler()
        }
    }

    fun pauseAll() {
        scope.launch {
            val all = dao.getAllTasksOnce()
            all.filter { it.status.isActive || it.status == DownloadStatus.QUEUED }.forEach { task ->
                activeJobs.remove(task.id)?.cancel()
                dao.updateStatus(task.id, DownloadStatus.PAUSED, "Paused")
            }
            DownloadForegroundService.stop(context)
        }
    }

    fun resumeAll() {
        scope.launch {
            val all = dao.getAllTasksOnce()
            all.filter { it.status == DownloadStatus.PAUSED }.forEach { task ->
                dao.updateTask(
                    task.copy(
                        status = DownloadStatus.QUEUED,
                        errorMessage = null,
                        statusDetail = "Queued (${task.priority.label})"
                    )
                )
            }
            triggerScheduler()
        }
    }

    fun clearCompleted() {
        scope.launch {
            dao.clearTerminalTasks()
        }
    }

    suspend fun extractExistingArchive(archiveFile: File): Result<File> {
        return runCatching {
            val baseName = ArchiveManager.stripArchiveExtension(archiveFile.name)
            val destDir = StoragePaths.uniqueDirectory(archiveFile.parentFile ?: StoragePaths.getDownloadsRoot(context), baseName)
            ArchiveManager.extractArchive(archiveFile, destDir)
        }
    }

    fun triggerScheduler() {
        scope.launch {
            scheduleMutex.withLock {
                val settings = prefsRepo.settingsFlow.first()
                val allTasks = dao.getAllTasksOnce()
                val runningCount = activeJobs.count { it.value.isActive }
                val queued = allTasks
                    .filter { it.status == DownloadStatus.QUEUED }
                    .sortedWith(
                        compareByDescending<DownloadTaskEntity> { it.priority.rank }
                            .thenBy { it.createdAt }
                    )

                // URGENT tasks get +1 express slot above normal concurrency limit
                var slotsAvailable = (settings.maxConcurrentDownloads - runningCount).coerceAtLeast(0)
                for (task in queued) {
                    if (activeJobs.containsKey(task.id)) continue
                    val canStartExpress = task.priority == DownloadPriority.URGENT && runningCount < settings.maxConcurrentDownloads + 2
                    if (slotsAvailable > 0 || canStartExpress) {
                        startTask(task)
                        if (slotsAvailable > 0) slotsAvailable--
                    }
                }

                updateForegroundNotification()
            }
        }
    }

    private fun startTask(initialTask: DownloadTaskEntity) {
        val job = scope.launch {
            var current = initialTask
            val downloadsRoot = StoragePaths.getDownloadsRoot(context)
            val stagingRoot = StoragePaths.getSitesStagingRoot(context)

            try {
                val isUrgentActive = {
                    activeJobs.keys.any { activeId ->
                        activeId != current.id &&
                            runCatching { kotlinx.coroutines.runBlocking { dao.getTaskById(activeId)?.priority == DownloadPriority.URGENT } }
                                .getOrDefault(false)
                    }
                }

                val progressCallback: suspend (TransferProgressUpdate) -> Unit = { update ->
                    val latest = dao.getTaskById(current.id) ?: current
                    current = latest.copy(
                        status = update.status,
                        bytesDownloaded = update.bytesDownloaded.coerceAtLeast(latest.bytesDownloaded),
                        totalBytes = if (update.totalBytes > 0L) update.totalBytes else latest.totalBytes,
                        speedBytesPerSec = update.speedBytesPerSec,
                        etaSeconds = update.etaSeconds,
                        assetsDownloaded = update.assetsDownloaded,
                        assetsTotal = update.assetsTotal,
                        peersConnected = update.peersConnected,
                        seedsFound = update.seedsFound,
                        statusDetail = update.statusDetail
                    )
                    dao.updateTask(current)
                    recordSpeedSample(current.id, update.speedBytesPerSec)
                }

                val outputFile: File = when (current.kind) {
                    DownloadKind.DIRECT_FILE, DownloadKind.FOLDER_BUNDLE -> {
                        directDownloader.download(
                            task = current,
                            downloadsRoot = downloadsRoot,
                            isUrgentActive = isUrgentActive,
                            onProgress = progressCallback
                        )
                    }
                    DownloadKind.WEBSITE -> {
                        siteMirrorDownloader.mirrorSite(
                            task = current,
                            downloadsRoot = downloadsRoot,
                            stagingRoot = stagingRoot,
                            onProgress = progressCallback
                        )
                    }
                    DownloadKind.TORRENT -> {
                        torrentDownloader.download(
                            task = current,
                            downloadsRoot = downloadsRoot,
                            onProgress = progressCallback
                        )
                    }
                }

                var extractedDir: File? = null
                val shouldExtract = current.extractOnDownload &&
                    outputFile.isFile &&
                    ArchiveManager.isSupportedArchive(outputFile.name)

                if (shouldExtract) {
                    current = current.copy(
                        status = DownloadStatus.EXTRACTING,
                        speedBytesPerSec = 0L,
                        statusDetail = "Extracting ${outputFile.name}..."
                    )
                    dao.updateTask(current)
                    val destName = ArchiveManager.stripArchiveExtension(outputFile.name)
                    val targetExtractDir = StoragePaths.uniqueDirectory(
                        outputFile.parentFile ?: downloadsRoot,
                        destName
                    )
                    extractedDir = ArchiveManager.extractArchive(outputFile, targetExtractDir) { count, entryName ->
                        scope.launch {
                            dao.updateStatus(current.id, DownloadStatus.EXTRACTING, "Extracted $count: $entryName")
                        }
                    }
                }

                // Export to user-picked SAF directory if configured
                val safUriStr = current.safTreeUri
                if (!safUriStr.isNullOrBlank()) {
                    runCatching {
                        val exportTarget = extractedDir ?: outputFile
                        ShareAndExportHelper.copyToSafTree(context, exportTarget, Uri.parse(safUriStr))
                    }
                }

                val finalSize = StoragePaths.computeSize(outputFile)
                val latest = dao.getTaskById(current.id) ?: current
                val finalTitle = if (outputFile.name.isNotBlank() && latest.title.startsWith("http")) {
                    outputFile.name
                } else latest.title

                dao.updateTask(
                    latest.copy(
                        title = finalTitle,
                        status = DownloadStatus.COMPLETED,
                        localPath = outputFile.absolutePath,
                        extractedPath = extractedDir?.absolutePath,
                        bytesDownloaded = finalSize.coerceAtLeast(latest.bytesDownloaded),
                        totalBytes = finalSize.coerceAtLeast(latest.totalBytes),
                        speedBytesPerSec = 0L,
                        etaSeconds = 0L,
                        statusDetail = buildCompletionDetail(outputFile, extractedDir),
                        errorMessage = null,
                        completedAt = System.currentTimeMillis()
                    )
                )
            } catch (_: CancellationException) {
                // Status already updated by pause/cancel/delete
            } catch (e: Exception) {
                val latest = dao.getTaskById(current.id) ?: current
                dao.updateTask(
                    latest.copy(
                        status = DownloadStatus.FAILED,
                        speedBytesPerSec = 0L,
                        etaSeconds = -1L,
                        errorMessage = e.message ?: e::class.java.simpleName,
                        statusDetail = "Error: ${e.message ?: "Transfer failed"}"
                    )
                )
            } finally {
                activeJobs.remove(initialTask.id)
                triggerScheduler()
            }
        }
        activeJobs[initialTask.id] = job
    }

    private fun recordSpeedSample(id: String, speed: Long) {
        _speedHistory.update { map ->
            val currentList = map[id].orEmpty()
            val updated = (currentList + speed).takeLast(32)
            map + (id to updated)
        }
    }

    private fun buildCompletionDetail(outputFile: File, extractedDir: File?): String {
        val base = if (outputFile.isDirectory) {
            "Saved folder (${StoragePaths.countFiles(outputFile)} files)"
        } else {
            "Saved ${outputFile.name}"
        }
        return if (extractedDir != null) {
            "$base • Extracted to ${extractedDir.name}/"
        } else base
    }

    private suspend fun updateForegroundNotification() {
        val active = dao.getAllTasksOnce().filter { it.status.isActive }
        if (active.isEmpty()) {
            DownloadForegroundService.stop(context)
        } else {
            val lead = active.first()
            val totalSpeed = active.sumOf { it.speedBytesPerSec }
            DownloadForegroundService.updateOrStart(
                context = context,
                activeCount = active.size,
                title = if (active.size == 1) lead.title else "${active.size} active transfers",
                subtitle = "${FormatUtils.formatSpeed(totalSpeed)} • ${lead.statusDetail}"
            )
        }
    }

    companion object {
        fun deriveDefaultTitle(sourceUrl: String, kind: DownloadKind): String {
            val firstLine = sourceUrl.lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            if (firstLine.startsWith("magnet:", true)) {
                val dn = Regex("[?&]dn=([^&]+)").find(firstLine)?.groupValues?.getOrNull(1)
                return dn?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrNull() }
                    ?.ifBlank { null } ?: "Magnet Torrent"
            }
            if (kind == DownloadKind.WEBSITE) {
                return firstLine.substringAfter("://").substringBefore('/').ifBlank { "Saved Website" }
            }
            val lastSeg = firstLine.substringBefore('?').substringBefore('#').substringAfterLast('/')
            return if (lastSeg.isNotBlank() && lastSeg.contains('.')) lastSeg else {
                firstLine.substringAfter("://").substringBefore('/').ifBlank { "Download" }
            }
        }
    }
}
