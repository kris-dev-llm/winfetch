package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DownloadPriority
import com.example.data.model.SiteArchiveFormat
import com.example.ui.theme.WinThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "winfetch_settings")

data class WinFetchSettings(
    val themeMode: WinThemeMode = WinThemeMode.SYSTEM,
    val maxConcurrentDownloads: Int = 3,
    val defaultPriority: DownloadPriority = DownloadPriority.QUICK,
    val defaultSiteFormat: SiteArchiveFormat = SiteArchiveFormat.ZIP,
    val defaultRewriteSiteRefs: Boolean = true,
    val defaultExtractOnDownload: Boolean = false
)

class AppPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent")
        val DEFAULT_PRIORITY = stringPreferencesKey("default_priority")
        val DEFAULT_SITE_FORMAT = stringPreferencesKey("default_site_format")
        val DEFAULT_REWRITE_REFS = booleanPreferencesKey("default_rewrite_refs")
        val DEFAULT_EXTRACT = booleanPreferencesKey("default_extract")
    }

    val settingsFlow: Flow<WinFetchSettings> = context.dataStore.data.map { prefs ->
        WinFetchSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { WinThemeMode.valueOf(it) }.getOrNull() } ?: WinThemeMode.SYSTEM,
            maxConcurrentDownloads = (prefs[Keys.MAX_CONCURRENT] ?: 3).coerceIn(1, 5),
            defaultPriority = prefs[Keys.DEFAULT_PRIORITY]?.let { runCatching { DownloadPriority.valueOf(it) }.getOrNull() } ?: DownloadPriority.QUICK,
            defaultSiteFormat = prefs[Keys.DEFAULT_SITE_FORMAT]?.let { runCatching { SiteArchiveFormat.valueOf(it) }.getOrNull() } ?: SiteArchiveFormat.ZIP,
            defaultRewriteSiteRefs = prefs[Keys.DEFAULT_REWRITE_REFS] ?: true,
            defaultExtractOnDownload = prefs[Keys.DEFAULT_EXTRACT] ?: false
        )
    }

    suspend fun setThemeMode(mode: WinThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setMaxConcurrent(count: Int) {
        context.dataStore.edit { it[Keys.MAX_CONCURRENT] = count.coerceIn(1, 5) }
    }

    suspend fun setDefaultPriority(priority: DownloadPriority) {
        context.dataStore.edit { it[Keys.DEFAULT_PRIORITY] = priority.name }
    }

    suspend fun setDefaultSiteFormat(format: SiteArchiveFormat) {
        context.dataStore.edit { it[Keys.DEFAULT_SITE_FORMAT] = format.name }
    }

    suspend fun setDefaultRewriteRefs(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_REWRITE_REFS] = enabled }
    }

    suspend fun setDefaultExtract(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_EXTRACT] = enabled }
    }
}
