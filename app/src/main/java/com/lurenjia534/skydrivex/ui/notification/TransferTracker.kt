package com.lurenjia534.skydrivex.ui.notification

import com.lurenjia534.skydrivex.data.local.db.TransferRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * TransferTracker persists transfer states via Room while exposing a simple API.
 */
object TransferTracker {

    enum class TransferType { DOWNLOAD_SYSTEM, DOWNLOAD_CUSTOM, UPLOAD }

    enum class Status { RUNNING, SUCCESS, FAILED, CANCELLED }

    data class Entry(
        val notificationId: Int,
        val title: String,
        val type: TransferType,
        val status: Status,
        val progress: Int?,
        val indeterminate: Boolean,
        val allowCancel: Boolean,
        val message: String? = null,
        val startedAt: Long = System.currentTimeMillis(),
        val completedAt: Long? = null
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private lateinit var repository: TransferRepository

    fun initialize(repo: TransferRepository) {
        if (::repository.isInitialized) return
        repository = repo
        scope.launch {
            repository.observeTransfers().collectLatest { list ->
                _entries.value = list
            }
        }
    }

    private fun ensureRepo(): TransferRepository {
        check(::repository.isInitialized) { "TransferTracker.initialize must be called before use" }
        return repository
    }

    fun start(
        notificationId: Int,
        title: String,
        type: TransferType,
        allowCancel: Boolean,
        indeterminate: Boolean
    ) {
        val repo = ensureRepo()
        scope.launch {
            repo.upsert(
                Entry(
                    notificationId = notificationId,
                    title = title,
                    type = type,
                    status = Status.RUNNING,
                    progress = null,
                    indeterminate = indeterminate,
                    allowCancel = allowCancel,
                    message = null,
                    startedAt = System.currentTimeMillis(),
                    completedAt = null
                )
            )
        }
    }

    fun updateProgress(notificationId: Int, progress: Int?, indeterminate: Boolean) {
        val repo = ensureRepo()
        scope.launch {
            repo.updateStatus(
                notificationId = notificationId,
                status = Status.RUNNING,
                progress = progress,
                indeterminate = indeterminate,
                message = null,
                completedAt = null
            )
        }
    }

    fun markSuccess(notificationId: Int, message: String? = null) {
        markStatus(notificationId, Status.SUCCESS, message)
    }

    fun markFailed(notificationId: Int, message: String? = null) {
        markStatus(notificationId, Status.FAILED, message)
    }

    fun markCancelled(notificationId: Int, message: String? = null) {
        markStatus(notificationId, Status.CANCELLED, message)
    }

    fun remove(notificationId: Int) {
        val repo = ensureRepo()
        scope.launch { repo.remove(notificationId) }
    }

    fun clearFinished() {
        val repo = ensureRepo()
        scope.launch { repo.clearFinished() }
    }

    private fun markStatus(notificationId: Int, status: Status, message: String?) {
        val repo = ensureRepo()
        scope.launch {
            repo.updateStatus(
                notificationId = notificationId,
                status = status,
                progress = if (status == Status.SUCCESS) 100 else null,
                indeterminate = false,
                message = message,
                completedAt = System.currentTimeMillis()
            )
        }
    }
}
