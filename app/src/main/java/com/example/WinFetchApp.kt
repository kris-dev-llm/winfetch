package com.example

import android.app.Application
import com.example.data.local.WinFetchDatabase
import com.example.data.prefs.AppPreferencesRepository
import com.example.data.repository.DownloadRepository
import com.example.engine.DownloadManagerEngine

class WinFetchApp : Application() {
    lateinit var database: WinFetchDatabase
        private set
    lateinit var preferencesRepository: AppPreferencesRepository
        private set
    lateinit var downloadEngine: DownloadManagerEngine
        private set
    lateinit var downloadRepository: DownloadRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = WinFetchDatabase.getInstance(this)
        preferencesRepository = AppPreferencesRepository(this)
        downloadEngine = DownloadManagerEngine(
            context = this,
            dao = database.downloadTaskDao(),
            prefsRepo = preferencesRepository
        )
        downloadRepository = DownloadRepository(
            dao = database.downloadTaskDao(),
            engine = downloadEngine
        )
    }
}
