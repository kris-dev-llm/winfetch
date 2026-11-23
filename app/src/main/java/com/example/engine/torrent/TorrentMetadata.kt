package com.example.engine.torrent

import java.net.URLDecoder
import java.security.MessageDigest

data class TorrentFileEntry(
    val relativePath: String,
    val length: Long,
    val offset: Long
)

data class TorrentMetadata(
    val name: String,
    val infoHash: ByteArray,
    val infoHashHex: String,
    val trackers: List<String>,
    val webSeeds: List<String>,
    val files: List<TorrentFileEntry>,
    val totalLength: Long,
    val pieceLength: Int,
    val pieceHashes: List<ByteArray>,
    val isMagnetOnly: Boolean = false
) {
    override fun equals(other: Any?): Boolean = other is TorrentMetadata && infoHash.contentEquals(other.infoHash)
    override fun hashCode(): Int = infoHash.contentHashCode()

    companion object {
        fun fromTorrentBytes(rawBytes: ByteArray): TorrentMetadata {
            val root = Bencode.decode(rawBytes) as? BencodeValue.BDict
                ?: throw IllegalArgumentException("Invalid .torrent file: root is not a bencode dictionary")

            val infoDict = root.entries["info"] as? BencodeValue.BDict
                ?: throw IllegalArgumentException("Invalid .torrent file: missing 'info' dictionary")

            val infoRaw = infoDict.rawBytes ?: Bencode.encode(infoDict)
            val infoHash = MessageDigest.getInstance("SHA-1").digest(infoRaw)
            val infoHashHex = infoHash.joinToString("") { "%02x".format(it) }

            val name = (infoDict.entries["name"] as? BencodeValue.BBytes)?.asUtf8()?.ifBlank { null }
                ?: "torrent_${infoHashHex.take(8)}"

            val pieceLength = (infoDict.entries["piece length"] as? BencodeValue.BInt)?.value?.toInt()
                ?: throw IllegalArgumentException("Missing piece length in torrent")

            val piecesBytes = (infoDict.entries["pieces"] as? BencodeValue.BBytes)?.bytes
                ?: throw IllegalArgumentException("Missing piece hashes in torrent")

            val pieceHashes = ArrayList<ByteArray>(piecesBytes.size / 20)
            var idx = 0
            while (idx + 20 <= piecesBytes.size) {
                pieceHashes.add(piecesBytes.copyOfRange(idx, idx + 20))
                idx += 20
            }

            val trackers = LinkedHashSet<String>()
            (root.entries["announce"] as? BencodeValue.BBytes)?.asUtf8()?.let { if (it.isNotBlank()) trackers.add(it) }
            (root.entries["announce-list"] as? BencodeValue.BList)?.items?.forEach { tier ->
                (tier as? BencodeValue.BList)?.items?.forEach { tr ->
                    (tr as? BencodeValue.BBytes)?.asUtf8()?.let { if (it.isNotBlank()) trackers.add(it) }
                }
            }

            val webSeeds = LinkedHashSet<String>()
            when (val urlList = root.entries["url-list"]) {
                is BencodeValue.BBytes -> urlList.asUtf8().takeIf { it.isNotBlank() }?.let { webSeeds.add(it) }
                is BencodeValue.BList -> urlList.items.forEach { item ->
                    (item as? BencodeValue.BBytes)?.asUtf8()?.takeIf { it.isNotBlank() }?.let { webSeeds.add(it) }
                }
                else -> {}
            }

            val filesList = mutableListOf<TorrentFileEntry>()
            val multiFiles = infoDict.entries["files"] as? BencodeValue.BList
            var runningOffset = 0L
            if (multiFiles != null) {
                multiFiles.items.forEach { entry ->
                    val dict = entry as? BencodeValue.BDict ?: return@forEach
                    val len = (dict.entries["length"] as? BencodeValue.BInt)?.value ?: 0L
                    val pathParts = (dict.entries["path"] as? BencodeValue.BList)?.items
                        ?.mapNotNull { (it as? BencodeValue.BBytes)?.asUtf8() }
                        ?: emptyList()
                    val relPath = pathParts.joinToString("/").ifBlank { "file_${filesList.size}" }
                    filesList.add(TorrentFileEntry(relPath, len, runningOffset))
                    runningOffset += len
                }
            } else {
                val len = (infoDict.entries["length"] as? BencodeValue.BInt)?.value ?: 0L
                filesList.add(TorrentFileEntry(name, len, 0L))
                runningOffset = len
            }

            return TorrentMetadata(
                name = name,
                infoHash = infoHash,
                infoHashHex = infoHashHex,
                trackers = trackers.toList(),
                webSeeds = webSeeds.toList(),
                files = filesList,
                totalLength = runningOffset,
                pieceLength = pieceLength,
                pieceHashes = pieceHashes,
                isMagnetOnly = false
            )
        }

        fun fromMagnetUri(magnetUri: String): TorrentMetadata {
            val query = magnetUri.substringAfter('?', "")
            val params = query.split('&').mapNotNull { part ->
                val eq = part.indexOf('=')
                if (eq <= 0) null else {
                    val key = part.substring(0, eq).lowercase()
                    val value = runCatching { URLDecoder.decode(part.substring(eq + 1), "UTF-8") }
                        .getOrDefault(part.substring(eq + 1))
                    key to value
                }
            }

            val xt = params.firstOrNull { it.first == "xt" && it.second.startsWith("urn:btih:", true) }?.second
                ?: throw IllegalArgumentException("Magnet link missing urn:btih info hash")

            val rawHash = xt.substringAfter("urn:btih:", "").trim()
            val infoHash = when (rawHash.length) {
                40 -> rawHash.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                32 -> decodeBase32(rawHash)
                else -> throw IllegalArgumentException("Unsupported magnet info hash length: ${rawHash.length}")
            }
            val infoHashHex = infoHash.joinToString("") { "%02x".format(it) }
            val dn = params.firstOrNull { it.first == "dn" }?.second?.ifBlank { null } ?: "magnet_${infoHashHex.take(8)}"
            val trackers = params.filter { it.first == "tr" }.map { it.second }.distinct()
            val webSeeds = params.filter { it.first == "ws" }.map { it.second }.distinct()

            return TorrentMetadata(
                name = dn,
                infoHash = infoHash,
                infoHashHex = infoHashHex,
                trackers = trackers,
                webSeeds = webSeeds,
                files = emptyList(),
                totalLength = -1L,
                pieceLength = 0,
                pieceHashes = emptyList(),
                isMagnetOnly = true
            )
        }

        private fun decodeBase32(input: String): ByteArray {
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
            var buffer = 0
            var bitsLeft = 0
            val out = ArrayList<Byte>(20)
            for (ch in input.uppercase()) {
                val val5 = alphabet.indexOf(ch)
                if (val5 < 0) continue
                buffer = (buffer shl 5) or val5
                bitsLeft += 5
                if (bitsLeft >= 8) {
                    out.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                    bitsLeft -= 8
                }
            }
            return out.toByteArray()
        }
    }
}
