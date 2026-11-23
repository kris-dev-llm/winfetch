package com.example.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class WinFetchDatabase_Impl : WinFetchDatabase() {
  private val _downloadTaskDao: Lazy<DownloadTaskDao> = lazy {
    DownloadTaskDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2,
        "f5c1f22b32268ae2b9043beb2c1d06b5", "0443b7bacc2cb8ce9f3cf29eebda9368") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `download_tasks` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `sourceUrl` TEXT NOT NULL, `kind` TEXT NOT NULL, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `siteArchiveFormat` TEXT NOT NULL, `rewriteSiteRefs` INTEGER NOT NULL, `includeSiteScripts` INTEGER NOT NULL, `downloadIntoFolder` INTEGER NOT NULL, `folderName` TEXT NOT NULL, `extractOnDownload` INTEGER NOT NULL, `localPath` TEXT NOT NULL, `extractedPath` TEXT, `safTreeUri` TEXT, `gitBranch` TEXT NOT NULL, `gitCommitMessage` TEXT NOT NULL, `gitShallow` INTEGER NOT NULL, `speedLimitBytesPerSec` INTEGER NOT NULL, `bytesDownloaded` INTEGER NOT NULL, `totalBytes` INTEGER NOT NULL, `speedBytesPerSec` INTEGER NOT NULL, `etaSeconds` INTEGER NOT NULL, `peersConnected` INTEGER NOT NULL, `seedsFound` INTEGER NOT NULL, `assetsDownloaded` INTEGER NOT NULL, `assetsTotal` INTEGER NOT NULL, `statusDetail` TEXT NOT NULL, `errorMessage` TEXT, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f5c1f22b32268ae2b9043beb2c1d06b5')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `download_tasks`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsDownloadTasks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDownloadTasks.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("sourceUrl", TableInfo.Column("sourceUrl", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("kind", TableInfo.Column("kind", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("priority", TableInfo.Column("priority", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("siteArchiveFormat", TableInfo.Column("siteArchiveFormat", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("rewriteSiteRefs", TableInfo.Column("rewriteSiteRefs", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("includeSiteScripts", TableInfo.Column("includeSiteScripts",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("downloadIntoFolder", TableInfo.Column("downloadIntoFolder",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("folderName", TableInfo.Column("folderName", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("extractOnDownload", TableInfo.Column("extractOnDownload",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("localPath", TableInfo.Column("localPath", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("extractedPath", TableInfo.Column("extractedPath", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("safTreeUri", TableInfo.Column("safTreeUri", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("gitBranch", TableInfo.Column("gitBranch", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("gitCommitMessage", TableInfo.Column("gitCommitMessage", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("gitShallow", TableInfo.Column("gitShallow", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("speedLimitBytesPerSec", TableInfo.Column("speedLimitBytesPerSec",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("bytesDownloaded", TableInfo.Column("bytesDownloaded", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("totalBytes", TableInfo.Column("totalBytes", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("speedBytesPerSec", TableInfo.Column("speedBytesPerSec",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("etaSeconds", TableInfo.Column("etaSeconds", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("peersConnected", TableInfo.Column("peersConnected", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("seedsFound", TableInfo.Column("seedsFound", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("assetsDownloaded", TableInfo.Column("assetsDownloaded",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("assetsTotal", TableInfo.Column("assetsTotal", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("statusDetail", TableInfo.Column("statusDetail", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("errorMessage", TableInfo.Column("errorMessage", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloadTasks.put("completedAt", TableInfo.Column("completedAt", "INTEGER", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDownloadTasks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDownloadTasks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDownloadTasks: TableInfo = TableInfo("download_tasks", _columnsDownloadTasks,
            _foreignKeysDownloadTasks, _indicesDownloadTasks)
        val _existingDownloadTasks: TableInfo = read(connection, "download_tasks")
        if (!_infoDownloadTasks.equals(_existingDownloadTasks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |download_tasks(com.example.data.local.DownloadTaskEntity).
              | Expected:
              |""".trimMargin() + _infoDownloadTasks + """
              |
              | Found:
              |""".trimMargin() + _existingDownloadTasks)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "download_tasks")
  }

  public override fun clearAllTables() {
    super.performClear(false, "download_tasks")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(DownloadTaskDao::class, DownloadTaskDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun downloadTaskDao(): DownloadTaskDao = _downloadTaskDao.value
}
