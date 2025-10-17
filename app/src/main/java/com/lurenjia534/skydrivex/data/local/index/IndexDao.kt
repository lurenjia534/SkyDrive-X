package com.lurenjia534.skydrivex.data.local.index

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IndexDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<IndexItemEntity>)

    @Query("DELETE FROM index_items WHERE itemId IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM index_items")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM index_items")
    fun countFlow(): Flow<Int>

    @Query(
        "SELECT i.* FROM index_items i " +
            "JOIN index_items_fts f ON (i.rowid = f.rowid) " +
            "WHERE index_items_fts MATCH :ftsQuery " +
            "ORDER BY (i.lastModified IS NULL), i.lastModified DESC " +
            "LIMIT :limit"
    )
    suspend fun search(ftsQuery: String, limit: Int): List<IndexItemEntity>
}
