package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DownloadPriority
import com.example.data.model.SiteArchiveFormat
import com.example.data.model.SpeedUnit
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
    val defaultExtractOnDownload: Boolean = false,
    val speedLimitEnabled: Boolean = false,
    val speedLimitValue: Long = 512L,
    val speedLimitUnit: SpeedUnit = SpeedUnit.KIB_BYTE
) {
    val effectiveLimitBytesPerSec: Long
        get() = if (speedLimitEnabled && speedLimitValue > 0L) {
            speedLimitUnit.toBytesPerSecond(speedLimitValue)
        } else 0L
}

class AppPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent")
        val DEFAULT_PRIORITY = stringPreferencesKey("default_priority")
        val DEFAULT_SITE_FORMAT = stringPreferencesKey("default_site_format")
        val DEFAULT_REWRITE_REFS = booleanPreferencesKey("default_rewrite_refs")
        val DEFAULT_EXTRACT = booleanPreferencesKey("default_extract")
        val SPEED_LIMIT_ENABLED = booleanPreferencesKey("speed_limit_enabled")
        val SPEED_LIMIT_VALUE = longPreferencesKey("speed_limit_value")
        val SPEED_LIMIT_UNIT = stringPreferencesKey("speed_limit_unit")
    }

    val settingsFlow: Flow<WinFetchSettings> = context.dataStore.data.map { prefs ->
        WinFetchSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { WinThemeMode.valueOf(it) }.getOrNull() } ?: WinThemeMode.SYSTEM,
            maxConcurrentDownloads = (prefs[Keys.MAX_CONCURRENT] ?: 3).coerceIn(1, 5),
            defaultPriority = prefs[Keys.DEFAULT_PRIORITY]?.let { runCatching { DownloadPriority.valueOf(it) }.getOrNull() } ?: DownloadPriority.QUICK,
            defaultSiteFormat = prefs[Keys.DEFAULT_SITE_FORMAT]?.let { runCatching { SiteArchiveFormat.valueOf(it) }.getOrNull() } ?: SiteArchiveFormat.ZIP,
            defaultRewriteSiteRefs = prefs[Keys.DEFAULT_REWRITE_REFS] ?: true,
            defaultExtractOnDownload = prefs[Keys.DEFAULT_EXTRACT] ?: false,
            speedLimitEnabled = prefs[Keys.SPEED_LIMIT_ENABLED] ?: false,
            speedLimitValue = (prefs[Keys.SPEED_LIMIT_VALUE] ?: 512L).coerceAtLeast(1L),
            speedLimitUnit = prefs[Keys.SPEED_LIMIT_UNIT]?.let { runCatching { SpeedUnit.valueOf(it) }.getOrNull() } ?: SpeedUnit.KIB_BYTE
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

    suspend fun setSpeedLimit(enabled: Boolean, value: Long, unit: SpeedUnit) {
        context.dataStore.edit {
            it[Keys.SPEED_LIMIT_ENABLED] = enabled
            it[Keys.SPEED_LIMIT_VALUE] = value.coerceAtLeast(1L)
            it[Keys.SPEED_LIMIT_UNIT] = unit.name
        }
    }
}
