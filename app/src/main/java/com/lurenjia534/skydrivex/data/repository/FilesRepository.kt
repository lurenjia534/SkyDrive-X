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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilesRepository @Inject constructor(
    private val graphApiService: GraphApiService,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {
    suspend fun getRootChildren(token: String): List<DriveItemDto> =
        graphApiService.getRootChildren(token).value

    suspend fun getChildren(itemId: String, token: String): List<DriveItemDto> =
        graphApiService.getChildren(itemId, token).value

    suspend fun deleteFile(itemId: String, token: String) {
        graphApiService.deleteFile(id = itemId, token = token)
    }

    suspend fun getDownloadUrl(itemId: String, token: String): String? =
        graphApiService.getDownloadUrl(id = itemId, token = token).downloadUrl

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
                        val bodyStr = resp.body?.string()
                        val item = adapter.fromJson(bodyStr ?: "")
                        if (item != null) {
                            completedItem = item
                        } else {
                            throw IllegalStateException("Empty item response")
                        }
                    } else {
                        val errBody = try { resp.body?.string() } catch (_: Exception) { null }
                        // Query session status (IO thread) for nextExpectedRanges
                        val statusMsg = runCatching {
                            val statusReq = Request.Builder().url(uploadUrl).get().build()
                            okHttpClient.newCall(statusReq).execute().use { sResp ->
                                if (sResp.isSuccessful) {
                                    val sBody = sResp.body?.string()
                                    if (!sBody.isNullOrBlank()) {
                                        " status=" + sBody.take(300)
                                    } else ""
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
                        Log.e("UploadSession", msg)
                        throw IllegalStateException(msg)
                    }
                }
            }
            if (completedItem != null) return completedItem!!
        }
        error("Upload did not complete")
    }
}
