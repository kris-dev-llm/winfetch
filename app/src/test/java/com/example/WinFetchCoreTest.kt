package com.example

import com.example.data.model.DownloadPriority
import com.example.data.model.SiteArchiveFormat
import com.example.data.model.SpeedUnit
import com.example.engine.ArchiveManager
import com.example.engine.DirectDownloader
import com.example.engine.GitTransferEngine
import com.example.engine.torrent.Bencode
import com.example.engine.torrent.BencodeValue
import com.example.engine.torrent.TorrentMetadata
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WinFetchCoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun archiveManager_zipAnd7zRoundTrip() = runTest {
        val srcDir = tempFolder.newFolder("site_src")
        File(srcDir, "index.html").writeText("<html><img src='images/logo.png'></html>")
        val imgDir = File(srcDir, "images").apply { mkdirs() }
        File(imgDir, "logo.png").writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        // ZIP round-trip
        val zipOut = File(tempFolder.root, "site.zip")
        ArchiveManager.createZipFromDirectory(srcDir, zipOut)
        assertTrue(zipOut.exists() && zipOut.length() > 0)

        val zipExtractDir = File(tempFolder.root, "unzipped")
        ArchiveManager.extractArchive(zipOut, zipExtractDir)
        assertEquals(
            "<html><img src='images/logo.png'></html>",
            File(zipExtractDir, "index.html").readText()
        )
        assertTrue(File(zipExtractDir, "images/logo.png").exists())

        // 7Z round-trip
        val sevenZOut = File(tempFolder.root, "site.7z")
        ArchiveManager.create7zFromDirectory(srcDir, sevenZOut)
        assertTrue(sevenZOut.exists() && sevenZOut.length() > 0)

        val sevenZExtractDir = File(tempFolder.root, "un7zipped")
        ArchiveManager.extractArchive(sevenZOut, sevenZExtractDir)
        assertEquals(
            "<html><img src='images/logo.png'></html>",
            File(sevenZExtractDir, "index.html").readText()
        )
        assertTrue(File(sevenZExtractDir, "images/logo.png").exists())
    }

    @Test
    fun torrentMetadata_parsesTorrentAndMagnet() {
        val dummyPiece = ByteArray(20) { 0x5A }
        val torrentBytes = Bencode.encode(
            BencodeValue.BDict(
                mapOf(
                    "announce" to BencodeValue.BBytes("https://tracker.example.org/announce".toByteArray()),
                    "url-list" to BencodeValue.BBytes("https://cdn.example.org/file.iso".toByteArray()),
                    "info" to BencodeValue.BDict(
                        mapOf(
                            "name" to BencodeValue.BBytes("ubuntu.iso".toByteArray()),
                            "piece length" to BencodeValue.BInt(262144),
                            "length" to BencodeValue.BInt(1048576),
                            "pieces" to BencodeValue.BBytes(dummyPiece)
                        )
                    )
                )
            )
        )
        val meta = TorrentMetadata.fromTorrentBytes(torrentBytes)
        assertEquals("ubuntu.iso", meta.name)
        assertEquals(1048576L, meta.totalLength)
        assertEquals(1, meta.pieceHashes.size)
        assertEquals(listOf("https://tracker.example.org/announce"), meta.trackers)
        assertEquals(listOf("https://cdn.example.org/file.iso"), meta.webSeeds)

        val magnet = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=Sample+Pack&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337"
        val magMeta = TorrentMetadata.fromMagnetUri(magnet)
        assertEquals("Sample Pack", magMeta.name)
        assertEquals("0123456789abcdef0123456789abcdef01234567", magMeta.infoHashHex)
        assertTrue(magMeta.trackers.isNotEmpty())
    }

    @Test
    fun speedUnitConversions_areExact() {
        // 800 Kb/s (kilobits/s) == 100,000 bytes/s
        assertEquals(100_000L, SpeedUnit.KB_BIT.toBytesPerSecond(800L))
        // 500 KB/s (kilobytes/s) == 500,000 bytes/s
        assertEquals(500_000L, SpeedUnit.KB_BYTE.toBytesPerSecond(500L))
        // 512 KiB/s (kibibytes/s) == 524,288 bytes/s
        assertEquals(524_288L, SpeedUnit.KIB_BYTE.toBytesPerSecond(512L))
        // 8 Mb/s == 1,000,000 bytes/s
        assertEquals(1_000_000L, SpeedUnit.MB_BIT.toBytesPerSecond(8L))
        assertEquals(1_000_000L, SpeedUnit.MB_BYTE.toBytesPerSecond(1L))
        assertEquals(1_048_576L, SpeedUnit.MIB_BYTE.toBytesPerSecond(1L))
    }

    @Test
    fun gitPushAndCloneRoundTrip_worksWithJGit() {
        val bareRemoteDir = tempFolder.newFolder("remote_bare.git")
        Git.init().setDirectory(bareRemoteDir).setBare(true).setInitialBranch("main").call().close()

        val uploadSourceDir = tempFolder.newFolder("upload_folder")
        File(uploadSourceDir, "README.md").writeText("# WinFetch Git Test")

        Git.init().setDirectory(uploadSourceDir).setInitialBranch("main").call().use { git ->
            git.remoteAdd().setName("origin").setUri(URIish(bareRemoteDir.toURI().toString())).call()
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").setAuthor("Test", "test@localhost").call()
            git.push().setRemote("origin").call()
        }

        val clonedDir = File(tempFolder.root, "cloned_repo")
        Git.cloneRepository()
            .setURI(bareRemoteDir.toURI().toString())
            .setDirectory(clonedDir)
            .setBranch("main")
            .call()
            .close()

        assertEquals("# WinFetch Git Test", File(clonedDir, "README.md").readText())
        assertEquals("winfetch", GitTransferEngine.deriveRepoName("https://github.com/org/winfetch.git"))
    }

    @Test
    fun priorityOrdering_andDefaultsAreCorrect() {
        assertTrue(DownloadPriority.URGENT.rank > DownloadPriority.QUICK.rank)
        assertTrue(DownloadPriority.QUICK.rank > DownloadPriority.CASUAL.rank)
        assertEquals(SiteArchiveFormat.ZIP, SiteArchiveFormat.entries.first())
        assertEquals(
            "report.pdf",
            DirectDownloader.parseFileNameFromHeaders(
                "attachment; filename=\"report.pdf\"",
                "https://example.com/dl?id=42"
            )
        )
    }
}
