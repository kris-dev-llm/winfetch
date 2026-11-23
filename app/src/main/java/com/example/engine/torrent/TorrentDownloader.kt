package com.example.engine.torrent

import android.content.Context
import android.net.Uri
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadStatus
import com.example.engine.StoragePaths
import com.example.engine.TransferProgressUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.BitSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

class TorrentDownloader(
    private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val trackerClient = TrackerClient(httpClient)

    suspend fun download(
        task: DownloadTaskEntity,
        downloadsRoot: File,
        onProgress: suspend (TransferProgressUpdate) -> Unit
    ): File = withContext(Dispatchers.IO) {
        onProgress(
            TransferProgressUpdate(
                status = DownloadStatus.CONNECTING,
                statusDetail = "Parsing torrent metadata..."
            )
        )

        val src = task.sourceUrl.trim()
        val metadata = loadMetadata(src)
        val peerId = ByteArray(20).apply {
            "-WF0100-".toByteArray(StandardCharsets.US_ASCII).copyInto(this)
            Random.nextBytes(this, 8, 20)
        }

        val outFolder = if (task.downloadIntoFolder || metadata.files.size > 1) {
            StoragePaths.uniqueDirectory(
                downloadsRoot,
                task.folderName.ifBlank { metadata.name }
            )
        } else {
            downloadsRoot
        }

        // Discover peers & seeders from trackers
        val discoveredPeers = LinkedHashSet<InetSocketAddress>()
        var maxSeeds = 0
        val trackers = metadata.trackers.ifEmpty {
            listOf(
                "udp://tracker.opentrackr.org:1337/announce",
                "udp://open.stealth.si:80/announce",
                "https://tracker.tamersunion.org:443/announce"
            )
        }

        onProgress(
            TransferProgressUpdate(
                status = DownloadStatus.CONNECTING,
                totalBytes = metadata.totalLength,
                statusDetail = "Announcing to ${trackers.size} tracker(s)..."
            )
        )

        coroutineScope {
            trackers.take(6).map { trackerUrl ->
                async {
                    runCatching {
                        trackerClient.announce(
                            trackerUrl = trackerUrl,
                            infoHash = metadata.infoHash,
                            peerId = peerId,
                            left = metadata.totalLength.coerceAtLeast(1024L)
                        )
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull().forEach { res ->
                discoveredPeers.addAll(res.peers)
                if (res.seeders > maxSeeds) maxSeeds = res.seeders
            }
        }

        // Magnet-only links without .torrent file metadata require BEP-9 metadata exchange (ut_metadata) or a .torrent file/URL
        if (metadata.isMagnetOnly && metadata.webSeeds.isEmpty()) {
            // Try BEP-10 + BEP-9 metadata fetch from discovered peers
            val resolvedMeta = if (discoveredPeers.isNotEmpty()) {
                fetchMetadataFromPeers(metadata, discoveredPeers.take(10), peerId)
            } else null

            if (resolvedMeta == null) {
                throw IllegalStateException(
                    "Magnet link announced to trackers (${discoveredPeers.size} peers, $maxSeeds seeds), " +
                        "but no peer served BEP-9 torrent metadata. Try a direct .torrent file/URL or a seeded magnet."
                )
            }
            return@withContext executeVerifiedTorrentDownload(
                metadata = resolvedMeta,
                outFolder = outFolder,
                peers = discoveredPeers.toList(),
                seedsFound = maxSeeds,
                peerId = peerId,
                onProgress = onProgress
            )
        }

        if (metadata.pieceHashes.isEmpty() && metadata.webSeeds.isEmpty()) {
            throw IllegalStateException("Torrent has no piece hashes or web seeds.")
        }

        return@withContext executeVerifiedTorrentDownload(
            metadata = metadata,
            outFolder = outFolder,
            peers = discoveredPeers.toList(),
            seedsFound = maxSeeds,
            peerId = peerId,
            onProgress = onProgress
        )
    }

    private suspend fun executeVerifiedTorrentDownload(
        metadata: TorrentMetadata,
        outFolder: File,
        peers: List<InetSocketAddress>,
        seedsFound: Int,
        peerId: ByteArray,
        onProgress: suspend (TransferProgressUpdate) -> Unit
    ): File {
        val totalPieces = metadata.pieceHashes.size
        val verifiedPieces = ConcurrentHashMap<Int, Boolean>()
        val bytesDownloaded = AtomicLong(0L)
        val activePeers = AtomicInteger(0)
        val startMs = System.currentTimeMillis()

        // Allocate target files
        val fileTargets = metadata.files.map { entry ->
            val file = File(outFolder, entry.relativePath).apply {
                parentFile?.mkdirs()
                if (!exists()) createNewFile()
            }
            RandomAccessFile(file, "rw").apply { setLength(entry.length) }
        }

        try {
            fun writePieceToFiles(pieceIndex: Int, pieceData: ByteArray) {
                var globalOffset = pieceIndex.toLong() * metadata.pieceLength.toLong()
                var dataOffset = 0
                var remaining = pieceData.size
                for (i in metadata.files.indices) {
                    val entry = metadata.files[i]
                    val entryEnd = entry.offset + entry.length
                    if (globalOffset >= entry.offset && globalOffset < entryEnd && remaining > 0) {
                        val fileOffset = globalOffset - entry.offset
                        val toWrite = minOf(remaining.toLong(), entryEnd - globalOffset).toInt()
                        synchronized(fileTargets[i]) {
                            fileTargets[i].seek(fileOffset)
                            fileTargets[i].write(pieceData, dataOffset, toWrite)
                        }
                        globalOffset += toWrite
                        dataOffset += toWrite
                        remaining -= toWrite
                    }
                }
            }

            fun expectedPieceSize(pieceIndex: Int): Int {
                val start = pieceIndex.toLong() * metadata.pieceLength.toLong()
                val rem = metadata.totalLength - start
                return minOf(metadata.pieceLength.toLong(), rem).toInt()
            }

            suspend fun emitPieceProgress(detail: String) {
                val elapsedSec = ((System.currentTimeMillis() - startMs) / 1000.0).coerceAtLeast(0.5)
                val doneBytes = bytesDownloaded.get()
                val speed = (doneBytes / elapsedSec).toLong()
                val eta = if (speed > 0L) ((metadata.totalLength - doneBytes).coerceAtLeast(0L) / speed) else -1L
                onProgress(
                    TransferProgressUpdate(
                        status = DownloadStatus.DOWNLOADING,
                        bytesDownloaded = doneBytes,
                        totalBytes = metadata.totalLength,
                        speedBytesPerSec = speed,
                        etaSeconds = eta,
                        peersConnected = activePeers.get(),
                        seedsFound = seedsFound,
                        assetsDownloaded = verifiedPieces.size,
                        assetsTotal = totalPieces,
                        statusDetail = detail
                    )
                )
            }

            // Priority 1: BEP-19 WebSeeds if present
            if (metadata.webSeeds.isNotEmpty() && metadata.files.size == 1) {
                val seedUrl = metadata.webSeeds.first()
                for (pieceIdx in 0 until totalPieces) {
                    coroutineContext.ensureActive()
                    if (verifiedPieces.containsKey(pieceIdx)) continue
                    val pSize = expectedPieceSize(pieceIdx)
                    val startByte = pieceIdx.toLong() * metadata.pieceLength.toLong()
                    val endByte = startByte + pSize - 1
                    val req = Request.Builder()
                        .url(seedUrl)
                        .header("Range", "bytes=$startByte-$endByte")
                        .build()
                    val data = runCatching {
                        httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful || resp.code == 206) resp.body?.bytes() else null
                        }
                    }.getOrNull()

                    if (data != null && data.size == pSize && verifySha1(data, metadata.pieceHashes[pieceIdx])) {
                        writePieceToFiles(pieceIdx, data)
                        verifiedPieces[pieceIdx] = true
                        bytesDownloaded.addAndGet(pSize.toLong())
                        emitPieceProgress("WebSeed piece ${verifiedPieces.size}/$totalPieces")
                    }
                }
            }

            // Priority 2: BitTorrent Peer Wire Protocol for remaining pieces
            if (verifiedPieces.size < totalPieces && peers.isNotEmpty()) {
                val nextPieceIndex = AtomicInteger(0)
                coroutineScope {
                    peers.take(8).map { addr ->
                        async {
                            runCatching {
                                downloadFromTcpPeer(
                                    address = addr,
                                    metadata = metadata,
                                    peerId = peerId,
                                    verifiedPieces = verifiedPieces,
                                    nextPieceCursor = nextPieceIndex,
                                    expectedPieceSize = ::expectedPieceSize,
                                    onPeerConnected = { connected ->
                                        if (connected) activePeers.incrementAndGet() else activePeers.decrementAndGet()
                                    },
                                    onPieceVerified = { pieceIdx, data ->
                                        writePieceToFiles(pieceIdx, data)
                                        verifiedPieces[pieceIdx] = true
                                        bytesDownloaded.addAndGet(data.size.toLong())
                                        emitPieceProgress("Piece ${verifiedPieces.size}/$totalPieces (${activePeers.get()} peers)")
                                    }
                                )
                            }
                        }
                    }.awaitAll()
                }
            }

            if (verifiedPieces.size < totalPieces) {
                throw IllegalStateException(
                    "Downloaded ${verifiedPieces.size}/$totalPieces pieces " +
                        "(${peers.size} tracker peers found, $seedsFound seeds). " +
                        "Insufficient reachable seeders on current network."
                )
            }
        } finally {
            fileTargets.forEach { runCatching { it.close() } }
        }

        return if (metadata.files.size == 1 && !outFolder.equals(File(outFolder, metadata.files.first().relativePath))) {
            val single = File(outFolder, metadata.files.first().relativePath)
            if (single.exists() && metadata.files.size == 1) outFolder else outFolder
        } else {
            outFolder
        }
    }

    private suspend fun downloadFromTcpPeer(
        address: InetSocketAddress,
        metadata: TorrentMetadata,
        peerId: ByteArray,
        verifiedPieces: ConcurrentHashMap<Int, Boolean>,
        nextPieceCursor: AtomicInteger,
        expectedPieceSize: (Int) -> Int,
        onPeerConnected: (Boolean) -> Unit,
        onPieceVerified: suspend (Int, ByteArray) -> Unit
    ) {
        val socket = Socket()
        try {
            socket.connect(address, 3500)
            socket.soTimeout = 5000
            onPeerConnected(true)

            val out = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // Send 68-byte BitTorrent handshake
            val handshake = ByteArray(68)
            handshake[0] = 19
            "BitTorrent protocol".toByteArray(StandardCharsets.US_ASCII).copyInto(handshake, 1)
            metadata.infoHash.copyInto(handshake, 28)
            peerId.copyInto(handshake, 48)
            out.write(handshake)
            out.flush()

            val respHandshake = ByteArray(68)
            input.readFully(respHandshake)
            val respInfoHash = respHandshake.copyOfRange(28, 48)
            if (!respInfoHash.contentEquals(metadata.infoHash)) return

            // Send interested (len=1, id=2)
            out.writeInt(1)
            out.writeByte(2)
            out.flush()

            val peerBitfield = BitSet(metadata.pieceHashes.size)
            var unchoked = false

            // Read initial messages until unchoked or timeout
            var handshakeLoops = 0
            while (!unchoked && handshakeLoops < 12) {
                coroutineContext.ensureActive()
                val msgLen = input.readInt()
                if (msgLen == 0) {
                    handshakeLoops++
                    continue
                }
                val msgId = input.readUnsignedByte()
                val payload = ByteArray((msgLen - 1).coerceAtLeast(0))
                if (payload.isNotEmpty()) input.readFully(payload)
                when (msgId) {
                    1 -> unchoked = true // unchoke
                    4 -> { // have
                        if (payload.size >= 4) {
                            val idx = ByteBuffer.wrap(payload).getInt()
                            if (idx in 0 until metadata.pieceHashes.size) peerBitfield.set(idx)
                        }
                    }
                    5 -> { // bitfield
                        for (bIdx in payload.indices) {
                            for (bit in 0..7) {
                                if ((payload[bIdx].toInt() and (1 shl (7 - bit))) != 0) {
                                    val pIdx = bIdx * 8 + bit
                                    if (pIdx < metadata.pieceHashes.size) peerBitfield.set(pIdx)
                                }
                            }
                        }
                    }
                }
                handshakeLoops++
            }

            if (!unchoked) return

            val totalPieces = metadata.pieceHashes.size
            while (verifiedPieces.size < totalPieces) {
                coroutineContext.ensureActive()
                val pieceIdx = nextPieceCursor.getAndIncrement() % totalPieces
                if (verifiedPieces.containsKey(pieceIdx)) continue
                if (!peerBitfield.isEmpty && !peerBitfield.get(pieceIdx)) continue

                val pieceSize = expectedPieceSize(pieceIdx)
                val pieceBuf = ByteArray(pieceSize)
                val blockSize = 16 * 1024
                var offset = 0

                while (offset < pieceSize) {
                    val reqLen = minOf(blockSize, pieceSize - offset)
                    val reqMsg = ByteBuffer.allocate(17)
                    reqMsg.putInt(13)
                    reqMsg.put(6) // request
                    reqMsg.putInt(pieceIdx)
                    reqMsg.putInt(offset)
                    reqMsg.putInt(reqLen)
                    out.write(reqMsg.array())
                    out.flush()

                    var receivedBlock = false
                    while (!receivedBlock) {
                        val mLen = input.readInt()
                        if (mLen == 0) continue
                        val mId = input.readUnsignedByte()
                        val mPayload = ByteArray((mLen - 1).coerceAtLeast(0))
                        if (mPayload.isNotEmpty()) input.readFully(mPayload)
                        if (mId == 7 && mPayload.size >= 8) {
                            val bb = ByteBuffer.wrap(mPayload)
                            val rIdx = bb.getInt()
                            val rOff = bb.getInt()
                            if (rIdx == pieceIdx && rOff == offset) {
                                val dataLen = mPayload.size - 8
                                System.arraycopy(mPayload, 8, pieceBuf, offset, minOf(dataLen, pieceSize - offset))
                                offset += dataLen
                                receivedBlock = true
                            }
                        } else if (mId == 0) {
                            return // choked
                        }
                    }
                }

                if (verifySha1(pieceBuf, metadata.pieceHashes[pieceIdx])) {
                    onPieceVerified(pieceIdx, pieceBuf)
                }
            }
        } finally {
            onPeerConnected(false)
            runCatching { socket.close() }
        }
    }

    private fun fetchMetadataFromPeers(
        magnetMeta: TorrentMetadata,
        peers: List<InetSocketAddress>,
        peerId: ByteArray
    ): TorrentMetadata? {
        // Attempt BEP-10 extended handshake + BEP-9 ut_metadata fetch
        for (addr in peers) {
            runCatching {
                Socket().use { socket ->
                    socket.connect(addr, 2500)
                    socket.soTimeout = 3000
                    val out = DataOutputStream(socket.getOutputStream())
                    val input = DataInputStream(socket.getInputStream())

                    val handshake = ByteArray(68)
                    handshake[0] = 19
                    "BitTorrent protocol".toByteArray(StandardCharsets.US_ASCII).copyInto(handshake, 1)
                    handshake[25] = 0x10 // BEP-10 extension bit
                    magnetMeta.infoHash.copyInto(handshake, 28)
                    peerId.copyInto(handshake, 48)
                    out.write(handshake)
                    out.flush()

                    val resp = ByteArray(68)
                    input.readFully(resp)
                    if ((resp[25].toInt() and 0x10) == 0) return@use

                    // Send BEP-10 extended handshake
                    val extDict = Bencode.encode(
                        BencodeValue.BDict(
                            mapOf(
                                "m" to BencodeValue.BDict(mapOf("ut_metadata" to BencodeValue.BInt(1)))
                            )
                        )
                    )
                    out.writeInt(extDict.size + 2)
                    out.writeByte(20)
                    out.writeByte(0)
                    out.write(extDict)
                    out.flush()

                    var utMetadataId = -1
                    var metadataSize = 0
                    val pieces = HashMap<Int, ByteArray>()
                    var loops = 0
                    while (loops < 20) {
                        val len = input.readInt()
                        if (len == 0) {
                            loops++
                            continue
                        }
                        val id = input.readUnsignedByte()
                        val payload = ByteArray((len - 1).coerceAtLeast(0))
                        if (payload.isNotEmpty()) input.readFully(payload)
                        if (id == 20 && payload.isNotEmpty()) {
                            val extId = payload[0].toInt() and 0xFF
                            val extPayload = payload.copyOfRange(1, payload.size)
                            if (extId == 0) {
                                val d = Bencode.decode(extPayload) as? BencodeValue.BDict
                                val m = d?.entries?.get("m") as? BencodeValue.BDict
                                utMetadataId = (m?.entries?.get("ut_metadata") as? BencodeValue.BInt)?.value?.toInt() ?: -1
                                metadataSize = (d?.entries?.get("metadata_size") as? BencodeValue.BInt)?.value?.toInt() ?: 0
                                if (utMetadataId > 0 && metadataSize > 0) {
                                    val numPieces = (metadataSize + 16383) / 16384
                                    for (p in 0 until numPieces) {
                                        val reqBytes = Bencode.encode(
                                            BencodeValue.BDict(
                                                mapOf(
                                                    "msg_type" to BencodeValue.BInt(0),
                                                    "piece" to BencodeValue.BInt(p.toLong())
                                                )
                                            )
                                        )
                                        out.writeInt(reqBytes.size + 2)
                                        out.writeByte(20)
                                        out.writeByte(utMetadataId)
                                        out.write(reqBytes)
                                    }
                                    out.flush()
                                }
                            } else if (extId == 1 && metadataSize > 0) {
                                // Split bencoded dict from raw info piece bytes
                                val str = String(extPayload, StandardCharsets.ISO_8859_1)
                                val endDictIdx = str.indexOf("ee")
                                if (endDictIdx > 0) {
                                    val headerDict = Bencode.decode(extPayload.copyOfRange(0, endDictIdx + 2)) as? BencodeValue.BDict
                                    val msgType = (headerDict?.entries?.get("msg_type") as? BencodeValue.BInt)?.value?.toInt()
                                    val pieceIdx = (headerDict?.entries?.get("piece") as? BencodeValue.BInt)?.value?.toInt()
                                    if (msgType == 1 && pieceIdx != null) {
                                        pieces[pieceIdx] = extPayload.copyOfRange(endDictIdx + 2, extPayload.size)
                                        val expectedPieces = (metadataSize + 16383) / 16384
                                        if (pieces.size == expectedPieces) {
                                            val fullInfo = ByteArray(metadataSize)
                                            var off = 0
                                            for (p in 0 until expectedPieces) {
                                                val chunk = pieces[p] ?: break
                                                chunk.copyInto(fullInfo, off)
                                                off += chunk.size
                                            }
                                            if (verifySha1(fullInfo, magnetMeta.infoHash)) {
                                                val wrapper = Bencode.encode(
                                                    BencodeValue.BDict(
                                                        mapOf(
                                                            "info" to Bencode.decode(fullInfo),
                                                            "announce-list" to BencodeValue.BList(
                                                                listOf(
                                                                    BencodeValue.BList(
                                                                        magnetMeta.trackers.map {
                                                                            BencodeValue.BBytes(it.toByteArray())
                                                                        }
                                                                    )
                                                                )
                                                            )
                                                        )
                                                    )
                                                )
                                                return TorrentMetadata.fromTorrentBytes(wrapper)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        loops++
                    }
                }
            }
        }
        return null
    }

    private fun loadMetadata(source: String): TorrentMetadata {
        return when {
            source.startsWith("magnet:", true) -> TorrentMetadata.fromMagnetUri(source)
            source.startsWith("content://", true) -> {
                val bytes = context.contentResolver.openInputStream(Uri.parse(source))?.use { it.readBytes() }
                    ?: throw IllegalStateException("Cannot read selected .torrent file")
                TorrentMetadata.fromTorrentBytes(bytes)
            }
            source.startsWith("http://", true) || source.startsWith("https://", true) -> {
                val req = Request.Builder().url(source).header("User-Agent", "WinFetch/1.0").build()
                val bytes = httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code} fetching .torrent")
                    resp.body?.bytes() ?: throw IllegalStateException("Empty .torrent file")
                }
                TorrentMetadata.fromTorrentBytes(bytes)
            }
            else -> {
                val file = File(source)
                if (!file.exists()) throw IllegalArgumentException("Torrent file not found: $source")
                TorrentMetadata.fromTorrentBytes(file.readBytes())
            }
        }
    }

    private fun verifySha1(data: ByteArray, expectedHash: ByteArray): Boolean {
        val digest = MessageDigest.getInstance("SHA-1").digest(data)
        return digest.contentEquals(expectedHash)
    }
}
