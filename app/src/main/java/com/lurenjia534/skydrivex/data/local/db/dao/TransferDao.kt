package com.lurenjia534.skydrivex.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lurenjia534.skydrivex.data.local.db.TransferEntity
import com.lurenjia534.skydrivex.data.local.db.model.TransferStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Query("SELECT * FROM transfers ORDER BY started_at DESC")
    fun observeTransfers(): Flow<List<TransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TransferEntity>)

    @Update
    suspend fun update(entity: TransferEntity)

    @Query("DELETE FROM transfers WHERE notification_id = :notificationId")
    suspend fun delete(notificationId: Int)

    @Query("DELETE FROM transfers WHERE status != :runningStatus")
    suspend fun clearFinished(runningStatus: TransferStatus = TransferStatus.RUNNING)

    @Query("SELECT * FROM transfers WHERE notification_id = :notificationId LIMIT 1")
    suspend fun findById(notificationId: Int): TransferEntity?

    @Query(
        """
        UPDATE transfers
        SET progress = :progress,
            indeterminate = :indeterminate,
            message = CASE WHEN :message IS NOT NULL THEN :message ELSE message END
        WHERE notification_id = :notificationId AND status = :runningStatus
        """
    )
    suspend fun updateProgressIfRunning(
        notificationId: Int,
        progress: Int?,
        indeterminate: Boolean,
        message: String?,
        runningStatus: TransferStatus = TransferStatus.RUNNING
    ): Int
}
