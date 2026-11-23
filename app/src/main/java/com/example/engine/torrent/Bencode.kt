package com.example.engine.torrent

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

sealed class BencodeValue {
    data class BInt(val value: Long) : BencodeValue()
    data class BBytes(val bytes: ByteArray) : BencodeValue() {
        fun asUtf8(): String = String(bytes, StandardCharsets.UTF_8)
        override fun equals(other: Any?): Boolean = other is BBytes && bytes.contentEquals(other.bytes)
        override fun hashCode(): Int = bytes.contentHashCode()
    }
    data class BList(val items: List<BencodeValue>) : BencodeValue()
    data class BDict(val entries: Map<String, BencodeValue>, val rawBytes: ByteArray? = null) : BencodeValue() {
        override fun equals(other: Any?): Boolean = other is BDict && entries == other.entries
        override fun hashCode(): Int = entries.hashCode()
    }
}

object Bencode {
    fun decode(data: ByteArray): BencodeValue {
        val cursor = IntArray(1) { 0 }
        return readValue(data, cursor)
    }

    private fun readValue(data: ByteArray, cursor: IntArray): BencodeValue {
        val pos = cursor[0]
        if (pos >= data.size) throw IllegalArgumentException("Unexpected end of bencode stream at $pos")
        return when (val ch = data[pos].toInt().toChar()) {
            'i' -> {
                cursor[0] = pos + 1
                val end = findChar(data, cursor[0], 'e')
                val numStr = String(data, cursor[0], end - cursor[0], StandardCharsets.US_ASCII)
                cursor[0] = end + 1
                BencodeValue.BInt(numStr.toLong())
            }
            'l' -> {
                cursor[0] = pos + 1
                val list = mutableListOf<BencodeValue>()
                while (cursor[0] < data.size && data[cursor[0]].toInt().toChar() != 'e') {
                    list.add(readValue(data, cursor))
                }
                if (cursor[0] >= data.size) throw IllegalArgumentException("Unterminated bencode list")
                cursor[0]++
                BencodeValue.BList(list)
            }
            'd' -> {
                val start = pos
                cursor[0] = pos + 1
                val map = LinkedHashMap<String, BencodeValue>()
                while (cursor[0] < data.size && data[cursor[0]].toInt().toChar() != 'e') {
                    val keyVal = readValue(data, cursor) as? BencodeValue.BBytes
                        ?: throw IllegalArgumentException("Bencode dictionary key must be bytes")
                    val value = readValue(data, cursor)
                    map[keyVal.asUtf8()] = value
                }
                if (cursor[0] >= data.size) throw IllegalArgumentException("Unterminated bencode dict")
                cursor[0]++
                BencodeValue.BDict(map, data.copyOfRange(start, cursor[0]))
            }
            in '0'..'9' -> {
                val colon = findChar(data, pos, ':')
                val len = String(data, pos, colon - pos, StandardCharsets.US_ASCII).toInt()
                val start = colon + 1
                val end = start + len
                if (end > data.size) throw IllegalArgumentException("Bencode byte string out of bounds")
                cursor[0] = end
                BencodeValue.BBytes(data.copyOfRange(start, end))
            }
            else -> throw IllegalArgumentException("Invalid bencode token '$ch' at $pos")
        }
    }

    private fun findChar(data: ByteArray, start: Int, target: Char): Int {
        var idx = start
        while (idx < data.size) {
            if (data[idx].toInt().toChar() == target) return idx
            idx++
        }
        throw IllegalArgumentException("Expected '$target' in bencode stream starting at $start")
    }

    fun encode(value: BencodeValue): ByteArray {
        val out = ByteArrayOutputStream()
        writeValue(value, out)
        return out.toByteArray()
    }

    private fun writeValue(value: BencodeValue, out: ByteArrayOutputStream) {
        when (value) {
            is BencodeValue.BInt -> {
                out.write('i'.code)
                out.write(value.value.toString().toByteArray(StandardCharsets.US_ASCII))
                out.write('e'.code)
            }
            is BencodeValue.BBytes -> {
                out.write(value.bytes.size.toString().toByteArray(StandardCharsets.US_ASCII))
                out.write(':'.code)
                out.write(value.bytes)
            }
            is BencodeValue.BList -> {
                out.write('l'.code)
                value.items.forEach { writeValue(it, out) }
                out.write('e'.code)
            }
            is BencodeValue.BDict -> {
                out.write('d'.code)
                value.entries.entries.sortedBy { it.key }.forEach { (k, v) ->
                    val kb = k.toByteArray(StandardCharsets.UTF_8)
                    out.write(kb.size.toString().toByteArray(StandardCharsets.US_ASCII))
                    out.write(':'.code)
                    out.write(kb)
                    writeValue(v, out)
                }
                out.write('e'.code)
            }
        }
    }
}
