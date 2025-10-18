package com.lurenjia534.skydrivex.data.repository

import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto
import com.lurenjia534.skydrivex.data.remote.GraphApiService
import com.lurenjia534.skydrivex.data.model.permission.CreateLinkRequest
import com.lurenjia534.skydrivex.data.model.driveitem.CreateFolderBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import com.squareup.moshi.Moshi
import com.lurenjia534.skydrivex.data.model.upload.CreateUploadSessionRequest
import com.lurenjia534.skydrivex.data.model.upload.DriveItemUploadableProperties
import retrofit2.HttpException
import android.util.Log
import android.net.Uri
import com.lurenjia534.skydrivex.data.model.driveitem.MoveItemBody
import com.lurenjia534.skydrivex.data.model.driveitem.ParentReferenceUpdate
import com.lurenjia534.skydrivex.data.model.driveitem.CopyItemBody
import com.lurenjia534.skydrivex.data.model.driveitem.CopyParentReference
import com.lurenjia534.skydrivex.data.model.driveitem.CopyMonitorStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import com.lurenjia534.skydrivex.data.model.batch.BatchRequest
import com.lurenjia534.skydrivex.data.model.batch.BatchSubRequest

private const val TAG_UPLOAD_SESSION = "UploadSession"

@Singleton
class FilesRepository @Inject constructor(
    private val graphApiService: GraphApiService,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {
    suspend fun getRootId(token: String): String? =
        graphApiService.getRootItem(token).id

    suspend fun getRootChildren(token: String): List<DriveItemDto> =
        fetchChildrenWithThumbnails(parentId = null, token = token)

    suspend fun getChildren(itemId: String, token: String): List<DriveItemDto> =
        fetchChildrenWithThumbnails(parentId = itemId, token = token)

    private suspend fun fetchChildrenWithThumbnails(parentId: String?, token: String): List<DriveItemDto> {
        // First try using $expand=thumbnails (works well on personal; may be ignored on Business)
        val expanded = runCatching {
            if (parentId == null || parentId == "root") {
                graphApiService.getRootChildrenExpanded(token = token).value
            } else {
                graphApiService.getChildrenExpanded(id = parentId, token = token).value
            }
        }.getOrNull()

        if (expanded != null) {
            if (expanded.any { it.thumbnails?.isNotEmpty() == true }) {
                return expanded
            }
            // Fallback enrich if expanded present but thumbnails missing (likely Business tenant)
            return enrichWithThumbnails(expanded, token)
        }

        // If expanded request failed, fallback to basic listing then enrich
        val basic = if (parentId == null || parentId == "root")
            graphApiService.getRootChildren(token).value
        else
            graphApiService.getChildren(parentId, token).value
        return enrichWithThumbnails(basic, token)
    }

    private suspend fun enrichWithThumbnails(items: List<DriveItemDto>, token: String): List<DriveItemDto> = coroutineScope {
        // Only fetch for file items that don't already have thumbnails
        val tasks = items.map { item ->
            async(Dispatchers.IO) {
                if (item.file == null || item.id.isNullOrEmpty()) return@async item
                if (!item.thumbnails.isNullOrEmpty()) return@async item
                val thumbs = runCatching {
                    graphApiService.getThumbnails(
                        itemId = item.id,
                        token = token,
                        select = "small,smallSquare,medium,mediumSquare"
                    ).value
                }.getOrNull()
                if (thumbs.isNullOrEmpty()) item else item.copy(thumbnails = thumbs)
            }
        }
        tasks.map { it.await() }
    }

    suspend fun deleteFile(itemId: String, token: String) {
        graphApiService.deleteFile(id = itemId, token = token)
    }

    suspend fun deleteFilesBatch(itemIds: List<String>, token: String): Map<String, Int> {
        if (itemIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, Int>()
        var counter = 1
        itemIds.chunked(20).forEach { chunk ->
            val idMapping = mutableMapOf<String, String>()
            val requests = chunk.map { itemId ->
                val requestId = (counter++).toString()
                idMapping[requestId] = itemId
                BatchSubRequest(
                    id = requestId,
                    method = "DELETE",
                    url = "/me/drive/items/$itemId"
                )
            }
            val response = graphApiService.batch(
                token = token,
                body = BatchRequest(requests)
            )
            val errors = mutableListOf<Pair<String, Int>>()
            response.responses.forEach { sub ->
                val originalId = idMapping[sub.id] ?: return@forEach
                result[originalId] = sub.status
                val ok = sub.status in 200..299 || sub.status == 404
                if (!ok) {
                    errors += originalId to sub.status
                }
            }
            if (errors.isNotEmpty()) {
                val summary = errors.joinToString { (id, status) -> "$id:$status" }
                throw IllegalStateException("批量删除失败: $summary")
            }
        }
        return result
    }

    suspend fun getDownloadUrl(itemId: String, token: String): String? =
        graphApiService.getDownloadUrl(id = itemId, token = token).downloadUrl

    suspend fun getItemDetails(itemId: String, token: String): DriveItemDto =
        graphApiService.getItemDetails(itemId = itemId, token = token)

    suspend fun createShareLink(
        itemId: String,
        token: String,
        type: String,                 // view | edit | embed
        scope: String?,               // anonymous | organization
        password: String?,            // only valid for personal + anonymous
        expirationDateTime: String?   // RFC3339 or null
    ): String? {
        val body = CreateLinkRequest(
            type = type,
            scope = scope,
            password = password,
            expirationDateTime = expirationDateTime
        )
        val resp = graphApiService.createLink(id = itemId, token = token, body = body)
        return resp.link?.webUrl
    }

    suspend fun uploadSmallFile(
        parentId: String?,
        token: String,
        fileName: String,
        mimeType: String?,
        bytes: ByteArray
    ): DriveItemDto {
        val ct = (mimeType ?: "application/octet-stream")
        val body: RequestBody = bytes.toRequestBody(ct.toMediaTypeOrNull())
        return if (parentId == null || parentId == "root") {
            graphApiService.uploadSmallFileToRoot(
                fileName = fileName,
                token = token,
                contentType = ct,
                body = body
            )
        } else {
            graphApiService.uploadSmallFileToParent(
                parentId = parentId,
                fileName = fileName,
                token = token,
                contentType = ct,
                body = body
            )
        }.also {
        }
    }

    suspend fun replaceSmallFileContent(
        itemId: String,
        token: String,
        mimeType: String?,
        bytes: ByteArray
    ): DriveItemDto {
        val ct = (mimeType ?: "application/octet-stream")
        val body: RequestBody = bytes.toRequestBody(ct.toMediaTypeOrNull())
        return graphApiService.replaceSmallFileContent(
            itemId = itemId,
            token = token,
            contentType = ct,
            body = body
        )
    }

    suspend fun createFolder(
        parentId: String?,
        token: String,
        name: String,
        conflictBehavior: String = "rename"
    ): DriveItemDto {
        val body = CreateFolderBody(name = name, conflictBehavior = conflictBehavior)
        return if (parentId == null || parentId == "root") {
            graphApiService.createFolderUnderRoot(token = token, body = body)
        } else {
            graphApiService.createFolderUnderParent(parentId = parentId, token = token, body = body)
        }
    }

    // Search helpers
    suspend fun searchInDrive(
        token: String,
        query: String,
        top: Int? = null,
        orderBy: String? = null,
        select: String? = null,
        expand: String? = null,
        skipToken: String? = null
    ): Pair<List<DriveItemDto>, String?> {
        val resp = graphApiService.searchInDrive(
            q = query,
            token = token,
            top = top,
            orderBy = orderBy,
            select = select,
            expand = expand,
            skipToken = skipToken
        )
        return resp.value to resp.nextLink
    }

    suspend fun searchInRoot(
        token: String,
        query: String,
        top: Int? = null,
        orderBy: String? = null,
        select: String? = null,
        expand: String? = null,
        skipToken: String? = null
    ): Pair<List<DriveItemDto>, String?> {
        val resp = graphApiService.searchInRoot(
            q = query,
            token = token,
            top = top,
            orderBy = orderBy,
            select = select,
            expand = expand,
            skipToken = skipToken
        )
        return resp.value to resp.nextLink
    }

    suspend fun searchInFolder(
        folderId: String,
        token: String,
        query: String,
        top: Int? = null,
        orderBy: String? = null,
        select: String? = null,
        expand: String? = null,
        skipToken: String? = null
    ): Pair<List<DriveItemDto>, String?> {
        val resp = graphApiService.searchInFolder(
            itemId = folderId,
            q = query,
            token = token,
            top = top,
            orderBy = orderBy,
            select = select,
            expand = expand,
            skipToken = skipToken
        )
        return resp.value to resp.nextLink
    }

    // Convenience wrappers to provide thumbnails for search results as well (with fallback)
    suspend fun searchInDriveWithThumbnails(
        token: String,
        query: String,
        top: Int? = 50
    ): Pair<List<DriveItemDto>, String?> {
        val (items, next) = searchInDrive(
            token = token,
            query = query,
            top = top,
            select = "id,name,size,parentReference,folder,file,thumbnails",
            expand = "thumbnails"
        )
        val haveAny = items.any { it.thumbnails?.isNotEmpty() == true }
        return if (haveAny) items to next else enrichWithThumbnails(items, token) to next
    }

    suspend fun searchInFolderWithThumbnails(
        folderId: String,
        token: String,
        query: String,
        top: Int? = 50
    ): Pair<List<DriveItemDto>, String?> {
        val (items, next) = searchInFolder(
            folderId = folderId,
            token = token,
            query = query,
            top = top,
            select = "id,name,size,parentReference,folder,file,thumbnails",
            expand = "thumbnails"
        )
        val haveAny = items.any { it.thumbnails?.isNotEmpty() == true }
        return if (haveAny) items to next else enrichWithThumbnails(items, token) to next
    }

    // Large file upload via Upload Session.
    suspend fun uploadLargeFile(
        parentId: String?,
        token: String,
        fileName: String,
        totalBytes: Long,
        bytesProvider: suspend (offset: Long, size: Int) -> ByteArray,
        cancelFlag: AtomicBoolean? = null,
        onProgress: ((uploaded: Long, total: Long) -> Unit)? = null
    ): DriveItemDto {
        // Keep body minimal to avoid invalidRequest on some tenants; include only conflict behavior.
        val req = CreateUploadSessionRequest(
            item = DriveItemUploadableProperties(
                conflictBehavior = "rename"
            )
        )
        val encodedName = Uri.encode(fileName)
        val session = try {
            if (parentId == null || parentId == "root") {
                graphApiService.createUploadSessionForRoot(
                    fileName = encodedName,
                    token = token,
                    body = req
                )
            } else {
                graphApiService.createUploadSessionForNew(
                    parentId = parentId,
                    fileName = encodedName,
                    token = token,
                    body = req
                )
            }
        } catch (e: HttpException) {
            val err = e.response()?.errorBody()?.string()
            val msg = buildString {
                append("CreateUploadSession failed: HTTP ")
                append(e.code())
                append(" file=")
                append(fileName)
                if (!err.isNullOrBlank()) {
                    append(": ")
                    append(err.take(300))
                }
            }
            Log.e("UploadSession", msg, e)
            error(msg)
        }
        val uploadUrl = session.uploadUrl
        val chunkSize = 5 * 1024 * 1024 // 5 MiB (multiple of 320 KiB)
        var uploaded = 0L
        while (uploaded < totalBytes) {
            if (cancelFlag?.get() == true) throw java.util.concurrent.CancellationException("Cancelled")
            val remaining = (totalBytes - uploaded).toInt()
            val want = if (remaining < chunkSize) remaining else chunkSize
            val chunk = bytesProvider(uploaded, want)
            val actual = chunk.size
            if (actual <= 0) {
                val emsg = "No data read at offset=$uploaded"
                Log.e("UploadSession", emsg)
                error(emsg)
            }
            val end = uploaded + actual - 1
            val request = Request.Builder()
                .url(uploadUrl)
                .put(chunk.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
                .header("Content-Length", actual.toString())
                .header("Content-Range", "bytes ${uploaded}-${end}/${totalBytes}")
                .build()
            var completedItem: DriveItemDto? = null
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { resp ->
                    if (resp.code == 202) {
                        // safe to update primitive from IO context; read back on caller
                        uploaded += actual
                        onProgress?.invoke(uploaded, totalBytes)
                    } else if (resp.code == 201 || resp.code == 200) {
                        val adapter = moshi.adapter(DriveItemDto::class.java)
                        val bodyStr = resp.body.string()
                        val item = adapter.fromJson(bodyStr)
                            ?: throw IllegalStateException("Empty item response")
                        completedItem = item
                    } else {
                        val errBody = try { resp.body.string() } catch (_: Exception) { null }
                        // Query session status (IO thread) for nextExpectedRanges
                        val statusMsg = runCatching {
                            val statusReq = Request.Builder().url(uploadUrl).get().build()
                            okHttpClient.newCall(statusReq).execute().use { sResp ->
                                if (sResp.isSuccessful) {
                                    val sBody = sResp.body.string()
                                    if (sBody.isNotBlank()) " status=" + sBody.take(300) else ""
                                } else ""
                            }
                        }.getOrDefault("")
                        val msg = buildString {
                            append("Upload failed: HTTP ")
                            append(resp.code)
                            append(" range=")
                            append("bytes ")
                            append(uploaded)
                            append('-')
                            append(end)
                            append('/')
                            append(totalBytes)
                            if (!errBody.isNullOrBlank()) {
                                append(": ")
                                append(errBody.take(300))
                            }
                            if (statusMsg.isNotEmpty()) append(statusMsg)
                        }
                        Log.e(TAG_UPLOAD_SESSION, msg)
                        throw IllegalStateException(msg)
                    }
                }
            }
            completedItem?.let { return it }
        }
        error("Upload did not complete")
    }

    suspend fun moveItem(
        itemId: String,
        token: String,
        newParentId: String,
        newName: String? = null
    ): DriveItemDto {
        val body = MoveItemBody(
            parentReference = ParentReferenceUpdate(id = newParentId),
            name = newName
        )
        return graphApiService.moveItem(
            itemId = itemId,
            token = token,
            body = body
        )
    }

    // Rename item by PATCH with only name field
    suspend fun renameItem(
        itemId: String,
        token: String,
        newName: String
    ): DriveItemDto {
        val body = MoveItemBody(
            parentReference = null,
            name = newName
        )
        return graphApiService.moveItem(
            itemId = itemId,
            token = token,
            body = body
        )
    }

    /**
     * Copy an item to a new parent (optionally with a new name). This method waits for the async
     * operation to complete by polling the monitor URL returned in the Location header.
     * Returns the created DriveItemDto when available, or null if completion cannot be resolved.
     */
    suspend fun copyItem(
        itemId: String,
        token: String,
        newParentId: String,
        newName: String? = null,
        timeoutMillis: Long = 60_000,
        pollIntervalMillis: Long = 1000
    ): DriveItemDto? {
        // Resolve destination driveId (Graph wants driveId + id in parentReference)
        val destParent = graphApiService.getItemParentReference(newParentId, token)
        val driveId = destParent.parentReference?.driveId
        val body = CopyItemBody(
            parentReference = CopyParentReference(driveId = driveId, id = newParentId),
            name = newName
        )

        val resp = graphApiService.copyItem(itemId = itemId, token = token, body = body)
        if (resp.code() != 202) {
            val msg = "Copy not accepted: HTTP ${resp.code()}"
            Log.e("CopyItem", msg)
            error(msg)
        }
        val location = resp.headers()["Location"]
        if (location.isNullOrBlank()) {
            // Some tenants may return 202 without a monitor URL; fallback to returning null
            Log.w("CopyItem", "No monitor Location header; cannot await completion")
            return null
        }

        val adapter = moshi.adapter(CopyMonitorStatus::class.java)
        val start = System.currentTimeMillis()
        var lastStatus: String? = null
        var resourceId: String? = null
        while (System.currentTimeMillis() - start < timeoutMillis) {
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(Request.Builder().url(location).get().build()).execute().use { r ->
                    val bodyStr = try { r.body.string() } catch (_: Exception) { null }
                    if (!bodyStr.isNullOrBlank()) {
                        val st = adapter.fromJson(bodyStr)
                        lastStatus = st?.status
                        resourceId = st?.resourceId ?: st?.resourceLocation?.substringAfterLast('/')
                    }
                }
            }
            if (lastStatus == "completed") break
            if (lastStatus == "failed") {
                error("Copy failed")
            }
            kotlinx.coroutines.delay(pollIntervalMillis)
        }

        // If new resource id known, fetch it; otherwise return null
        return resourceId?.let {
            runCatching { graphApiService.getItemBasic(it, token) }.getOrNull()
        }
    }
}
