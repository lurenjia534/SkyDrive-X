package com.lurenjia534.skydrivex.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.data.local.index.IndexDao
import com.lurenjia534.skydrivex.data.local.index.IndexPreferenceRepository
import com.lurenjia534.skydrivex.data.repository.IndexRepository
import com.lurenjia534.skydrivex.work.IndexWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LocalIndexSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: IndexPreferenceRepository,
    private val scheduler: IndexWorkScheduler,
    private val repository: IndexRepository,
    private val dao: IndexDao
) : ViewModel() {

    data class UiState(
        val enabled: Boolean = false,
        val wifiOnly: Boolean = true,
        val chargeOnly: Boolean = false,
        val indexedCount: Int = 0,
        val lastSyncText: String = "—"
    )

    private val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    val uiState = combine(prefs.snapshot, dao.countFlow()) { snapshot, count ->
        UiState(
            enabled = snapshot.enabled,
            wifiOnly = snapshot.wifiOnly,
            chargeOnly = snapshot.chargeOnly,
            indexedCount = count,
            lastSyncText = snapshot.lastSyncMillis.takeIf { it > 0L }
                ?.let { millis ->
                    val instant = java.time.Instant.ofEpochMilli(millis)
                    timeFormatter.format(instant.atZone(java.time.ZoneId.systemDefault()))
                } ?: "—"
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    fun setEnabled(value: Boolean) {
        viewModelScope.launch {
            prefs.setEnabled(value)
            if (value) scheduler.ensurePeriodic(context) else scheduler.cancelAll(context)
        }
    }

    fun setWifiOnly(value: Boolean) {
        viewModelScope.launch {
            prefs.setWifiOnly(value)
            scheduler.ensurePeriodic(context)
        }
    }

    fun setChargeOnly(value: Boolean) {
        viewModelScope.launch {
            prefs.setChargeOnly(value)
            scheduler.ensurePeriodic(context)
        }
    }

    fun clearIndex() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun rebuildNow() {
        viewModelScope.launch {
            scheduler.enqueueRebuild(context)
        }
    }
}

