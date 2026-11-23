package com.example.engine

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import kotlin.coroutines.coroutineContext

data class GitRuntimeCredentials(
    val username: String = "",
    val tokenOrPassword: String = "",
    val authorName: String = "WinFetch User",
    val authorEmail: String = "winfetch@localhost"
)

class GitTransferEngine(
    private val context: Context
) {
    suspend fun downloadRepo(
        task: DownloadTaskEntity,
        downloadsRoot: File,
        credentials: GitRuntimeCredentials?,
        limiter: BandwidthLimiter,
        onProgress: suspend (TransferProgressUpdate) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val repoUrl = task.sourceUrl.trim()
        if (repoUrl.isBlank()) throw IllegalArgumentException("Enter a Git repository URL.")

        val defaultRepoName = deriveRepoName(repoUrl)
        val targetFolderName = StoragePaths.sanitizeName(
            task.folderName.ifBlank { defaultRepoName },
            defaultRepoName
        )
        val targetDir = File(downloadsRoot, targetFolderName)

        val credsProvider = credentials
            ?.takeIf { it.username.isNotBlank() || it.tokenOrPassword.isNotBlank() }
            ?.let { UsernamePasswordCredentialsProvider(it.username.ifBlank { "git" }, it.tokenOrPassword) }

        val uiScope = CoroutineScope(coroutineContext)
        val monitor = JGitBridgeMonitor(
            isUpload = false,
            targetDir = targetDir,
            limiter = limiter,
            onProgress = { update -> uiScope.launch { onProgress(update) } }
        )

        onProgress(
            TransferProgressUpdate(
                status = DownloadStatus.CONNECTING,
                statusDetail = "Connecting to Git remote..."
            )
        )

        val gitDir = File(targetDir, ".git")
        if (gitDir.exists()) {
            Git.open(targetDir).use { git ->
                val pull = git.pull()
                    .setProgressMonitor(monitor)
                if (credsProvider != null) pull.setCredentialsProvider(credsProvider)
                if (task.gitBranch.isNotBlank()) pull.setRemoteBranchName(task.gitBranch.trim())
                pull.call()
            }
        } else {
            val dest = if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
                StoragePaths.uniqueDirectory(downloadsRoot, targetFolderName)
            } else {
                targetDir.apply { mkdirs() }
            }
            val clone = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(dest)
                .setProgressMonitor(monitor)
            if (task.gitBranch.isNotBlank()) {
                clone.setBranch(task.gitBranch.trim())
            }
            if (task.gitShallow) {
                clone.setDepth(1)
            }
            if (credsProvider != null) {
                clone.setCredentialsProvider(credsProvider)
            }
            clone.call().close()
            return@withContext dest
        }
        targetDir
    }

    suspend fun uploadFolder(
        task: DownloadTaskEntity,
        downloadsRoot: File,
        credentials: GitRuntimeCredentials?,
        limiter: BandwidthLimiter,
        onProgress: suspend (TransferProgressUpdate) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val remoteUrl = task.sourceUrl.trim()
        if (remoteUrl.isBlank()) throw IllegalArgumentException("Enter a target Git remote URL.")

        val credsProvider = credentials
            ?.takeIf { it.username.isNotBlank() || it.tokenOrPassword.isNotBlank() }
            ?.let { UsernamePasswordCredentialsProvider(it.username.ifBlank { "git" }, it.tokenOrPassword) }

        onProgress(
            TransferProgressUpdate(
                status = DownloadStatus.PACKAGING,
                statusDetail = "Preparing local folder for Git push..."
            )
        )

        val localDir = resolveUploadSourceDir(task, downloadsRoot)
        if (!localDir.exists() || !localDir.isDirectory) {
            throw IllegalArgumentException("Source folder not found: ${localDir.absolutePath}")
        }

        val branch = task.gitBranch.trim().ifBlank { "main" }
        val commitMsg = task.gitCommitMessage.trim().ifBlank { "Upload via WinFetch" }
        val authorName = credentials?.authorName?.ifBlank { "WinFetch User" } ?: "WinFetch User"
        val authorEmail = credentials?.authorEmail?.ifBlank { "winfetch@localhost" } ?: "winfetch@localhost"

        val uiScope = CoroutineScope(coroutineContext)
        val monitor = JGitBridgeMonitor(
            isUpload = true,
            targetDir = localDir,
            limiter = limiter,
            onProgress = { update -> uiScope.launch { onProgress(update) } }
        )

        val gitExisted = File(localDir, ".git").exists()
        val git = if (gitExisted) {
            Git.open(localDir)
        } else {
            Git.init()
                .setDirectory(localDir)
                .setInitialBranch(branch)
                .call()
        }

        git.use { g ->
            // Ensure remote 'origin' points to requested remoteUrl
            val config = g.repository.config
            val existingOrigin = config.getString("remote", "origin", "url")
            if (existingOrigin == null) {
                g.remoteAdd().setName("origin").setUri(URIish(remoteUrl)).call()
            } else if (existingOrigin != remoteUrl) {
                g.remoteSetUrl().setRemoteName("origin").setRemoteUri(URIish(remoteUrl)).call()
            }

            onProgress(
                TransferProgressUpdate(
                    status = DownloadStatus.PACKAGING,
                    bytesDownloaded = StoragePaths.computeSize(localDir),
                    statusDetail = "Staging files in ${localDir.name}..."
                )
            )

            g.add().addFilepattern(".").call()
            if (gitExisted) {
                g.add().addFilepattern(".").setUpdate(true).call()
            }

            val status = g.status().call()
            val hasHead = g.repository.resolve("HEAD") != null
            if (!hasHead || !status.isClean || status.hasUncommittedChanges()) {
                g.commit()
                    .setMessage(commitMsg)
                    .setAuthor(authorName, authorEmail)
                    .setCommitter(authorName, authorEmail)
                    .call()
            }

            onProgress(
                TransferProgressUpdate(
                    status = DownloadStatus.UPLOADING,
                    bytesDownloaded = StoragePaths.computeSize(localDir),
                    statusDetail = "Pushing branch '$branch' to remote..."
                )
            )

            val pushCmd = g.push()
                .setRemote("origin")
                .setRefSpecs(RefSpec("HEAD:refs/heads/$branch"))
                .setProgressMonitor(monitor)
            if (credsProvider != null) {
                pushCmd.setCredentialsProvider(credsProvider)
            }
            val results = pushCmd.call()
            val errors = results.flatMap { it.remoteUpdates }
                .filter {
                    val s = it.status
                    s != org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK &&
                        s != org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE
                }
            if (errors.isNotEmpty()) {
                val msg = errors.joinToString("; ") { "${it.remoteName}: ${it.status} ${it.message.orEmpty()}" }
                throw IllegalStateException("Git push rejected: $msg")
            }
        }

        localDir
    }

    private fun resolveUploadSourceDir(task: DownloadTaskEntity, downloadsRoot: File): File {
        if (task.localPath.isNotBlank()) {
            val f = File(task.localPath)
            if (f.exists() && f.isDirectory) return f
        }
        val safUri = task.safTreeUri
        if (!safUri.isNullOrBlank()) {
            val docRoot = DocumentFile.fromTreeUri(context, Uri.parse(safUri))
                ?: throw IllegalStateException("Cannot access selected device folder")
            val dirName = StoragePaths.sanitizeName(
                task.folderName.ifBlank { docRoot.name ?: "git_upload" },
                "git_upload"
            )
            val staging = StoragePaths.uniqueDirectory(downloadsRoot, dirName)
            copyDocumentTreeToLocal(docRoot, staging)
            return staging
        }
        if (task.folderName.isNotBlank()) {
            val candidate = File(downloadsRoot, task.folderName)
            if (candidate.exists() && candidate.isDirectory) return candidate
        }
        throw IllegalArgumentException("Select a folder in WinFetch or pick a device folder to push.")
    }

    private fun copyDocumentTreeToLocal(docDir: DocumentFile, localDir: File) {
        if (!localDir.exists()) localDir.mkdirs()
        docDir.listFiles().forEach { child ->
            val name = child.name ?: return@forEach
            if (name == ".git") return@forEach
            if (child.isDirectory) {
                copyDocumentTreeToLocal(child, File(localDir, name))
            } else if (child.isFile) {
                val outFile = File(localDir, name)
                context.contentResolver.openInputStream(child.uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private class JGitBridgeMonitor(
        private val isUpload: Boolean,
        private val targetDir: File,
        private val limiter: BandwidthLimiter,
        private val onProgress: (TransferProgressUpdate) -> Unit
    ) : ProgressMonitor {
        private var currentTitle = if (isUpload) "Pushing objects" else "Cloning repository"
        private var currentTotal = 0
        private var currentDone = 0
        private var lastEmitMs = 0L
        private var startMs = System.currentTimeMillis()
        @Volatile private var cancelled = false

        override fun start(totalTasks: Int) {
            startMs = System.currentTimeMillis()
        }

        override fun beginTask(title: String?, totalWork: Int) {
            currentTitle = title ?: currentTitle
            currentTotal = totalWork.coerceAtLeast(0)
            currentDone = 0
            emitNow(force = true)
        }

        override fun update(completed: Int) {
            currentDone += completed
            limiter.throttleBlocking(completed * 512)
            emitNow(force = false)
        }

        override fun endTask() {
            currentDone = currentTotal
            emitNow(force = true)
        }

        override fun isCancelled(): Boolean = cancelled

        override fun showDuration(enabled: Boolean) {}

        private fun emitNow(force: Boolean) {
            val now = System.currentTimeMillis()
            if (!force && now - lastEmitMs < 250L) return
            lastEmitMs = now
            val diskBytes = StoragePaths.computeSize(targetDir)
            val elapsedSec = ((now - startMs) / 1000.0).coerceAtLeast(0.5)
            val speed = (diskBytes / elapsedSec).toLong()
            onProgress(
                TransferProgressUpdate(
                    status = if (isUpload) DownloadStatus.UPLOADING else DownloadStatus.DOWNLOADING,
                    bytesDownloaded = diskBytes,
                    speedBytesPerSec = speed,
                    assetsDownloaded = currentDone,
                    assetsTotal = currentTotal,
                    statusDetail = if (currentTotal > 0) {
                        "$currentTitle ($currentDone/$currentTotal)"
                    } else {
                        currentTitle
                    }
                )
            )
        }
    }

    companion object {
        fun deriveRepoName(url: String): String {
            val clean = url.trim().trimEnd('/').substringBefore('?').substringBefore('#')
            val last = clean.substringAfterLast('/').removeSuffix(".git")
            return StoragePaths.sanitizeName(last.ifBlank { "git_repo" }, "git_repo")
        }
    }
}
