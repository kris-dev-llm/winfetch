package com.example.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.`data`.model.DownloadKind
import com.example.`data`.model.DownloadPriority
import com.example.`data`.model.DownloadStatus
import com.example.`data`.model.SiteArchiveFormat
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class DownloadTaskDao_Impl(
  __db: RoomDatabase,
) : DownloadTaskDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfDownloadTaskEntity: EntityInsertAdapter<DownloadTaskEntity>

  private val __winFetchTypeConverters: WinFetchTypeConverters = WinFetchTypeConverters()

  private val __updateAdapterOfDownloadTaskEntity: EntityDeleteOrUpdateAdapter<DownloadTaskEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfDownloadTaskEntity = object : EntityInsertAdapter<DownloadTaskEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `download_tasks` (`id`,`title`,`sourceUrl`,`kind`,`priority`,`status`,`siteArchiveFormat`,`rewriteSiteRefs`,`includeSiteScripts`,`downloadIntoFolder`,`folderName`,`extractOnDownload`,`localPath`,`extractedPath`,`safTreeUri`,`gitBranch`,`gitCommitMessage`,`gitShallow`,`speedLimitBytesPerSec`,`bytesDownloaded`,`totalBytes`,`speedBytesPerSec`,`etaSeconds`,`peersConnected`,`seedsFound`,`assetsDownloaded`,`assetsTotal`,`statusDetail`,`errorMessage`,`createdAt`,`completedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DownloadTaskEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.sourceUrl)
        val _tmp: String = __winFetchTypeConverters.fromKind(entity.kind)
        statement.bindText(4, _tmp)
        val _tmp_1: String = __winFetchTypeConverters.fromPriority(entity.priority)
        statement.bindText(5, _tmp_1)
        val _tmp_2: String = __winFetchTypeConverters.fromStatus(entity.status)
        statement.bindText(6, _tmp_2)
        val _tmp_3: String = __winFetchTypeConverters.fromSiteFormat(entity.siteArchiveFormat)
        statement.bindText(7, _tmp_3)
        val _tmp_4: Int = if (entity.rewriteSiteRefs) 1 else 0
        statement.bindLong(8, _tmp_4.toLong())
        val _tmp_5: Int = if (entity.includeSiteScripts) 1 else 0
        statement.bindLong(9, _tmp_5.toLong())
        val _tmp_6: Int = if (entity.downloadIntoFolder) 1 else 0
        statement.bindLong(10, _tmp_6.toLong())
        statement.bindText(11, entity.folderName)
        val _tmp_7: Int = if (entity.extractOnDownload) 1 else 0
        statement.bindLong(12, _tmp_7.toLong())
        statement.bindText(13, entity.localPath)
        val _tmpExtractedPath: String? = entity.extractedPath
        if (_tmpExtractedPath == null) {
          statement.bindNull(14)
        } else {
          statement.bindText(14, _tmpExtractedPath)
        }
        val _tmpSafTreeUri: String? = entity.safTreeUri
        if (_tmpSafTreeUri == null) {
          statement.bindNull(15)
        } else {
          statement.bindText(15, _tmpSafTreeUri)
        }
        statement.bindText(16, entity.gitBranch)
        statement.bindText(17, entity.gitCommitMessage)
        val _tmp_8: Int = if (entity.gitShallow) 1 else 0
        statement.bindLong(18, _tmp_8.toLong())
        statement.bindLong(19, entity.speedLimitBytesPerSec)
        statement.bindLong(20, entity.bytesDownloaded)
        statement.bindLong(21, entity.totalBytes)
        statement.bindLong(22, entity.speedBytesPerSec)
        statement.bindLong(23, entity.etaSeconds)
        statement.bindLong(24, entity.peersConnected.toLong())
        statement.bindLong(25, entity.seedsFound.toLong())
        statement.bindLong(26, entity.assetsDownloaded.toLong())
        statement.bindLong(27, entity.assetsTotal.toLong())
        statement.bindText(28, entity.statusDetail)
        val _tmpErrorMessage: String? = entity.errorMessage
        if (_tmpErrorMessage == null) {
          statement.bindNull(29)
        } else {
          statement.bindText(29, _tmpErrorMessage)
        }
        statement.bindLong(30, entity.createdAt)
        val _tmpCompletedAt: Long? = entity.completedAt
        if (_tmpCompletedAt == null) {
          statement.bindNull(31)
        } else {
          statement.bindLong(31, _tmpCompletedAt)
        }
      }
    }
    this.__updateAdapterOfDownloadTaskEntity = object :
        EntityDeleteOrUpdateAdapter<DownloadTaskEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `download_tasks` SET `id` = ?,`title` = ?,`sourceUrl` = ?,`kind` = ?,`priority` = ?,`status` = ?,`siteArchiveFormat` = ?,`rewriteSiteRefs` = ?,`includeSiteScripts` = ?,`downloadIntoFolder` = ?,`folderName` = ?,`extractOnDownload` = ?,`localPath` = ?,`extractedPath` = ?,`safTreeUri` = ?,`gitBranch` = ?,`gitCommitMessage` = ?,`gitShallow` = ?,`speedLimitBytesPerSec` = ?,`bytesDownloaded` = ?,`totalBytes` = ?,`speedBytesPerSec` = ?,`etaSeconds` = ?,`peersConnected` = ?,`seedsFound` = ?,`assetsDownloaded` = ?,`assetsTotal` = ?,`statusDetail` = ?,`errorMessage` = ?,`createdAt` = ?,`completedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: DownloadTaskEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.sourceUrl)
        val _tmp: String = __winFetchTypeConverters.fromKind(entity.kind)
        statement.bindText(4, _tmp)
        val _tmp_1: String = __winFetchTypeConverters.fromPriority(entity.priority)
        statement.bindText(5, _tmp_1)
        val _tmp_2: String = __winFetchTypeConverters.fromStatus(entity.status)
        statement.bindText(6, _tmp_2)
        val _tmp_3: String = __winFetchTypeConverters.fromSiteFormat(entity.siteArchiveFormat)
        statement.bindText(7, _tmp_3)
        val _tmp_4: Int = if (entity.rewriteSiteRefs) 1 else 0
        statement.bindLong(8, _tmp_4.toLong())
        val _tmp_5: Int = if (entity.includeSiteScripts) 1 else 0
        statement.bindLong(9, _tmp_5.toLong())
        val _tmp_6: Int = if (entity.downloadIntoFolder) 1 else 0
        statement.bindLong(10, _tmp_6.toLong())
        statement.bindText(11, entity.folderName)
        val _tmp_7: Int = if (entity.extractOnDownload) 1 else 0
        statement.bindLong(12, _tmp_7.toLong())
        statement.bindText(13, entity.localPath)
        val _tmpExtractedPath: String? = entity.extractedPath
        if (_tmpExtractedPath == null) {
          statement.bindNull(14)
        } else {
          statement.bindText(14, _tmpExtractedPath)
        }
        val _tmpSafTreeUri: String? = entity.safTreeUri
        if (_tmpSafTreeUri == null) {
          statement.bindNull(15)
        } else {
          statement.bindText(15, _tmpSafTreeUri)
        }
        statement.bindText(16, entity.gitBranch)
        statement.bindText(17, entity.gitCommitMessage)
        val _tmp_8: Int = if (entity.gitShallow) 1 else 0
        statement.bindLong(18, _tmp_8.toLong())
        statement.bindLong(19, entity.speedLimitBytesPerSec)
        statement.bindLong(20, entity.bytesDownloaded)
        statement.bindLong(21, entity.totalBytes)
        statement.bindLong(22, entity.speedBytesPerSec)
        statement.bindLong(23, entity.etaSeconds)
        statement.bindLong(24, entity.peersConnected.toLong())
        statement.bindLong(25, entity.seedsFound.toLong())
        statement.bindLong(26, entity.assetsDownloaded.toLong())
        statement.bindLong(27, entity.assetsTotal.toLong())
        statement.bindText(28, entity.statusDetail)
        val _tmpErrorMessage: String? = entity.errorMessage
        if (_tmpErrorMessage == null) {
          statement.bindNull(29)
        } else {
          statement.bindText(29, _tmpErrorMessage)
        }
        statement.bindLong(30, entity.createdAt)
        val _tmpCompletedAt: Long? = entity.completedAt
        if (_tmpCompletedAt == null) {
          statement.bindNull(31)
        } else {
          statement.bindLong(31, _tmpCompletedAt)
        }
        statement.bindText(32, entity.id)
      }
    }
  }

  public override suspend fun insertTask(task: DownloadTaskEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfDownloadTaskEntity.insert(_connection, task)
  }

  public override suspend fun updateTask(task: DownloadTaskEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __updateAdapterOfDownloadTaskEntity.handle(_connection, task)
  }

  public override fun observeAllTasks(): Flow<List<DownloadTaskEntity>> {
    val _sql: String = "SELECT * FROM download_tasks ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("download_tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSourceUrl: Int = getColumnIndexOrThrow(_stmt, "sourceUrl")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSiteArchiveFormat: Int = getColumnIndexOrThrow(_stmt, "siteArchiveFormat")
        val _columnIndexOfRewriteSiteRefs: Int = getColumnIndexOrThrow(_stmt, "rewriteSiteRefs")
        val _columnIndexOfIncludeSiteScripts: Int = getColumnIndexOrThrow(_stmt,
            "includeSiteScripts")
        val _columnIndexOfDownloadIntoFolder: Int = getColumnIndexOrThrow(_stmt,
            "downloadIntoFolder")
        val _columnIndexOfFolderName: Int = getColumnIndexOrThrow(_stmt, "folderName")
        val _columnIndexOfExtractOnDownload: Int = getColumnIndexOrThrow(_stmt, "extractOnDownload")
        val _columnIndexOfLocalPath: Int = getColumnIndexOrThrow(_stmt, "localPath")
        val _columnIndexOfExtractedPath: Int = getColumnIndexOrThrow(_stmt, "extractedPath")
        val _columnIndexOfSafTreeUri: Int = getColumnIndexOrThrow(_stmt, "safTreeUri")
        val _columnIndexOfGitBranch: Int = getColumnIndexOrThrow(_stmt, "gitBranch")
        val _columnIndexOfGitCommitMessage: Int = getColumnIndexOrThrow(_stmt, "gitCommitMessage")
        val _columnIndexOfGitShallow: Int = getColumnIndexOrThrow(_stmt, "gitShallow")
        val _columnIndexOfSpeedLimitBytesPerSec: Int = getColumnIndexOrThrow(_stmt,
            "speedLimitBytesPerSec")
        val _columnIndexOfBytesDownloaded: Int = getColumnIndexOrThrow(_stmt, "bytesDownloaded")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "totalBytes")
        val _columnIndexOfSpeedBytesPerSec: Int = getColumnIndexOrThrow(_stmt, "speedBytesPerSec")
        val _columnIndexOfEtaSeconds: Int = getColumnIndexOrThrow(_stmt, "etaSeconds")
        val _columnIndexOfPeersConnected: Int = getColumnIndexOrThrow(_stmt, "peersConnected")
        val _columnIndexOfSeedsFound: Int = getColumnIndexOrThrow(_stmt, "seedsFound")
        val _columnIndexOfAssetsDownloaded: Int = getColumnIndexOrThrow(_stmt, "assetsDownloaded")
        val _columnIndexOfAssetsTotal: Int = getColumnIndexOrThrow(_stmt, "assetsTotal")
        val _columnIndexOfStatusDetail: Int = getColumnIndexOrThrow(_stmt, "statusDetail")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _result: MutableList<DownloadTaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DownloadTaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSourceUrl: String
          _tmpSourceUrl = _stmt.getText(_columnIndexOfSourceUrl)
          val _tmpKind: DownloadKind
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfKind)
          _tmpKind = __winFetchTypeConverters.toKind(_tmp)
          val _tmpPriority: DownloadPriority
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __winFetchTypeConverters.toPriority(_tmp_1)
          val _tmpStatus: DownloadStatus
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfStatus)
          _tmpStatus = __winFetchTypeConverters.toStatus(_tmp_2)
          val _tmpSiteArchiveFormat: SiteArchiveFormat
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfSiteArchiveFormat)
          _tmpSiteArchiveFormat = __winFetchTypeConverters.toSiteFormat(_tmp_3)
          val _tmpRewriteSiteRefs: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfRewriteSiteRefs).toInt()
          _tmpRewriteSiteRefs = _tmp_4 != 0
          val _tmpIncludeSiteScripts: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfIncludeSiteScripts).toInt()
          _tmpIncludeSiteScripts = _tmp_5 != 0
          val _tmpDownloadIntoFolder: Boolean
          val _tmp_6: Int
          _tmp_6 = _stmt.getLong(_columnIndexOfDownloadIntoFolder).toInt()
          _tmpDownloadIntoFolder = _tmp_6 != 0
          val _tmpFolderName: String
          _tmpFolderName = _stmt.getText(_columnIndexOfFolderName)
          val _tmpExtractOnDownload: Boolean
          val _tmp_7: Int
          _tmp_7 = _stmt.getLong(_columnIndexOfExtractOnDownload).toInt()
          _tmpExtractOnDownload = _tmp_7 != 0
          val _tmpLocalPath: String
          _tmpLocalPath = _stmt.getText(_columnIndexOfLocalPath)
          val _tmpExtractedPath: String?
          if (_stmt.isNull(_columnIndexOfExtractedPath)) {
            _tmpExtractedPath = null
          } else {
            _tmpExtractedPath = _stmt.getText(_columnIndexOfExtractedPath)
          }
          val _tmpSafTreeUri: String?
          if (_stmt.isNull(_columnIndexOfSafTreeUri)) {
            _tmpSafTreeUri = null
          } else {
            _tmpSafTreeUri = _stmt.getText(_columnIndexOfSafTreeUri)
          }
          val _tmpGitBranch: String
          _tmpGitBranch = _stmt.getText(_columnIndexOfGitBranch)
          val _tmpGitCommitMessage: String
          _tmpGitCommitMessage = _stmt.getText(_columnIndexOfGitCommitMessage)
          val _tmpGitShallow: Boolean
          val _tmp_8: Int
          _tmp_8 = _stmt.getLong(_columnIndexOfGitShallow).toInt()
          _tmpGitShallow = _tmp_8 != 0
          val _tmpSpeedLimitBytesPerSec: Long
          _tmpSpeedLimitBytesPerSec = _stmt.getLong(_columnIndexOfSpeedLimitBytesPerSec)
          val _tmpBytesDownloaded: Long
          _tmpBytesDownloaded = _stmt.getLong(_columnIndexOfBytesDownloaded)
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpSpeedBytesPerSec: Long
          _tmpSpeedBytesPerSec = _stmt.getLong(_columnIndexOfSpeedBytesPerSec)
          val _tmpEtaSeconds: Long
          _tmpEtaSeconds = _stmt.getLong(_columnIndexOfEtaSeconds)
          val _tmpPeersConnected: Int
          _tmpPeersConnected = _stmt.getLong(_columnIndexOfPeersConnected).toInt()
          val _tmpSeedsFound: Int
          _tmpSeedsFound = _stmt.getLong(_columnIndexOfSeedsFound).toInt()
          val _tmpAssetsDownloaded: Int
          _tmpAssetsDownloaded = _stmt.getLong(_columnIndexOfAssetsDownloaded).toInt()
          val _tmpAssetsTotal: Int
          _tmpAssetsTotal = _stmt.getLong(_columnIndexOfAssetsTotal).toInt()
          val _tmpStatusDetail: String
          _tmpStatusDetail = _stmt.getText(_columnIndexOfStatusDetail)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          _item =
              DownloadTaskEntity(_tmpId,_tmpTitle,_tmpSourceUrl,_tmpKind,_tmpPriority,_tmpStatus,_tmpSiteArchiveFormat,_tmpRewriteSiteRefs,_tmpIncludeSiteScripts,_tmpDownloadIntoFolder,_tmpFolderName,_tmpExtractOnDownload,_tmpLocalPath,_tmpExtractedPath,_tmpSafTreeUri,_tmpGitBranch,_tmpGitCommitMessage,_tmpGitShallow,_tmpSpeedLimitBytesPerSec,_tmpBytesDownloaded,_tmpTotalBytes,_tmpSpeedBytesPerSec,_tmpEtaSeconds,_tmpPeersConnected,_tmpSeedsFound,_tmpAssetsDownloaded,_tmpAssetsTotal,_tmpStatusDetail,_tmpErrorMessage,_tmpCreatedAt,_tmpCompletedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTaskById(id: String): DownloadTaskEntity? {
    val _sql: String = "SELECT * FROM download_tasks WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSourceUrl: Int = getColumnIndexOrThrow(_stmt, "sourceUrl")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSiteArchiveFormat: Int = getColumnIndexOrThrow(_stmt, "siteArchiveFormat")
        val _columnIndexOfRewriteSiteRefs: Int = getColumnIndexOrThrow(_stmt, "rewriteSiteRefs")
        val _columnIndexOfIncludeSiteScripts: Int = getColumnIndexOrThrow(_stmt,
            "includeSiteScripts")
        val _columnIndexOfDownloadIntoFolder: Int = getColumnIndexOrThrow(_stmt,
            "downloadIntoFolder")
        val _columnIndexOfFolderName: Int = getColumnIndexOrThrow(_stmt, "folderName")
        val _columnIndexOfExtractOnDownload: Int = getColumnIndexOrThrow(_stmt, "extractOnDownload")
        val _columnIndexOfLocalPath: Int = getColumnIndexOrThrow(_stmt, "localPath")
        val _columnIndexOfExtractedPath: Int = getColumnIndexOrThrow(_stmt, "extractedPath")
        val _columnIndexOfSafTreeUri: Int = getColumnIndexOrThrow(_stmt, "safTreeUri")
        val _columnIndexOfGitBranch: Int = getColumnIndexOrThrow(_stmt, "gitBranch")
        val _columnIndexOfGitCommitMessage: Int = getColumnIndexOrThrow(_stmt, "gitCommitMessage")
        val _columnIndexOfGitShallow: Int = getColumnIndexOrThrow(_stmt, "gitShallow")
        val _columnIndexOfSpeedLimitBytesPerSec: Int = getColumnIndexOrThrow(_stmt,
            "speedLimitBytesPerSec")
        val _columnIndexOfBytesDownloaded: Int = getColumnIndexOrThrow(_stmt, "bytesDownloaded")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "totalBytes")
        val _columnIndexOfSpeedBytesPerSec: Int = getColumnIndexOrThrow(_stmt, "speedBytesPerSec")
        val _columnIndexOfEtaSeconds: Int = getColumnIndexOrThrow(_stmt, "etaSeconds")
        val _columnIndexOfPeersConnected: Int = getColumnIndexOrThrow(_stmt, "peersConnected")
        val _columnIndexOfSeedsFound: Int = getColumnIndexOrThrow(_stmt, "seedsFound")
        val _columnIndexOfAssetsDownloaded: Int = getColumnIndexOrThrow(_stmt, "assetsDownloaded")
        val _columnIndexOfAssetsTotal: Int = getColumnIndexOrThrow(_stmt, "assetsTotal")
        val _columnIndexOfStatusDetail: Int = getColumnIndexOrThrow(_stmt, "statusDetail")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _result: DownloadTaskEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSourceUrl: String
          _tmpSourceUrl = _stmt.getText(_columnIndexOfSourceUrl)
          val _tmpKind: DownloadKind
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfKind)
          _tmpKind = __winFetchTypeConverters.toKind(_tmp)
          val _tmpPriority: DownloadPriority
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __winFetchTypeConverters.toPriority(_tmp_1)
          val _tmpStatus: DownloadStatus
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfStatus)
          _tmpStatus = __winFetchTypeConverters.toStatus(_tmp_2)
          val _tmpSiteArchiveFormat: SiteArchiveFormat
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfSiteArchiveFormat)
          _tmpSiteArchiveFormat = __winFetchTypeConverters.toSiteFormat(_tmp_3)
          val _tmpRewriteSiteRefs: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfRewriteSiteRefs).toInt()
          _tmpRewriteSiteRefs = _tmp_4 != 0
          val _tmpIncludeSiteScripts: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfIncludeSiteScripts).toInt()
          _tmpIncludeSiteScripts = _tmp_5 != 0
          val _tmpDownloadIntoFolder: Boolean
          val _tmp_6: Int
          _tmp_6 = _stmt.getLong(_columnIndexOfDownloadIntoFolder).toInt()
          _tmpDownloadIntoFolder = _tmp_6 != 0
          val _tmpFolderName: String
          _tmpFolderName = _stmt.getText(_columnIndexOfFolderName)
          val _tmpExtractOnDownload: Boolean
          val _tmp_7: Int
          _tmp_7 = _stmt.getLong(_columnIndexOfExtractOnDownload).toInt()
          _tmpExtractOnDownload = _tmp_7 != 0
          val _tmpLocalPath: String
          _tmpLocalPath = _stmt.getText(_columnIndexOfLocalPath)
          val _tmpExtractedPath: String?
          if (_stmt.isNull(_columnIndexOfExtractedPath)) {
            _tmpExtractedPath = null
          } else {
            _tmpExtractedPath = _stmt.getText(_columnIndexOfExtractedPath)
          }
          val _tmpSafTreeUri: String?
          if (_stmt.isNull(_columnIndexOfSafTreeUri)) {
            _tmpSafTreeUri = null
          } else {
            _tmpSafTreeUri = _stmt.getText(_columnIndexOfSafTreeUri)
          }
          val _tmpGitBranch: String
          _tmpGitBranch = _stmt.getText(_columnIndexOfGitBranch)
          val _tmpGitCommitMessage: String
          _tmpGitCommitMessage = _stmt.getText(_columnIndexOfGitCommitMessage)
          val _tmpGitShallow: Boolean
          val _tmp_8: Int
          _tmp_8 = _stmt.getLong(_columnIndexOfGitShallow).toInt()
          _tmpGitShallow = _tmp_8 != 0
          val _tmpSpeedLimitBytesPerSec: Long
          _tmpSpeedLimitBytesPerSec = _stmt.getLong(_columnIndexOfSpeedLimitBytesPerSec)
          val _tmpBytesDownloaded: Long
          _tmpBytesDownloaded = _stmt.getLong(_columnIndexOfBytesDownloaded)
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpSpeedBytesPerSec: Long
          _tmpSpeedBytesPerSec = _stmt.getLong(_columnIndexOfSpeedBytesPerSec)
          val _tmpEtaSeconds: Long
          _tmpEtaSeconds = _stmt.getLong(_columnIndexOfEtaSeconds)
          val _tmpPeersConnected: Int
          _tmpPeersConnected = _stmt.getLong(_columnIndexOfPeersConnected).toInt()
          val _tmpSeedsFound: Int
          _tmpSeedsFound = _stmt.getLong(_columnIndexOfSeedsFound).toInt()
          val _tmpAssetsDownloaded: Int
          _tmpAssetsDownloaded = _stmt.getLong(_columnIndexOfAssetsDownloaded).toInt()
          val _tmpAssetsTotal: Int
          _tmpAssetsTotal = _stmt.getLong(_columnIndexOfAssetsTotal).toInt()
          val _tmpStatusDetail: String
          _tmpStatusDetail = _stmt.getText(_columnIndexOfStatusDetail)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          _result =
              DownloadTaskEntity(_tmpId,_tmpTitle,_tmpSourceUrl,_tmpKind,_tmpPriority,_tmpStatus,_tmpSiteArchiveFormat,_tmpRewriteSiteRefs,_tmpIncludeSiteScripts,_tmpDownloadIntoFolder,_tmpFolderName,_tmpExtractOnDownload,_tmpLocalPath,_tmpExtractedPath,_tmpSafTreeUri,_tmpGitBranch,_tmpGitCommitMessage,_tmpGitShallow,_tmpSpeedLimitBytesPerSec,_tmpBytesDownloaded,_tmpTotalBytes,_tmpSpeedBytesPerSec,_tmpEtaSeconds,_tmpPeersConnected,_tmpSeedsFound,_tmpAssetsDownloaded,_tmpAssetsTotal,_tmpStatusDetail,_tmpErrorMessage,_tmpCreatedAt,_tmpCompletedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllTasksOnce(): List<DownloadTaskEntity> {
    val _sql: String = "SELECT * FROM download_tasks ORDER BY createdAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSourceUrl: Int = getColumnIndexOrThrow(_stmt, "sourceUrl")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSiteArchiveFormat: Int = getColumnIndexOrThrow(_stmt, "siteArchiveFormat")
        val _columnIndexOfRewriteSiteRefs: Int = getColumnIndexOrThrow(_stmt, "rewriteSiteRefs")
        val _columnIndexOfIncludeSiteScripts: Int = getColumnIndexOrThrow(_stmt,
            "includeSiteScripts")
        val _columnIndexOfDownloadIntoFolder: Int = getColumnIndexOrThrow(_stmt,
            "downloadIntoFolder")
        val _columnIndexOfFolderName: Int = getColumnIndexOrThrow(_stmt, "folderName")
        val _columnIndexOfExtractOnDownload: Int = getColumnIndexOrThrow(_stmt, "extractOnDownload")
        val _columnIndexOfLocalPath: Int = getColumnIndexOrThrow(_stmt, "localPath")
        val _columnIndexOfExtractedPath: Int = getColumnIndexOrThrow(_stmt, "extractedPath")
        val _columnIndexOfSafTreeUri: Int = getColumnIndexOrThrow(_stmt, "safTreeUri")
        val _columnIndexOfGitBranch: Int = getColumnIndexOrThrow(_stmt, "gitBranch")
        val _columnIndexOfGitCommitMessage: Int = getColumnIndexOrThrow(_stmt, "gitCommitMessage")
        val _columnIndexOfGitShallow: Int = getColumnIndexOrThrow(_stmt, "gitShallow")
        val _columnIndexOfSpeedLimitBytesPerSec: Int = getColumnIndexOrThrow(_stmt,
            "speedLimitBytesPerSec")
        val _columnIndexOfBytesDownloaded: Int = getColumnIndexOrThrow(_stmt, "bytesDownloaded")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "totalBytes")
        val _columnIndexOfSpeedBytesPerSec: Int = getColumnIndexOrThrow(_stmt, "speedBytesPerSec")
        val _columnIndexOfEtaSeconds: Int = getColumnIndexOrThrow(_stmt, "etaSeconds")
        val _columnIndexOfPeersConnected: Int = getColumnIndexOrThrow(_stmt, "peersConnected")
        val _columnIndexOfSeedsFound: Int = getColumnIndexOrThrow(_stmt, "seedsFound")
        val _columnIndexOfAssetsDownloaded: Int = getColumnIndexOrThrow(_stmt, "assetsDownloaded")
        val _columnIndexOfAssetsTotal: Int = getColumnIndexOrThrow(_stmt, "assetsTotal")
        val _columnIndexOfStatusDetail: Int = getColumnIndexOrThrow(_stmt, "statusDetail")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _result: MutableList<DownloadTaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DownloadTaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSourceUrl: String
          _tmpSourceUrl = _stmt.getText(_columnIndexOfSourceUrl)
          val _tmpKind: DownloadKind
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfKind)
          _tmpKind = __winFetchTypeConverters.toKind(_tmp)
          val _tmpPriority: DownloadPriority
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfPriority)
          _tmpPriority = __winFetchTypeConverters.toPriority(_tmp_1)
          val _tmpStatus: DownloadStatus
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfStatus)
          _tmpStatus = __winFetchTypeConverters.toStatus(_tmp_2)
          val _tmpSiteArchiveFormat: SiteArchiveFormat
          val _tmp_3: String
          _tmp_3 = _stmt.getText(_columnIndexOfSiteArchiveFormat)
          _tmpSiteArchiveFormat = __winFetchTypeConverters.toSiteFormat(_tmp_3)
          val _tmpRewriteSiteRefs: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfRewriteSiteRefs).toInt()
          _tmpRewriteSiteRefs = _tmp_4 != 0
          val _tmpIncludeSiteScripts: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfIncludeSiteScripts).toInt()
          _tmpIncludeSiteScripts = _tmp_5 != 0
          val _tmpDownloadIntoFolder: Boolean
          val _tmp_6: Int
          _tmp_6 = _stmt.getLong(_columnIndexOfDownloadIntoFolder).toInt()
          _tmpDownloadIntoFolder = _tmp_6 != 0
          val _tmpFolderName: String
          _tmpFolderName = _stmt.getText(_columnIndexOfFolderName)
          val _tmpExtractOnDownload: Boolean
          val _tmp_7: Int
          _tmp_7 = _stmt.getLong(_columnIndexOfExtractOnDownload).toInt()
          _tmpExtractOnDownload = _tmp_7 != 0
          val _tmpLocalPath: String
          _tmpLocalPath = _stmt.getText(_columnIndexOfLocalPath)
          val _tmpExtractedPath: String?
          if (_stmt.isNull(_columnIndexOfExtractedPath)) {
            _tmpExtractedPath = null
          } else {
            _tmpExtractedPath = _stmt.getText(_columnIndexOfExtractedPath)
          }
          val _tmpSafTreeUri: String?
          if (_stmt.isNull(_columnIndexOfSafTreeUri)) {
            _tmpSafTreeUri = null
          } else {
            _tmpSafTreeUri = _stmt.getText(_columnIndexOfSafTreeUri)
          }
          val _tmpGitBranch: String
          _tmpGitBranch = _stmt.getText(_columnIndexOfGitBranch)
          val _tmpGitCommitMessage: String
          _tmpGitCommitMessage = _stmt.getText(_columnIndexOfGitCommitMessage)
          val _tmpGitShallow: Boolean
          val _tmp_8: Int
          _tmp_8 = _stmt.getLong(_columnIndexOfGitShallow).toInt()
          _tmpGitShallow = _tmp_8 != 0
          val _tmpSpeedLimitBytesPerSec: Long
          _tmpSpeedLimitBytesPerSec = _stmt.getLong(_columnIndexOfSpeedLimitBytesPerSec)
          val _tmpBytesDownloaded: Long
          _tmpBytesDownloaded = _stmt.getLong(_columnIndexOfBytesDownloaded)
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpSpeedBytesPerSec: Long
          _tmpSpeedBytesPerSec = _stmt.getLong(_columnIndexOfSpeedBytesPerSec)
          val _tmpEtaSeconds: Long
          _tmpEtaSeconds = _stmt.getLong(_columnIndexOfEtaSeconds)
          val _tmpPeersConnected: Int
          _tmpPeersConnected = _stmt.getLong(_columnIndexOfPeersConnected).toInt()
          val _tmpSeedsFound: Int
          _tmpSeedsFound = _stmt.getLong(_columnIndexOfSeedsFound).toInt()
          val _tmpAssetsDownloaded: Int
          _tmpAssetsDownloaded = _stmt.getLong(_columnIndexOfAssetsDownloaded).toInt()
          val _tmpAssetsTotal: Int
          _tmpAssetsTotal = _stmt.getLong(_columnIndexOfAssetsTotal).toInt()
          val _tmpStatusDetail: String
          _tmpStatusDetail = _stmt.getText(_columnIndexOfStatusDetail)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpCompletedAt: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAt)) {
            _tmpCompletedAt = null
          } else {
            _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          }
          _item =
              DownloadTaskEntity(_tmpId,_tmpTitle,_tmpSourceUrl,_tmpKind,_tmpPriority,_tmpStatus,_tmpSiteArchiveFormat,_tmpRewriteSiteRefs,_tmpIncludeSiteScripts,_tmpDownloadIntoFolder,_tmpFolderName,_tmpExtractOnDownload,_tmpLocalPath,_tmpExtractedPath,_tmpSafTreeUri,_tmpGitBranch,_tmpGitCommitMessage,_tmpGitShallow,_tmpSpeedLimitBytesPerSec,_tmpBytesDownloaded,_tmpTotalBytes,_tmpSpeedBytesPerSec,_tmpEtaSeconds,_tmpPeersConnected,_tmpSeedsFound,_tmpAssetsDownloaded,_tmpAssetsTotal,_tmpStatusDetail,_tmpErrorMessage,_tmpCreatedAt,_tmpCompletedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updatePriority(id: String, priority: DownloadPriority) {
    val _sql: String = "UPDATE download_tasks SET priority = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String = __winFetchTypeConverters.fromPriority(priority)
        _stmt.bindText(_argIndex, _tmp)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStatus(
    id: String,
    status: DownloadStatus,
    detail: String,
  ) {
    val _sql: String =
        "UPDATE download_tasks SET status = ?, speedBytesPerSec = 0, etaSeconds = -1, statusDetail = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String = __winFetchTypeConverters.fromStatus(status)
        _stmt.bindText(_argIndex, _tmp)
        _argIndex = 2
        _stmt.bindText(_argIndex, detail)
        _argIndex = 3
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: String) {
    val _sql: String = "DELETE FROM download_tasks WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearTerminalTasks() {
    val _sql: String =
        "DELETE FROM download_tasks WHERE status IN ('COMPLETED', 'FAILED', 'CANCELED')"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
