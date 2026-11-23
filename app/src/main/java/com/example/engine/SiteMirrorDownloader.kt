package com.example.engine

import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.SiteArchiveFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

class SiteMirrorDownloader(
    private val httpClient: OkHttpClient
) {
    private data class AssetRef(
        val absoluteUrl: String,
        val subFolder: String,
        val fallbackExt: String
    )

    suspend fun mirrorSite(
        task: DownloadTaskEntity,
        downloadsRoot: File,
        stagingRoot: File,
        onProgress: suspend (TransferProgressUpdate) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val rawUrl = task.sourceUrl.trim().let {
            if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it"
        }

        onProgress(TransferProgressUpdate(status = DownloadStatus.CONNECTING, statusDetail = "Fetching HTML from $rawUrl"))

        val htmlRequest = Request.Builder()
            .url(rawUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) WinFetch/1.0")
            .build()

        val (finalUrl, htmlString) = httpClient.newCall(htmlRequest).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code} fetching $rawUrl")
            }
            val finalReqUrl = resp.request.url.toString()
            val bodyStr = resp.body?.string() ?: throw IllegalStateException("Empty HTML response")
            finalReqUrl to bodyStr
        }

        val doc: Document = Jsoup.parse(htmlString, finalUrl)
        val siteBaseName = StoragePaths.sanitizeName(
            task.folderName.ifBlank {
                doc.title().ifBlank { finalUrl.toHttpUrlOrNull()?.host ?: "saved_site" }
            },
            "saved_site"
        )

        val workDir = File(stagingRoot, "${siteBaseName}_${task.id.take(8)}").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }
        val imagesDir = File(workDir, "images").apply { mkdirs() }
        val cssDir = File(workDir, "css").apply { mkdirs() }
        val jsDir = File(workDir, "js").apply { mkdirs() }

        // Map absolute asset URL -> relative local path inside workDir
        val urlToLocalRel = ConcurrentHashMap<String, String>()
        val assetRefs = LinkedHashMap<String, AssetRef>()

        fun registerAsset(absUrl: String, subFolder: String, fallbackExt: String) {
            if (absUrl.isBlank() || absUrl.startsWith("data:", true) || absUrl.startsWith("blob:", true)) return
            if (!absUrl.startsWith("http://", true) && !absUrl.startsWith("https://", true)) return
            if (!assetRefs.containsKey(absUrl)) {
                assetRefs[absUrl] = AssetRef(absUrl, subFolder, fallbackExt)
            }
        }

        // Collect <img>, <source>, <link rel="icon">, <link rel="stylesheet">, optional <script>
        doc.select("img").forEach { img ->
            val src = img.absUrl("src").ifBlank { img.absUrl("data-src") }
            if (src.isNotBlank()) registerAsset(src, "images", "png")
            val srcset = img.attr("srcset")
            if (srcset.isNotBlank()) {
                parseSrcsetUrls(srcset, finalUrl).forEach { registerAsset(it, "images", "png") }
            }
        }
        doc.select("picture source, video[poster]").forEach { el ->
            val poster = el.absUrl("poster")
            if (poster.isNotBlank()) registerAsset(poster, "images", "jpg")
            val srcset = el.attr("srcset")
            if (srcset.isNotBlank()) {
                parseSrcsetUrls(srcset, finalUrl).forEach { registerAsset(it, "images", "png") }
            }
        }
        doc.select("link[rel~=(?i)(icon|shortcut icon|apple-touch-icon)]").forEach { link ->
            val href = link.absUrl("href")
            if (href.isNotBlank()) registerAsset(href, "images", "png")
        }
        doc.select("link[rel~=(?i)stylesheet]").forEach { link ->
            val href = link.absUrl("href")
            if (href.isNotBlank()) registerAsset(href, "css", "css")
        }
        if (task.includeSiteScripts) {
            doc.select("script[src]").forEach { script ->
                val src = script.absUrl("src")
                if (src.isNotBlank()) registerAsset(src, "js", "js")
            }
        }

        val totalAssets = assetRefs.size
        val downloadedCount = AtomicInteger(0)
        val totalBytes = AtomicLong(htmlString.toByteArray().size.toLong())
        val startTime = System.currentTimeMillis()
        val semaphore = Semaphore(4)

        onProgress(
            TransferProgressUpdate(
                status = DownloadStatus.DOWNLOADING,
                bytesDownloaded = totalBytes.get(),
                assetsDownloaded = 0,
                assetsTotal = totalAssets,
                statusDetail = "Found $totalAssets site assets..."
            )
        )

        val cssFilesToRewrite = ConcurrentHashMap<File, String>()

        coroutineScope {
            assetRefs.values.mapIndexed { index, ref ->
                async {
                    semaphore.withPermit {
                        coroutineContext.ensureActive()
                        val targetSubDir = when (ref.subFolder) {
                            "css" -> cssDir
                            "js" -> jsDir
                            else -> imagesDir
                        }
                        val savedFile = downloadAssetFile(ref.absoluteUrl, targetSubDir, index + 1, ref.fallbackExt)
                        if (savedFile != null) {
                            val relPath = "${ref.subFolder}/${savedFile.name}"
                            urlToLocalRel[ref.absoluteUrl] = relPath
                            if (ref.subFolder == "css") {
                                cssFilesToRewrite[savedFile] = ref.absoluteUrl
                            }
                            totalBytes.addAndGet(savedFile.length())
                        }
                        val done = downloadedCount.incrementAndGet()
                        val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000.0).coerceAtLeast(0.5)
                        val speed = (totalBytes.get() / elapsedSec).toLong()
                        onProgress(
                            TransferProgressUpdate(
                                status = DownloadStatus.DOWNLOADING,
                                bytesDownloaded = totalBytes.get(),
                                speedBytesPerSec = speed,
                                assetsDownloaded = done,
                                assetsTotal = totalAssets,
                                statusDetail = "Asset $done/$totalAssets: ${savedFile?.name ?: "skipped"}"
                            )
                        )
                    }
                }
            }.awaitAll()
        }

        // Download CSS url(...) images too & rewrite CSS references
        cssFilesToRewrite.forEach { (cssFile, cssAbsUrl) ->
            coroutineContext.ensureActive()
            val cssText = runCatching { cssFile.readText() }.getOrNull() ?: return@forEach
            val rewrittenCss = rewriteCssUrls(cssText, cssAbsUrl, imagesDir, urlToLocalRel, totalBytes)
            cssFile.writeText(rewrittenCss)
        }

        // Rewrite HTML references to point to local directory paths
        if (task.rewriteSiteRefs) {
            doc.select("base").remove()
            doc.select("img").forEach { img ->
                val abs = img.absUrl("src").ifBlank { img.absUrl("data-src") }
                urlToLocalRel[abs]?.let { localRel ->
                    img.attr("src", localRel)
                    img.removeAttr("data-src")
                }
                val srcset = img.attr("srcset")
                if (srcset.isNotBlank()) {
                    img.attr("srcset", rewriteSrcset(srcset, finalUrl, urlToLocalRel))
                }
            }
            doc.select("picture source").forEach { source ->
                val srcset = source.attr("srcset")
                if (srcset.isNotBlank()) {
                    source.attr("srcset", rewriteSrcset(srcset, finalUrl, urlToLocalRel))
                }
            }
            doc.select("video[poster]").forEach { video ->
                val abs = video.absUrl("poster")
                urlToLocalRel[abs]?.let { video.attr("poster", it) }
            }
            doc.select("link[rel~=(?i)(icon|shortcut icon|apple-touch-icon|stylesheet)]").forEach { link ->
                val abs = link.absUrl("href")
                urlToLocalRel[abs]?.let { link.attr("href", it) }
            }
            if (task.includeSiteScripts) {
                doc.select("script[src]").forEach { script ->
                    val abs = script.absUrl("src")
                    urlToLocalRel[abs]?.let { script.attr("src", it) }
                }
            }
        }

        val indexFile = File(workDir, "index.html")
        indexFile.writeText(doc.outerHtml())

        val parentOutDir = if (task.downloadIntoFolder) {
            StoragePaths.uniqueDirectory(downloadsRoot, siteBaseName)
        } else {
            downloadsRoot
        }

        val finalOutput: File = when (task.siteArchiveFormat) {
            SiteArchiveFormat.DIR -> {
                onProgress(
                    TransferProgressUpdate(
                        status = DownloadStatus.PACKAGING,
                        bytesDownloaded = totalBytes.get(),
                        totalBytes = totalBytes.get(),
                        assetsDownloaded = totalAssets,
                        assetsTotal = totalAssets,
                        statusDetail = "Finalizing directory $siteBaseName/"
                    )
                )
                val destDir = StoragePaths.uniqueDirectory(parentOutDir, siteBaseName)
                workDir.copyRecursively(destDir, overwrite = true)
                workDir.deleteRecursively()
                destDir
            }

            SiteArchiveFormat.ZIP -> {
                val outZip = StoragePaths.uniqueFile(parentOutDir, "$siteBaseName.zip")
                ArchiveManager.createZipFromDirectory(workDir, outZip) { packed, total, name ->
                    onProgress(
                        TransferProgressUpdate(
                            status = DownloadStatus.PACKAGING,
                            bytesDownloaded = totalBytes.get(),
                            totalBytes = totalBytes.get(),
                            assetsDownloaded = packed,
                            assetsTotal = total,
                            statusDetail = "Zipping ($packed/$total): $name"
                        )
                    )
                }
                workDir.deleteRecursively()
                outZip
            }

            SiteArchiveFormat.SEVEN_Z -> {
                val out7z = StoragePaths.uniqueFile(parentOutDir, "$siteBaseName.7z")
                ArchiveManager.create7zFromDirectory(workDir, out7z) { packed, total, name ->
                    onProgress(
                        TransferProgressUpdate(
                            status = DownloadStatus.PACKAGING,
                            bytesDownloaded = totalBytes.get(),
                            totalBytes = totalBytes.get(),
                            assetsDownloaded = packed,
                            assetsTotal = total,
                            statusDetail = "Compressing 7Z ($packed/$total): $name"
                        )
                    )
                }
                workDir.deleteRecursively()
                out7z
            }
        }

        finalOutput
    }

    private fun parseSrcsetUrls(srcset: String, baseUrl: String): List<String> {
        val base = baseUrl.toHttpUrlOrNull() ?: return emptyList()
        return srcset.split(",")
            .mapNotNull { candidate ->
                val urlPart = candidate.trim().split(Regex("\\s+")).firstOrNull()?.trim()
                if (urlPart.isNullOrBlank()) null else base.resolve(urlPart)?.toString()
            }
    }

    private fun rewriteSrcset(
        srcset: String,
        baseUrl: String,
        urlMap: Map<String, String>
    ): String {
        val base = baseUrl.toHttpUrlOrNull() ?: return srcset
        return srcset.split(",").joinToString(", ") { entry ->
            val parts = entry.trim().split(Regex("\\s+"))
            val raw = parts.firstOrNull().orEmpty()
            val descriptor = parts.drop(1).joinToString(" ")
            val resolved = base.resolve(raw)?.toString()
            val mapped = if (resolved != null) urlMap[resolved] ?: raw else raw
            if (descriptor.isNotBlank()) "$mapped $descriptor" else mapped
        }
    }

    private fun rewriteCssUrls(
        cssText: String,
        cssAbsUrl: String,
        imagesDir: File,
        urlToLocalRel: ConcurrentHashMap<String, String>,
        totalBytes: AtomicLong
    ): String {
        val cssHttpUrl = cssAbsUrl.toHttpUrlOrNull() ?: return cssText
        val regex = Regex("url\\(([^)]+)\\)", RegexOption.IGNORE_CASE)
        var counter = 1
        return regex.replace(cssText) { match ->
            val rawInner = match.groupValues[1].trim().trim('"', '\'')
            if (rawInner.startsWith("data:", true) || rawInner.isBlank()) {
                match.value
            } else {
                val resolved = cssHttpUrl.resolve(rawInner)?.toString()
                if (resolved != null) {
                    val existingRel = urlToLocalRel[resolved]
                    val localRel = existingRel ?: run {
                        val saved = downloadAssetFile(resolved, imagesDir, 500 + counter++, "png")
                        if (saved != null) {
                            totalBytes.addAndGet(saved.length())
                            val rel = "images/${saved.name}"
                            urlToLocalRel[resolved] = rel
                            rel
                        } else null
                    }
                    if (localRel != null) "url('../$localRel')" else match.value
                } else {
                    match.value
                }
            }
        }
    }

    private fun downloadAssetFile(
        url: String,
        targetDir: File,
        index: Int,
        fallbackExt: String
    ): File? {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) WinFetch/1.0")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val body = resp.body ?: return@runCatching null
                val contentType = resp.header("Content-Type").orEmpty().lowercase()
                val rawName = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
                    .ifBlank { "asset_$index" }
                val ext = rawName.substringAfterLast('.', "").takeIf { it.length in 2..5 }
                    ?: extFromMime(contentType, fallbackExt)
                val base = StoragePaths.sanitizeName(rawName.substringBeforeLast('.').ifBlank { "asset_$index" }, "asset_$index")
                val fileName = "${index.toString().padStart(3, '0')}_$base.$ext"
                val outFile = StoragePaths.uniqueFile(targetDir, fileName)
                body.byteStream().use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                outFile
            }
        }.getOrNull()
    }

    private fun extFromMime(mime: String, fallback: String): String = when {
        mime.contains("png") -> "png"
        mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
        mime.contains("webp") -> "webp"
        mime.contains("svg") -> "svg"
        mime.contains("gif") -> "gif"
        mime.contains("css") -> "css"
        mime.contains("javascript") -> "js"
        mime.contains("woff2") -> "woff2"
        mime.contains("woff") -> "woff"
        else -> fallback
    }
}
