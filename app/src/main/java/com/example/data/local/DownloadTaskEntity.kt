package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadKind
import com.example.data.model.DownloadPriority
import com.example.data.model.DownloadStatus
import com.example.data.model.SiteArchiveFormat

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUrl: String,
    val kind: DownloadKind,
    val priority: DownloadPriority,
    val status: DownloadStatus,
    val siteArchiveFormat: SiteArchiveFormat = SiteArchiveFormat.ZIP,
    val rewriteSiteRefs: Boolean = true,
    val includeSiteScripts: Boolean = false,
    val downloadIntoFolder: Boolean = false,
    val folderName: String = "",
    val extractOnDownload: Boolean = false,
    val localPath: String = "",
    val extractedPath: String? = null,
    val safTreeUri: String? = null,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = -1L,
    val peersConnected: Int = 0,
    val seedsFound: Int = 0,
    val assetsDownloaded: Int = 0,
    val assetsTotal: Int = 0,
    val statusDetail: String = "",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val progressFraction: Float
        get() = when {
            status == DownloadStatus.COMPLETED -> 1f
            totalBytes > 0L -> (bytesDownloaded.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
            kind == DownloadKind.WEBSITE && assetsTotal > 0 ->
                (assetsDownloaded.toFloat() / assetsTotal.toFloat()).coerceIn(0f, 0.95f)
            else -> 0f
        }
}
