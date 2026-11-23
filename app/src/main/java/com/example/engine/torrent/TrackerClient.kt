package com.example.engine.torrent

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer
import kotlin.random.Random

data class TrackerAnnounceResult(
    val peers: List<InetSocketAddress>,
    val seeders: Int,
    val leechers: Int
)

class TrackerClient(
    private val httpClient: OkHttpClient
) {
    fun announce(
        trackerUrl: String,
        infoHash: ByteArray,
        peerId: ByteArray,
        left: Long
    ): TrackerAnnounceResult {
        return when {
            trackerUrl.startsWith("http://", true) || trackerUrl.startsWith("https://", true) ->
                announceHttp(trackerUrl, infoHash, peerId, left)
            trackerUrl.startsWith("udp://", true) ->
                announceUdp(trackerUrl, infoHash, peerId, left)
            else -> TrackerAnnounceResult(emptyList(), 0, 0)
        }
    }

    private fun announceHttp(
        trackerUrl: String,
        infoHash: ByteArray,
        peerId: ByteArray,
        left: Long
    ): TrackerAnnounceResult {
        val separator = if (trackerUrl.contains('?')) "&" else "?"
        val encodedHash = percentEncodeBytes(infoHash)
        val encodedPeer = percentEncodeBytes(peerId)
        val url = buildString {
            append(trackerUrl)
            append(separator)
            append("info_hash=").append(encodedHash)
            append("&peer_id=").append(encodedPeer)
            append("&port=6881&uploaded=0&downloaded=0")
            append("&left=").append(left.coerceAtLeast(0L))
            append("&compact=1&event=started&numwant=50")
        }

        val req = Request.Builder().url(url).header("User-Agent", "WinFetch/1.0").build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return TrackerAnnounceResult(emptyList(), 0, 0)
            val bodyBytes = resp.body?.bytes() ?: return TrackerAnnounceResult(emptyList(), 0, 0)
            val dict = Bencode.decode(bodyBytes) as? BencodeValue.BDict
                ?: return TrackerAnnounceResult(emptyList(), 0, 0)

            val complete = (dict.entries["complete"] as? BencodeValue.BInt)?.value?.toInt() ?: 0
            val incomplete = (dict.entries["incomplete"] as? BencodeValue.BInt)?.value?.toInt() ?: 0

            val peers = mutableListOf<InetSocketAddress>()
            when (val peersVal = dict.entries["peers"]) {
                is BencodeValue.BBytes -> {
                    val raw = peersVal.bytes
                    var i = 0
                    while (i + 6 <= raw.size) {
                        val ipBytes = raw.copyOfRange(i, i + 4)
                        val port = ((raw[i + 4].toInt() and 0xFF) shl 8) or (raw[i + 5].toInt() and 0xFF)
                        if (port in 1..65535) {
                            peers.add(InetSocketAddress(InetAddress.getByAddress(ipBytes), port))
                        }
                        i += 6
                    }
                }
                is BencodeValue.BList -> {
                    peersVal.items.forEach { item ->
                        val pDict = item as? BencodeValue.BDict ?: return@forEach
                        val ip = (pDict.entries["ip"] as? BencodeValue.BBytes)?.asUtf8() ?: return@forEach
                        val port = (pDict.entries["port"] as? BencodeValue.BInt)?.value?.toInt() ?: return@forEach
                        if (port in 1..65535) {
                            peers.add(InetSocketAddress(ip, port))
                        }
                    }
                }
                else -> {}
            }

            return TrackerAnnounceResult(peers, complete, incomplete)
        }
    }

    private fun announceUdp(
        trackerUrl: String,
        infoHash: ByteArray,
        peerId: ByteArray,
        left: Long
    ): TrackerAnnounceResult {
        val uri = URI(trackerUrl)
        val host = uri.host ?: return TrackerAnnounceResult(emptyList(), 0, 0)
        val port = if (uri.port > 0) uri.port else 80
        val address = InetAddress.getByName(host)

        DatagramSocket().use { socket ->
            socket.soTimeout = 4000
            val txId = Random.nextInt()

            // BEP-15 Connect Request (16 bytes)
            val connectReq = ByteBuffer.allocate(16)
            connectReq.putLong(0x41727101980L)
            connectReq.putInt(0) // action = connect
            connectReq.putInt(txId)
            val connectPacket = DatagramPacket(connectReq.array(), 16, address, port)
            socket.send(connectPacket)

            val connectRespBuf = ByteArray(16)
            val connectRespPacket = DatagramPacket(connectRespBuf, connectRespBuf.size)
            socket.receive(connectRespPacket)
            val cResp = ByteBuffer.wrap(connectRespBuf)
            val cAction = cResp.getInt()
            val cTx = cResp.getInt()
            if (cAction != 0 || cTx != txId) return TrackerAnnounceResult(emptyList(), 0, 0)
            val connectionId = cResp.getLong()

            // BEP-15 Announce Request (98 bytes)
            val annTxId = Random.nextInt()
            val annReq = ByteBuffer.allocate(98)
            annReq.putLong(connectionId)
            annReq.putInt(1) // action = announce
            annReq.putInt(annTxId)
            annReq.put(infoHash)
            annReq.put(peerId)
            annReq.putLong(0L) // downloaded
            annReq.putLong(left.coerceAtLeast(0L))
            annReq.putLong(0L) // uploaded
            annReq.putInt(2) // event = started
            annReq.putInt(0) // IP
            annReq.putInt(Random.nextInt()) // key
            annReq.putInt(50) // num_want
            annReq.putShort(6881.toShort())

            socket.send(DatagramPacket(annReq.array(), 98, address, port))

            val annRespBuf = ByteArray(1500)
            val annRespPacket = DatagramPacket(annRespBuf, annRespBuf.size)
            socket.receive(annRespPacket)
            if (annRespPacket.length < 20) return TrackerAnnounceResult(emptyList(), 0, 0)

            val aResp = ByteBuffer.wrap(annRespBuf, 0, annRespPacket.length)
            val aAction = aResp.getInt()
            val aTx = aResp.getInt()
            if (aAction != 1 || aTx != annTxId) return TrackerAnnounceResult(emptyList(), 0, 0)

            aResp.getInt() // interval
            val leechers = aResp.getInt()
            val seeders = aResp.getInt()

            val peers = mutableListOf<InetSocketAddress>()
            while (aResp.remaining() >= 6) {
                val ipBytes = ByteArray(4)
                aResp.get(ipBytes)
                val peerPort = aResp.getShort().toInt() and 0xFFFF
                if (peerPort in 1..65535) {
                    peers.add(InetSocketAddress(InetAddress.getByAddress(ipBytes), peerPort))
                }
            }
            return TrackerAnnounceResult(peers, seeders, leechers)
        }
    }

    private fun percentEncodeBytes(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 3)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            if ((v in '0'.code..'9'.code) ||
                (v in 'A'.code..'Z'.code) ||
                (v in 'a'.code..'z'.code) ||
                v == '-'.code || v == '_'.code || v == '.'.code || v == '~'.code
            ) {
                sb.append(v.toChar())
            } else {
                sb.append("%%%02X".format(v))
            }
        }
        return sb.toString()
    }
}
