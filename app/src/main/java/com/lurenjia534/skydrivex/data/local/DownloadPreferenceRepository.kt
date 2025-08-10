package com.lurenjia534.skydrivex.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFS_NAME_DOWNLOAD = "download_preferences"
private val Context.downloadDataStore by preferencesDataStore(name = PREFS_NAME_DOWNLOAD)

enum class DownloadLocationMode { SYSTEM_DOWNLOADS, CUSTOM_TREE }

data class DownloadLocationPreference(
    val mode: DownloadLocationMode = DownloadLocationMode.SYSTEM_DOWNLOADS,
    val treeUri: String? = null
)

@Singleton
class DownloadPreferenceRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val KEY_MODE = stringPreferencesKey("download_mode")
    private val KEY_TREE_URI = stringPreferencesKey("download_tree_uri")

    val downloadPreference: Flow<DownloadLocationPreference> =
        context.downloadDataStore.data.map { prefs ->
            val modeStr = prefs[KEY_MODE] ?: DownloadLocationMode.SYSTEM_DOWNLOADS.name
            val mode = runCatching { DownloadLocationMode.valueOf(modeStr) }
                .getOrDefault(DownloadLocationMode.SYSTEM_DOWNLOADS)
            val tree = prefs[KEY_TREE_URI]
            DownloadLocationPreference(mode = mode, treeUri = tree)
        }

    suspend fun setSystemDownloads() {
        context.downloadDataStore.edit { prefs ->
            prefs[KEY_MODE] = DownloadLocationMode.SYSTEM_DOWNLOADS.name
            prefs.remove(KEY_TREE_URI)
        }
    }

    suspend fun setCustomTree(uriString: String) {
        context.downloadDataStore.edit { prefs ->
            prefs[KEY_MODE] = DownloadLocationMode.CUSTOM_TREE.name
            prefs[KEY_TREE_URI] = uriString
        }
    }
}

