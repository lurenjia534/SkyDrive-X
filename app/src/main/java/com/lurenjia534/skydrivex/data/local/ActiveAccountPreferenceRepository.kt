package com.lurenjia534.skydrivex.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private const val PREFS_NAME = "active_account_preferences"
private val Context.activeAccountDataStore by preferencesDataStore(name = PREFS_NAME)

@Singleton
/**
 * 负责将“激活账户 ID”持久化到 DataStore 的仓库：
 * - 通过 [activeAccountId] 流监听当前激活账户变化
 * - 提供读/写/清除 API，供认证与 UI 层共用
 */
class ActiveAccountPreferenceRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val ACTIVE_ACCOUNT_ID = stringPreferencesKey("active_account_id")

    val activeAccountId: Flow<String?> = context.activeAccountDataStore.data.map { preferences ->
        preferences[ACTIVE_ACCOUNT_ID]
    }

    suspend fun getActiveAccountId(): String? = activeAccountId.firstOrNull()

    suspend fun setActiveAccountId(id: String) {
        context.activeAccountDataStore.edit { preferences ->
            preferences[ACTIVE_ACCOUNT_ID] = id
        }
    }

    suspend fun clearActiveAccountId() {
        context.activeAccountDataStore.edit { preferences ->
            preferences.remove(ACTIVE_ACCOUNT_ID)
        }
    }
}
