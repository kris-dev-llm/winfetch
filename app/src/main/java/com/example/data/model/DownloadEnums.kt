package com.example.data.model

enum class DownloadKind(val label: String) {
    DIRECT_FILE("File"),
    FOLDER_BUNDLE("Folder"),
    WEBSITE("Website"),
    TORRENT("Torrent")
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
