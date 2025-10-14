package com.lurenjia534.skydrivex.ui.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 内存态传输任务跟踪器，统一收集下载/上传进度供 UI 展示。
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

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun start(
        notificationId: Int,
        title: String,
        type: TransferType,
        allowCancel: Boolean,
        indeterminate: Boolean
    ) {
        _entries.update { current ->
            val filtered = current.filterNot { it.notificationId == notificationId }
            filtered + Entry(
                notificationId = notificationId,
                title = title,
                type = type,
                status = Status.RUNNING,
                progress = null,
                indeterminate = indeterminate,
                allowCancel = allowCancel
            )
        }
    }

    fun updateProgress(notificationId: Int, progress: Int?, indeterminate: Boolean) {
        _entries.update { list ->
            list.map { entry ->
                if (entry.notificationId != notificationId) return@map entry
                entry.copy(
                    progress = progress,
                    indeterminate = indeterminate
                )
            }
        }
    }

    fun markSuccess(notificationId: Int, message: String? = null) {
        updateStatus(notificationId, Status.SUCCESS, message)
    }

    fun markFailed(notificationId: Int, message: String? = null) {
        updateStatus(notificationId, Status.FAILED, message)
    }

    fun markCancelled(notificationId: Int, message: String? = null) {
        updateStatus(notificationId, Status.CANCELLED, message)
    }

    fun remove(notificationId: Int) {
        _entries.update { list ->
            list.filterNot { it.notificationId == notificationId }
        }
    }

    fun clearFinished() {
        _entries.update { list ->
            list.filter { it.status == Status.RUNNING }
        }
    }

    private fun updateStatus(notificationId: Int, status: Status, message: String?) {
        val completedAt = System.currentTimeMillis()
        _entries.update { list ->
            list.map { entry ->
                if (entry.notificationId != notificationId) return@map entry
                entry.copy(
                    status = status,
                    completedAt = completedAt,
                    message = message
                )
            }
        }
    }
}
