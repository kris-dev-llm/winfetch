package com.example.engine

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ShareAndExportHelper {

    suspend fun shareLocalTarget(context: Context, path: String, fallbackUrl: String, title: String) {
        val file = File(path)
        if (!file.exists()) {
            shareText(context, fallbackUrl.ifBlank { title }, title)
            return
        }

        val shareFile: File = if (file.isDirectory) {
            withContext(Dispatchers.IO) {
                val cacheRoot = StoragePaths.getShareCacheRoot(context)
                val zipFile = File(cacheRoot, "${StoragePaths.sanitizeName(file.name, "folder")}.zip")
                if (zipFile.exists()) zipFile.delete()
                ArchiveManager.createZipFromDirectory(file, zipFile)
            }
        } else {
            file
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
        val mime = guessMimeType(shareFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri(shareFile.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share $title").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        val chooser = Intent.createChooser(intent, "Share $title").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openFileExternally(context: Context, file: File) {
        val target = if (file.isDirectory) {
            val indexHtml = File(file, "index.html")
            if (indexHtml.exists()) indexHtml else return
        } else file

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            target
        )
        val mime = guessMimeType(target)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { shareLocalTargetSyncFallback(context, target) }
    }

    private fun shareLocalTargetSyncFallback(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = guessMimeType(file)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    suspend fun copyToSafTree(context: Context, source: File, treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
        if (source.isFile) {
            copySingleFileToDocDir(context, source, rootDoc)
        } else {
            copyDirectoryToDocDir(context, source, rootDoc)
        }
    }

    private fun copySingleFileToDocDir(context: Context, file: File, targetDir: DocumentFile): Boolean {
        val existing = targetDir.findFile(file.name)
        existing?.delete()
        val created = targetDir.createFile(guessMimeType(file), file.name) ?: return false
        context.contentResolver.openOutputStream(created.uri)?.use { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        } ?: return false
        return true
    }

    private fun copyDirectoryToDocDir(context: Context, dir: File, parentDoc: DocumentFile): Boolean {
        val subDoc = parentDoc.findFile(dir.name)?.takeIf { it.isDirectory }
            ?: parentDoc.createDirectory(dir.name)
            ?: return false
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                copyDirectoryToDocDir(context, child, subDoc)
            } else {
                copySingleFileToDocDir(context, child, subDoc)
            }
        }
        return true
    }

    fun guessMimeType(file: File): String {
        val ext = file.extension.lowercase()
        return when (ext) {
            "zip" -> "application/zip"
            "7z" -> "application/x-7z-compressed"
            "html", "htm" -> "text/html"
            "torrent" -> "application/x-bittorrent"
            "json" -> "application/json"
            "txt", "log", "md" -> "text/plain"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        }
    }
}
