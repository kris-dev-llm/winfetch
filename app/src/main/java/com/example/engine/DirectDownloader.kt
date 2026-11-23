package com.example.engine

import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadPriority
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import kotlin.coroutines.coroutineContext

data class TransferProgressUpdate(
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = -1L,
    val assetsDownloaded: Int = 0,
    val assetsTotal: Int = 0,
    val peersConnected: Int = 0,
    val seedsFound: Int = 0,
    val statusDetail: String = ""
)

class DirectDownloader(
    private val httpClient: OkHttpClient
) {
    suspend fun download(
        task: DownloadTaskEntity,
        downloadsRoot: File,
        isUrgentActive: () -> Boolean,
        onProgress: suspend (TransferProgressUpdate) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val urls = task.sourceUrl
            .lines()
            .map { it.trim() }
            .filter { it.startsWith("http://", true) || it.startsWith("https://", true) }

        if (urls.isEmpty()) {
            throw IllegalArgumentException("Enter a valid http:// or https:// URL.")
        }

        val targetDir = if (task.downloadIntoFolder || urls.size > 1) {
            val folderTitle = task.folderName.ifBlank { task.title.ifBlank { "Folder_${task.id.take(6)}" } }
            val candidate = File(downloadsRoot, StoragePaths.sanitizeName(folderTitle, "folder"))
            if (!candidate.exists()) candidate.mkdirs()
            candidate
        } else {
            downloadsRoot
        }

        if (urls.size == 1) {
            val singleUrl = urls.first()
            return@withContext downloadSingleFile(
                url = singleUrl,
                targetDir = targetDir,
                preferredFileName = task.title.takeIf { it.isNotBlank() && !it.startsWith("http") },
                existingLocalPath = task.localPath,
                priority = task.priority,
                isUrgentActive = isUrgentActive,
                onProgress = onProgress
            )
        }

        // Batch download multiple URLs into target folder
        var cumulativeBytes = 0L
        urls.forEachIndexed { idx, url ->
            coroutineContext.ensureActive()
            downloadSingleFile(
                url = url,
                targetDir = targetDir,
                preferredFileName = null,
                existingLocalPath = "",
                priority = task.priority,
                isUrgentActive = isUrgentActive
            ) { update ->
                onProgress(
                    update.copy(
                        bytesDownloaded = cumulativeBytes + update.bytesDownloaded,
                        assetsDownloaded = idx + 1,
                        assetsTotal = urls.size,
                        statusDetail = "Item ${idx + 1}/${urls.size}: ${update.statusDetail}"
                    )
                )
            }.also { finished ->
                cumulativeBytes += finished.length()
            }
        }
        targetDir
    }

    private suspend fun downloadSingleFile(
        url: String,
        targetDir: File,
        preferredFileName: String?,
        existingLocalPath: String,
        priority: DownloadPriority,
        isUrgentActive: () -> Boolean,
        onProgress: suspend (TransferProgressUpdate) -> Unit
    ): File {
        onProgress(TransferProgressUpdate(status = DownloadStatus.CONNECTING, statusDetail = "Connecting to host..."))

        // Determine file destination (supports resuming .part file if already started)
        val existingFile = existingLocalPath.takeIf { it.isNotBlank() }?.let { File(it) }
        val existingBytes = if (existingFile != null && existingFile.exists() && existingFile.isFile) {
            existingFile.length()
        } else 0L

        val reqBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "WinFetch/1.0 (Android; Windows10-Style-Client)")

        if (existingBytes > 0L) {
            reqBuilder.header("Range", "bytes=$existingBytes-")
        }

        httpClient.newCall(reqBuilder.build()).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                if (response.code == 416 && existingFile != null && existingFile.exists()) {
                    // Range already satisfied -> file complete
                    return existingFile
                }
                throw IllegalStateException("HTTP ${response.code}: ${response.message.ifBlank { "Server error" }}")
            }

            val isResumed = response.code == 206 && existingFile != null
            val body = response.body ?: throw IllegalStateException("Empty HTTP response body")
            val contentLength = body.contentLength()
            val totalBytes = when {
                isResumed && contentLength > 0 -> existingBytes + contentLength
                contentLength > 0 -> contentLength
                else -> -1L
            }

            val resolvedName = preferredFileName?.takeIf { it.contains('.') }
                ?: parseFileNameFromHeaders(response.header("Content-Disposition"), url)

            val outFile = if (isResumed) {
                existingFile
            } else {
                val candidate = File(targetDir, StoragePaths.sanitizeName(resolvedName, "download.bin"))
                if (candidate.exists() && candidate.length() == 0L) candidate else StoragePaths.uniqueFile(targetDir, resolvedName)
            }

            val startOffset = if (isResumed) existingBytes else 0L
            var downloaded = startOffset
            var windowBytes = 0L
            var windowStartMs = System.currentTimeMillis()
            var lastEmitMs = windowStartMs
            var currentSpeed = 0L

            onProgress(
                TransferProgressUpdate(
                    status = DownloadStatus.DOWNLOADING,
                    bytesDownloaded = downloaded,
                    totalBytes = totalBytes,
                    statusDetail = outFile.name
                )
            )

            val buffer = ByteArray(32 * 1024)
            body.byteStream().use { input ->
                BufferedOutputStream(FileOutputStream(outFile, isResumed)).use { output ->
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        windowBytes += read

                        // Casual priority politely yields slightly when URGENT transfers are active
                        if (priority == DownloadPriority.CASUAL && isUrgentActive()) {
                            delay(20L)
                        }

                        val now = System.currentTimeMillis()
                        val elapsedWindow = now - windowStartMs
                        if (elapsedWindow >= 600L) {
                            currentSpeed = (windowBytes * 1000L) / elapsedWindow.coerceAtLeast(1L)
                            windowBytes = 0L
                            windowStartMs = now
                        }

                        if (now - lastEmitMs >= 250L) {
                            val eta = if (totalBytes > 0L && currentSpeed > 0L) {
                                ((totalBytes - downloaded).coerceAtLeast(0L)) / currentSpeed
                            } else -1L
                            onProgress(
                                TransferProgressUpdate(
                                    status = DownloadStatus.DOWNLOADING,
                                    bytesDownloaded = downloaded,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = currentSpeed,
                                    etaSeconds = eta,
                                    statusDetail = outFile.name
                                )
                            )
                            lastEmitMs = now
                        }
                    }
                    output.flush()
                }
            }

            onProgress(
                TransferProgressUpdate(
                    status = DownloadStatus.DOWNLOADING,
                    bytesDownloaded = downloaded,
                    totalBytes = if (totalBytes > 0L) totalBytes else downloaded,
                    speedBytesPerSec = currentSpeed,
                    etaSeconds = 0L,
                    statusDetail = outFile.name
                )
            )
            return outFile
        }
    }

    companion object {
        fun parseFileNameFromHeaders(contentDisposition: String?, url: String): String {
            if (!contentDisposition.isNullOrBlank()) {
                val utf8Match = Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(contentDisposition)
                if (utf8Match != null) {
                    return runCatching { URLDecoder.decode(utf8Match.groupValues[1], "UTF-8") }
                        .getOrDefault(utf8Match.groupValues[1])
                        .trim('"', ' ')
                }
                val stdMatch = Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(contentDisposition)
                if (stdMatch != null) {
                    return stdMatch.groupValues[1].trim('"', ' ')
                }
            }
            val cleanUrl = url.substringBefore('#').substringBefore('?')
            val lastSeg = cleanUrl.substringAfterLast('/')
            return if (lastSeg.isNotBlank() && lastSeg.contains('.')) {
                runCatching { URLDecoder.decode(lastSeg, "UTF-8") }.getOrDefault(lastSeg)
            } else {
                val host = cleanUrl.substringAfter("://").substringBefore('/').ifBlank { "file" }
                "${host}_download.bin"
            }
        }
    }
}
