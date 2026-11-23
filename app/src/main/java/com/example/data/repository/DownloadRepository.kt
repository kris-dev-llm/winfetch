package com.example.data.repository

import com.example.data.local.DownloadTaskDao
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadKind
import com.example.data.model.DownloadPriority
import com.example.data.model.SiteArchiveFormat
import com.example.engine.DownloadManagerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class DownloadRepository(
    private val dao: DownloadTaskDao,
    private val engine: DownloadManagerEngine
) {
    val allTasks: Flow<List<DownloadTaskEntity>> = dao.observeAllTasks()
    val speedHistory: StateFlow<Map<String, List<Long>>> = engine.speedHistory

    suspend fun startNewDownload(
        sourceUrl: String,
        title: String,
        kind: DownloadKind,
        priority: DownloadPriority,
        siteFormat: SiteArchiveFormat,
        rewriteSiteRefs: Boolean,
        includeSiteScripts: Boolean,
        downloadIntoFolder: Boolean,
        folderName: String,
        extractOnDownload: Boolean,
        safTreeUri: String?
    ): DownloadTaskEntity {
        return engine.enqueueNewDownload(
            sourceUrl = sourceUrl,
            title = title,
            kind = kind,
            priority = priority,
            siteFormat = siteFormat,
            rewriteSiteRefs = rewriteSiteRefs,
            includeSiteScripts = includeSiteScripts,
            downloadIntoFolder = downloadIntoFolder,
            folderName = folderName,
            extractOnDownload = extractOnDownload,
            safTreeUri = safTreeUri
        )
    }

    fun pause(id: String) = engine.pauseDownload(id)
    fun resume(id: String) = engine.resumeDownload(id)
    fun cancel(id: String) = engine.cancelDownload(id)
    fun delete(id: String, deleteLocalFiles: Boolean) = engine.deleteDownload(id, deleteLocalFiles)
    fun updatePriority(id: String, priority: DownloadPriority) = engine.updatePriority(id, priority)
    fun pauseAll() = engine.pauseAll()
    fun resumeAll() = engine.resumeAll()
    fun clearCompleted() = engine.clearCompleted()

    suspend fun extractArchiveFile(file: File): Result<File> = engine.extractExistingArchive(file)
}
