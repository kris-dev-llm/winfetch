package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.DownloadKind
import com.example.data.model.DownloadPriority
import com.example.data.model.DownloadStatus
import com.example.data.model.SiteArchiveFormat

class WinFetchTypeConverters {
    @TypeConverter fun fromKind(value: DownloadKind): String = value.name
    @TypeConverter fun toKind(value: String): DownloadKind = runCatching { DownloadKind.valueOf(value) }.getOrDefault(DownloadKind.DIRECT_FILE)

    @TypeConverter fun fromPriority(value: DownloadPriority): String = value.name
    @TypeConverter fun toPriority(value: String): DownloadPriority = runCatching { DownloadPriority.valueOf(value) }.getOrDefault(DownloadPriority.QUICK)

    @TypeConverter fun fromStatus(value: DownloadStatus): String = value.name
    @TypeConverter fun toStatus(value: String): DownloadStatus = runCatching { DownloadStatus.valueOf(value) }.getOrDefault(DownloadStatus.QUEUED)

    @TypeConverter fun fromSiteFormat(value: SiteArchiveFormat): String = value.name
    @TypeConverter fun toSiteFormat(value: String): SiteArchiveFormat = runCatching { SiteArchiveFormat.valueOf(value) }.getOrDefault(SiteArchiveFormat.ZIP)
}

@Database(entities = [DownloadTaskEntity::class], version = 1, exportSchema = false)
@TypeConverters(WinFetchTypeConverters::class)
abstract class WinFetchDatabase : RoomDatabase() {
    abstract fun downloadTaskDao(): DownloadTaskDao

    companion object {
        @Volatile private var instance: WinFetchDatabase? = null

        fun getInstance(context: Context): WinFetchDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WinFetchDatabase::class.java,
                    "winfetch_downloads.db"
                ).build().also { instance = it }
            }
        }
    }
}
