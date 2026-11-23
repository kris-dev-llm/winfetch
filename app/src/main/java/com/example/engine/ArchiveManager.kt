package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

object ArchiveManager {

    fun isSupportedArchive(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".zip") ||
            lower.endsWith(".7z") ||
            lower.endsWith(".tar") ||
            lower.endsWith(".tar.gz") ||
            lower.endsWith(".tgz")
    }

    fun stripArchiveExtension(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".tar.gz") -> fileName.dropLast(7)
            lower.endsWith(".tgz") -> fileName.dropLast(4)
            lower.endsWith(".zip") -> fileName.dropLast(4)
            lower.endsWith(".7z") -> fileName.dropLast(3)
            lower.endsWith(".tar") -> fileName.dropLast(4)
            else -> "${fileName}_extracted"
        }.ifBlank { "extracted" }
    }

    suspend fun extractArchive(
        archiveFile: File,
        destDir: File,
        onProgress: suspend (extractedFiles: Int, currentEntry: String) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        if (!destDir.exists()) destDir.mkdirs()
        val canonicalDest = destDir.canonicalFile
        val lower = archiveFile.name.lowercase()

        when {
            lower.endsWith(".zip") -> extractZip(archiveFile, canonicalDest, onProgress)
            lower.endsWith(".7z") -> extract7z(archiveFile, canonicalDest, onProgress)
            lower.endsWith(".tar.gz") || lower.endsWith(".tgz") ->
                extractTar(archiveFile, canonicalDest, gzip = true, onProgress)
            lower.endsWith(".tar") ->
                extractTar(archiveFile, canonicalDest, gzip = false, onProgress)
            else -> throw IllegalArgumentException("Unsupported archive format: ${archiveFile.name}")
        }
        destDir
    }

    private suspend fun extractZip(
        archiveFile: File,
        destDir: File,
        onProgress: suspend (Int, String) -> Unit
    ) {
        var count = 0
        ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zis ->
            var entry = zis.nextEntry
            val buffer = ByteArray(16 * 1024)
            while (entry != null) {
                coroutineContext.ensureActive()
                val outFile = safeResolveChild(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            coroutineContext.ensureActive()
                            bos.write(buffer, 0, read)
                        }
                    }
                    count++
                    onProgress(count, entry.name)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private suspend fun extract7z(
        archiveFile: File,
        destDir: File,
        onProgress: suspend (Int, String) -> Unit
    ) {
        var count = 0
        SevenZFile.Builder().setFile(archiveFile).get().use { sevenZ ->
            val buffer = ByteArray(16 * 1024)
            var entry: SevenZArchiveEntry? = sevenZ.nextEntry
            while (entry != null) {
                coroutineContext.ensureActive()
                val outFile = safeResolveChild(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                        var read: Int
                        while (sevenZ.read(buffer).also { read = it } > 0) {
                            coroutineContext.ensureActive()
                            bos.write(buffer, 0, read)
                        }
                    }
                    count++
                    onProgress(count, entry.name)
                }
                entry = sevenZ.nextEntry
            }
        }
    }

    private suspend fun extractTar(
        archiveFile: File,
        destDir: File,
        gzip: Boolean,
        onProgress: suspend (Int, String) -> Unit
    ) {
        var count = 0
        val rawStream = BufferedInputStream(FileInputStream(archiveFile))
        val baseStream = if (gzip) GzipCompressorInputStream(rawStream) else rawStream
        TarArchiveInputStream(baseStream).use { tis ->
            val buffer = ByteArray(16 * 1024)
            var entry = tis.nextEntry
            while (entry != null) {
                coroutineContext.ensureActive()
                val outFile = safeResolveChild(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(outFile)).use { bos ->
                        var read: Int
                        while (tis.read(buffer).also { read = it } != -1) {
                            coroutineContext.ensureActive()
                            bos.write(buffer, 0, read)
                        }
                    }
                    count++
                    onProgress(count, entry.name)
                }
                entry = tis.nextEntry
            }
        }
    }

    suspend fun createZipFromDirectory(
        sourceDir: File,
        outputZip: File,
        onProgress: suspend (packedFiles: Int, totalFiles: Int, currentName: String) -> Unit = { _, _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        outputZip.parentFile?.mkdirs()
        val files = sourceDir.walkTopDown().filter { it.isFile }.toList()
        val total = files.size.coerceAtLeast(1)
        val buffer = ByteArray(16 * 1024)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zos ->
            files.forEachIndexed { index, file ->
                coroutineContext.ensureActive()
                val relPath = file.relativeTo(sourceDir).invariantSeparatorsPath
                zos.putNextEntry(ZipEntry(relPath))
                BufferedInputStream(FileInputStream(file)).use { bis ->
                    var read: Int
                    while (bis.read(buffer).also { read = it } != -1) {
                        zos.write(buffer, 0, read)
                    }
                }
                zos.closeEntry()
                onProgress(index + 1, total, relPath)
            }
        }
        outputZip
    }

    suspend fun create7zFromDirectory(
        sourceDir: File,
        output7z: File,
        onProgress: suspend (packedFiles: Int, totalFiles: Int, currentName: String) -> Unit = { _, _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        output7z.parentFile?.mkdirs()
        val files = sourceDir.walkTopDown().filter { it.isFile }.toList()
        val total = files.size.coerceAtLeast(1)
        val buffer = ByteArray(16 * 1024)
        SevenZOutputFile(output7z).use { sevenZ ->
            sevenZ.setContentCompression(SevenZMethod.LZMA2)
            files.forEachIndexed { index, file ->
                coroutineContext.ensureActive()
                val relPath = file.relativeTo(sourceDir).invariantSeparatorsPath
                val entry = sevenZ.createArchiveEntry(file, relPath)
                sevenZ.putArchiveEntry(entry)
                BufferedInputStream(FileInputStream(file)).use { bis ->
                    var read: Int
                    while (bis.read(buffer).also { read = it } != -1) {
                        sevenZ.write(buffer, 0, read)
                    }
                }
                sevenZ.closeArchiveEntry()
                onProgress(index + 1, total, relPath)
            }
        }
        output7z
    }

    private fun safeResolveChild(destDir: File, entryName: String): File {
        val candidate = File(destDir, entryName).canonicalFile
        val destPath = destDir.canonicalPath
        if (candidate.path != destPath && !candidate.path.startsWith("$destPath${File.separator}")) {
            throw SecurityException("Blocked unsafe archive path traversal: $entryName")
        }
        return candidate
    }
}
