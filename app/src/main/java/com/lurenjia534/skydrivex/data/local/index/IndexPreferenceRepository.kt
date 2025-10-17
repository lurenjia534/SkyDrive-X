package com.lurenjia534.skydrivex.data.local.index

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATA_STORE_NAME = "index_preferences"

private val Context.indexDataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)

@Singleton
class IndexPreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class Snapshot(
        val enabled: Boolean,
        val wifiOnly: Boolean,
        val chargeOnly: Boolean,
        val deltaLink: String?,
        val lastSyncMillis: Long
    )

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CHARGE_ONLY = booleanPreferencesKey("charge_only")
        val DELTA_LINK = stringPreferencesKey("delta_link")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }

    val snapshot: Flow<Snapshot> = context.indexDataStore.data.map { prefs ->
        Snapshot(
            enabled = prefs[Keys.ENABLED] ?: false,
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: true,
            chargeOnly = prefs[Keys.CHARGE_ONLY] ?: false,
            deltaLink = prefs[Keys.DELTA_LINK],
            lastSyncMillis = prefs[Keys.LAST_SYNC] ?: 0L
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.indexDataStore.edit { prefs ->
            prefs[Keys.ENABLED] = enabled
        }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.indexDataStore.edit { prefs ->
            prefs[Keys.WIFI_ONLY] = enabled
        }
    }

    suspend fun setChargeOnly(enabled: Boolean) {
        context.indexDataStore.edit { prefs ->
            prefs[Keys.CHARGE_ONLY] = enabled
        }
    }

    suspend fun updateDeltaLink(deltaLink: String?) {
        context.indexDataStore.edit { prefs ->
            if (deltaLink == null) prefs.remove(Keys.DELTA_LINK)
            else prefs[Keys.DELTA_LINK] = deltaLink
        }
    }

    suspend fun updateLastSync(millis: Long) {
        context.indexDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC] = millis
        }
    }

    suspend fun resetForRebuild() {
        context.indexDataStore.edit { prefs ->
            prefs.remove(Keys.DELTA_LINK)
            prefs[Keys.LAST_SYNC] = 0L
        }
    }
}
