package com.lurenjia534.skydrivex.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.lurenjia534.skydrivex.data.local.media.LocalAlbum
import com.lurenjia534.skydrivex.data.local.media.LocalMediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class PhotoSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun loadAlbums(): List<LocalAlbum> = withContext(ioDispatcher) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = buildString {
            append(MediaStore.Images.Media.DATE_TAKEN)
            append(" DESC, ")
            append(MediaStore.Images.Media.DATE_ADDED)
            append(" DESC")
        }
        val imagesUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val accumulator = LinkedHashMap<String, Triple<String, Uri?, Int>>()

        resolver.query(imagesUri, projection, null, null, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdIndex) ?: continue
                val displayName = cursor.getString(bucketNameIndex).takeUnless { it.isNullOrBlank() } ?: "未命名"
                val imageId = cursor.getLong(idIndex)
                val current = accumulator[bucketId]
                val coverUri = current?.second ?: ContentUris.withAppendedId(imagesUri, imageId)
                val count = (current?.third ?: 0) + 1
                accumulator[bucketId] = Triple(displayName, coverUri, count)
            }
        }

        accumulator.map { (bucketId, triple) ->
            LocalAlbum(
                bucketId = bucketId,
                displayName = triple.first,
                coverUri = triple.second,
                itemCount = triple.third
            )
        }.sortedByDescending { it.itemCount }
    }

    suspend fun loadAlbumItems(bucketId: String): List<LocalMediaItem> = withContext(ioDispatcher) {
        val resolver = context.contentResolver
        val filesUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE
        )
        val selection = buildString {
            append(MediaStore.Files.FileColumns.BUCKET_ID)
            append("=? AND (")
            append(MediaStore.Files.FileColumns.MEDIA_TYPE)
            append("=? OR ")
            append(MediaStore.Files.FileColumns.MEDIA_TYPE)
            append("=?)")
        }
        val selectionArgs = arrayOf(
            bucketId,
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = buildString {
            append(MediaStore.Files.FileColumns.DATE_TAKEN)
            append(" DESC, ")
            append(MediaStore.Files.FileColumns.DATE_ADDED)
            append(" DESC")
        }

        val items = mutableListOf<LocalMediaItem>()

        val imagesBase = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val videosBase = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        resolver.query(filesUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val mediaTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
            val dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val dateModifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val ownerBucketId = cursor.getString(bucketIdIndex) ?: continue
                if (ownerBucketId != bucketId) continue
                val mime = cursor.getString(mimeIndex)
                val mediaType = cursor.getInt(mediaTypeIndex)
                val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val duration = if (isVideo) cursor.getLong(durationIndex) else 0L
                val dateTaken = cursor.getLong(dateTakenIndex).takeIf { it > 0 }
                    ?: (cursor.getLong(dateAddedIndex) * 1000)
                val dateModified = cursor.getLong(dateModifiedIndex).takeIf { it > 0 }?.times(1000)
                    ?: dateTaken
                val size = cursor.getLong(sizeIndex)
                val baseUri = if (isVideo) videosBase else imagesBase
                val uri = ContentUris.withAppendedId(baseUri, id)
                items += LocalMediaItem(
                    id = id,
                    uri = uri,
                    bucketId = ownerBucketId,
                    displayName = cursor.getString(nameIndex),
                    mimeType = mime,
                    isVideo = isVideo,
                    durationMillis = duration,
                    takenAtMillis = dateTaken,
                    modifiedAtMillis = dateModified ?: dateTaken,
                    sizeBytes = size
                )
            }
        }

        items.sortByDescending { it.takenAtMillis }
        items
    }
}
