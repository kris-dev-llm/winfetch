package com.example.data.model

enum class DownloadKind(val label: String) {
    DIRECT_FILE("File"),
    FOLDER_BUNDLE("Folder"),
    WEBSITE("Website"),
    TORRENT("Torrent"),
    GIT_DOWNLOAD("Git Clone"),
    GIT_UPLOAD("Git Push")
}

enum class DownloadPriority(val label: String, val rank: Int) {
    CASUAL("Casual", 1),
    QUICK("Quick", 2),
    URGENT("URGENT", 3)
}

enum class DownloadStatus(val label: String, val isTerminal: Boolean, val isActive: Boolean) {
    QUEUED("Queued", isTerminal = false, isActive = false),
    CONNECTING("Connecting", isTerminal = false, isActive = true),
    DOWNLOADING("Downloading", isTerminal = false, isActive = true),
    UPLOADING("Uploading", isTerminal = false, isActive = true),
    PAUSED("Paused", isTerminal = false, isActive = false),
    PACKAGING("Packaging", isTerminal = false, isActive = true),
    EXTRACTING("Extracting", isTerminal = false, isActive = true),
    COMPLETED("Completed", isTerminal = true, isActive = false),
    FAILED("Failed", isTerminal = true, isActive = false),
    CANCELED("Canceled", isTerminal = true, isActive = false)
}

enum class SiteArchiveFormat(val label: String, val extension: String) {
    ZIP("ZIP", "zip"),
    DIR("DIR", ""),
    SEVEN_Z("7Z", "7z")
}

/**
 * Supports Kb (bits/sec), KB (1000 bytes/sec), KiB (1024 bytes/sec), plus Mb, MB, MiB.
 */
enum class SpeedUnit(
    val shortLabel: String,
    val description: String,
    val bytesMultiplier: Double
) {
    KB_BIT("Kb", "Kilobits/s (1,000 bits/s)", 1000.0 / 8.0),
    KB_BYTE("KB", "Kilobytes/s (1,000 B/s)", 1000.0),
    KIB_BYTE("KiB", "Kibibytes/s (1,024 B/s)", 1024.0),
    MB_BIT("Mb", "Megabits/s (1,000,000 bits/s)", 1_000_000.0 / 8.0),
    MB_BYTE("MB", "Megabytes/s (1,000,000 B/s)", 1_000_000.0),
    MIB_BYTE("MiB", "Mebibytes/s (1,048,576 B/s)", 1_048_576.0);

    fun toBytesPerSecond(value: Long): Long {
        if (value <= 0L) return 0L
        return (value.toDouble() * bytesMultiplier).toLong().coerceAtLeast(1L)
    }

    companion object {
        val primaryUnits: List<SpeedUnit> = listOf(KB_BIT, KB_BYTE, KIB_BYTE)
    }
}
