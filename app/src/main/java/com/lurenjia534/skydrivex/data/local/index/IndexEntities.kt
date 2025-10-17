package com.lurenjia534.skydrivex.data.local.index

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

@Entity(tableName = "index_items")
data class IndexItemEntity(
    @PrimaryKey val itemId: String,
    val driveId: String,
    val parentId: String?,
    val nameLower: String,
    val ext: String?,
    val isFolder: Boolean,
    val size: Long?,
    val lastModified: Long?,
    val pathLower: String?,
    val lastIndexedAt: Long
)

@Fts4(
    contentEntity = IndexItemEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=2"]
)
@Entity(tableName = "index_items_fts")
data class IndexItemFts(
    val nameLower: String,
    val pathLower: String,
    val ext: String?
)

