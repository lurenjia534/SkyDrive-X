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
        bytesProvider: suspend (offset: Long, size: Int) -> ByteArray
    ): DriveItemDto {
        val req = CreateUploadSessionRequest(
            item = DriveItemUploadableProperties(
                conflictBehavior = "rename",
                name = fileName,
                fileSize = totalBytes
            )
        )
        val session = if (parentId == null || parentId == "root") {
            graphApiService.createUploadSessionForRoot(
                fileName = fileName,
                token = token,
                body = req
            )
        } else {
            graphApiService.createUploadSessionForNew(
                parentId = parentId,
                fileName = fileName,
                token = token,
                body = req
            )
        }

        val uploadUrl = session.uploadUrl
        val chunkSize = 5 * 1024 * 1024 // 5 MiB (multiple of 320 KiB)
        var uploaded = 0L
        while (uploaded < totalBytes) {
            val remaining = (totalBytes - uploaded).toInt()
            val size = if (remaining < chunkSize) remaining else chunkSize
            val chunk = bytesProvider(uploaded, size)
            val end = uploaded + size - 1
            val request = Request.Builder()
                .url(uploadUrl)
                .put(chunk.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
                .header("Content-Length", size.toString())
                .header("Content-Range", "bytes ${uploaded}-${end}/${totalBytes}")
                .build()
            okHttpClient.newCall(request).execute().use { resp ->
                if (resp.code == 202) {
                    uploaded += size
                } else if (resp.code == 201 || resp.code == 200) {
                    val adapter = moshi.adapter(DriveItemDto::class.java)
                    val bodyStr = resp.body?.string()
                    val item = adapter.fromJson(bodyStr ?: "")
                    if (item != null) return item else error("Empty item response")
                } else {
                    error("Upload failed: HTTP ${resp.code}")
                }
            }
        }
        error("Upload did not complete")
    }
}
