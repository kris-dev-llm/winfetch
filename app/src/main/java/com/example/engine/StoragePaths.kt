package com.example.engine

import android.content.Context
import android.os.Environment
import java.io.File

object StoragePaths {
    fun getDownloadsRoot(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads")
        val root = File(base, "WinFetch")
        if (!root.exists()) root.mkdirs()
        return root
    }

    fun getSitesStagingRoot(context: Context): File {
        val staging = File(context.cacheDir, "site_staging")
        if (!staging.exists()) staging.mkdirs()
        return staging
    }

    fun getShareCacheRoot(context: Context): File {
        val share = File(context.cacheDir, "shared_zips")
        if (!share.exists()) share.mkdirs()
        return share
    }

    fun sanitizeName(raw: String, fallback: String = "download"): String {
        val cleaned = raw
            .replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F]"), "_")
            .trim()
            .trim('.')
        return if (cleaned.isBlank()) fallback else cleaned.take(96)
    }

    fun uniqueFile(parent: File, fileName: String): File {
        if (!parent.exists()) parent.mkdirs()
        val safeName = sanitizeName(fileName)
        var candidate = File(parent, safeName)
        if (!candidate.exists()) return candidate

        val dotIdx = safeName.lastIndexOf('.')
        val base = if (dotIdx > 0) safeName.substring(0, dotIdx) else safeName
        val ext = if (dotIdx > 0) safeName.substring(dotIdx) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(parent, "${base}_($counter)$ext")
            counter++
        }
        return candidate
    }

    fun uniqueDirectory(parent: File, dirName: String): File {
        if (!parent.exists()) parent.mkdirs()
        val safeName = sanitizeName(dirName, "folder")
        var candidate = File(parent, safeName)
        var counter = 1
        while (candidate.exists()) {
            candidate = File(parent, "${safeName}_($counter)")
            counter++
        }
        candidate.mkdirs()
        return candidate
    }

    fun computeSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun countFiles(file: File): Int {
        if (!file.exists()) return 0
        if (file.isFile) return 1
        return file.walkTopDown().filter { it.isFile }.count()
    }
}
