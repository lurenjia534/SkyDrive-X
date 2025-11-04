package com.lurenjia534.skydrivex.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.lurenjia534.skydrivex.data.local.media.LocalMediaItem
import com.lurenjia534.skydrivex.data.local.photosync.PhotoSyncLocalDataSource
import com.lurenjia534.skydrivex.data.local.photosync.UploadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PhotoBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filesRepository: FilesRepository,
    private val photoSyncRepository: PhotoSyncRepository,
    private val localDataSource: PhotoSyncLocalDataSource
) {

    data class BackupResult(
        val successCount: Int,
        val failureCount: Int,
        val totalPlanned: Int
    )

    companion object {
        private const val TAG = "PhotoBackup"
        private const val ROOT_BUCKET_ID = "__photo_root__"
        private const val ROOT_FOLDER_NAME = "SkyDrive X Photo Backups"
        private const val SMALL_FILE_THRESHOLD = 4L * 1024 * 1024 // 4 MiB
    }

    suspend fun backupAlbum(
        bearerToken: String,
        bucketId: String,
        albumName: String,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): BackupResult {
        Log.d(TAG, "backupAlbum start bucket=$bucketId name=$albumName")
        val rootFolderId = ensureRootFolder(bearerToken)
        val albumFolderId = ensureAlbumFolder(
            bearerToken = bearerToken,
            rootFolderId = rootFolderId,
            bucketId = bucketId,
            albumName = albumName
        )

        val items = photoSyncRepository.loadAlbumItems(bucketId)
        Log.d(TAG, "backupAlbum scan bucket=$bucketId total=${items.size}")
        if (items.isEmpty()) {
            onProgress(0, 0)
            return BackupResult(0, 0, 0)
        }

        val toUpload = items.filter { item ->
            val cached = localDataSource.getMediaUpload(item.uri.toString())
            if (cached == null) return@filter true
            when (cached.status) {
                UploadStatus.SUCCESS -> cached.dateModified < item.modifiedAtMillis || cached.size != item.sizeBytes
                UploadStatus.PENDING, UploadStatus.FAILED -> true
            }
        }
        Log.d(TAG, "backupAlbum pending bucket=$bucketId count=${toUpload.size}")

        if (toUpload.isEmpty()) {
            onProgress(items.size, items.size)
            return BackupResult(0, 0, 0)
        }

        var completed = 0
        val total = toUpload.size
        var success = 0
        var failed = 0
        onProgress(completed, total)
        val token = bearerToken

        for (media in toUpload) {
            val result = runCatching {
                uploadSingleMedia(token, albumFolderId, media)
            }
            if (result.isSuccess) {
                val itemId = result.getOrNull()
                Log.d(TAG, "upload success uri=${media.uri}")
                localDataSource.markUpload(
                    contentUri = media.uri.toString(),
                    bucketId = bucketId,
                    displayName = media.displayName,
                    size = media.sizeBytes,
                    dateModified = media.modifiedAtMillis,
                    remoteItemId = itemId,
                    status = UploadStatus.SUCCESS
                )
                success += 1
            } else {
                Log.e(TAG, "upload failed uri=${media.uri}", result.exceptionOrNull())
                localDataSource.markUpload(
                    contentUri = media.uri.toString(),
                    bucketId = bucketId,
                    displayName = media.displayName,
                    size = media.sizeBytes,
                    dateModified = media.modifiedAtMillis,
                    remoteItemId = null,
                    status = UploadStatus.FAILED
                )
                failed += 1
            }
            completed += 1
            onProgress(completed, total)
        }
        Log.d(TAG, "backupAlbum done bucket=$bucketId success=$success failed=$failed total=$total")
        return BackupResult(success, failed, total)
    }

    private suspend fun ensureRootFolder(bearerToken: String): String {
        val cached = localDataSource.getAlbumFolder(ROOT_BUCKET_ID)
        val expectedPath = "/$ROOT_FOLDER_NAME"
        Log.d(TAG, "ensureRootFolder cached=$cached expected=$expectedPath")
        if (cached != null && cached.remotePath == expectedPath) {
            val exists = folderExists(bearerToken, cached.remoteFolderId)
            if (exists) {
                return cached.remoteFolderId
            }
            Log.w(TAG, "ensureRootFolder cached folder missing remotely, recreating")
        }
        val children = filesRepository.getRootChildren(bearerToken).filter { it.folder != null }
        val existing = children.firstOrNull { it.name.equals(ROOT_FOLDER_NAME, ignoreCase = true) }
        val folderId = if (existing != null && existing.id != null) {
            Log.d(TAG, "ensureRootFolder found remote id=${existing.id}")
            existing.id
        } else {
            Log.d(TAG, "ensureRootFolder creating new root folder")
            val created = filesRepository.createFolder(
                parentId = "root",
                token = bearerToken,
                name = ROOT_FOLDER_NAME,
                conflictBehavior = "rename"
            )
            created.id ?: error("Failed to resolve root folder id")
        }
        if (cached != null && cached.remotePath != expectedPath) {
            Log.d(TAG, "ensureRootFolder path changed, reset root bucket uploads")
            localDataSource.resetUploadsForBucket(ROOT_BUCKET_ID)
        }

        localDataSource.setAlbumFolder(
            bucketId = ROOT_BUCKET_ID,
            albumName = ROOT_FOLDER_NAME,
            folderId = folderId,
            remotePath = expectedPath
        )
        return folderId
    }

    private suspend fun ensureAlbumFolder(
        bearerToken: String,
        rootFolderId: String,
        bucketId: String,
        albumName: String
    ): String {
        val safeName = slug(albumName)
        val expectedPath = "/$ROOT_FOLDER_NAME/$safeName"
        val existingMapping = localDataSource.getAlbumFolder(bucketId)
        Log.d(TAG, "ensureAlbumFolder cached=$existingMapping expected=$expectedPath")
        if (existingMapping != null && existingMapping.remotePath == expectedPath) {
            val exists = folderExists(bearerToken, existingMapping.remoteFolderId)
            if (exists) {
                return existingMapping.remoteFolderId
            }
            Log.w(TAG, "ensureAlbumFolder cached folder missing remotely, recreating bucket=$bucketId")
        }
        val children = filesRepository.getChildren(rootFolderId, bearerToken).filter { it.folder != null }
        val existing = children.firstOrNull { it.name.equals(safeName, ignoreCase = false) }
        val folderId = if (existing != null && existing.id != null) {
            Log.d(TAG, "ensureAlbumFolder found remote id=${existing.id}")
            existing.id
        } else {
            Log.d(TAG, "ensureAlbumFolder creating folder for $safeName")
            val created = filesRepository.createFolder(
                parentId = rootFolderId,
                token = bearerToken,
                name = safeName,
                conflictBehavior = "rename"
            )
            created.id ?: error("Failed to resolve album folder id")
        }

        localDataSource.setAlbumFolder(
            bucketId = bucketId,
            albumName = albumName,
            folderId = folderId,
            remotePath = expectedPath
        )
        if (existingMapping != null && existingMapping.remotePath != expectedPath) {
            Log.d(TAG, "ensureAlbumFolder path changed, reset bucket=$bucketId uploads")
            localDataSource.resetUploadsForBucket(bucketId)
        } else if (existingMapping != null) {
            Log.d(TAG, "ensureAlbumFolder remote folder recreated, reset bucket=$bucketId uploads")
            localDataSource.resetUploadsForBucket(bucketId)
        }
        return folderId
    }

    private suspend fun folderExists(bearerToken: String, folderId: String): Boolean {
        return runCatching {
            val item = filesRepository.getItemBasic(folderId, bearerToken)
            item.folder != null
        }.getOrElse {
            Log.w(TAG, "folderExists failed for id=$folderId : ${it.message}")
            false
        }
    }

    private suspend fun uploadSingleMedia(
        bearerToken: String,
        folderId: String,
        media: LocalMediaItem
    ): String {
        val resolver = context.contentResolver
        val uri = media.uri
        return if (media.sizeBytes in 1..SMALL_FILE_THRESHOLD) {
            val bytes = readFully(resolver, uri)
            filesRepository.uploadSmallFile(
                parentId = folderId,
                token = bearerToken,
                fileName = media.displayName ?: uri.lastPathSegment ?: "image.jpg",
                mimeType = media.mimeType,
                bytes = bytes
            ).id ?: error("Upload missing id")
        } else {
            uploadLargeFileSequential(
                bearerToken = bearerToken,
                folderId = folderId,
                media = media
            )
        }
    }

    private suspend fun uploadLargeFileSequential(
        bearerToken: String,
        folderId: String,
        media: LocalMediaItem
    ): String {
        val uri = media.uri
        return withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffered = stream.buffered()
                var position = 0L
                val provider: suspend (Long, Int) -> ByteArray = { offset, size ->
                    if (offset != position) {
                        skipFully(buffered, offset - position)
                        position = offset
                    }
                    val buffer = ByteArray(size)
                    var read = 0
                    while (read < size) {
                        val r = buffered.read(buffer, read, size - read)
                        if (r <= 0) break
                        read += r
                    }
                    position += read
                    if (read == size) buffer else buffer.copyOf(read)
                }
                filesRepository.uploadLargeFile(
                    parentId = folderId,
                    token = bearerToken,
                    fileName = media.displayName ?: uri.lastPathSegment ?: "image.jpg",
                    totalBytes = media.sizeBytes,
                    bytesProvider = provider
                ).id ?: error("Upload missing id")
            } ?: error("Cannot open stream for $uri")
        }
    }

    private suspend fun readFully(resolver: ContentResolver, uri: Uri): ByteArray =
        withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Cannot open stream for $uri")
        }

    private fun slug(name: String): String {
        val sanitized = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return sanitized.takeIf { it.isNotBlank() } ?: "Album"
    }

    private fun skipFully(stream: java.io.InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = stream.skip(remaining)
            if (skipped <= 0) break
            remaining -= skipped
        }
    }
}
