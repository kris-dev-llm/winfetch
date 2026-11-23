package com.example

import com.example.data.model.DownloadPriority
import com.example.data.model.SiteArchiveFormat
import com.example.engine.ArchiveManager
import com.example.engine.DirectDownloader
import com.example.engine.torrent.Bencode
import com.example.engine.torrent.BencodeValue
import com.example.engine.torrent.TorrentMetadata
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

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
