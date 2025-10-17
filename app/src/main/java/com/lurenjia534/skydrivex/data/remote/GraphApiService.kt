package com.lurenjia534.skydrivex.data.remote

import com.lurenjia534.skydrivex.data.model.drive.DriveDto
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemsResponse
import com.lurenjia534.skydrivex.data.model.user.UserDto
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDownloadUrlDto
import com.lurenjia534.skydrivex.data.model.permission.CreateLinkRequest
import com.lurenjia534.skydrivex.data.model.permission.CreateLinkResponse
import com.lurenjia534.skydrivex.data.model.driveitem.CreateFolderBody
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto
import okhttp3.RequestBody
import retrofit2.Response
import com.lurenjia534.skydrivex.data.model.upload.CreateUploadSessionRequest
import com.lurenjia534.skydrivex.data.model.upload.UploadSessionDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PATCH
import retrofit2.http.Url
import com.lurenjia534.skydrivex.data.model.thumbnail.ThumbnailSetsResponse
import com.lurenjia534.skydrivex.data.model.batch.BatchRequest
import com.lurenjia534.skydrivex.data.model.batch.BatchResponse
import com.lurenjia534.skydrivex.data.model.driveitem.DriveDeltaResponse

interface GraphApiService {
    @POST("\$batch")
    suspend fun batch(
        @Header("Authorization") token: String,
        @Body body: BatchRequest
    ): BatchResponse

    @DELETE("me/drive/items/{itemId}")
    suspend fun deleteFile(
        @Path("itemId") id: String,
        @Header("Authorization") token: String
    ): Response<Unit>
    @GET("me")
    suspend fun getMe(@Header("Authorization") token: String): UserDto

    @GET("me/drive?\$select=id,driveType,quota")
    suspend fun getDrive(@Header("Authorization") token: String): DriveDto

    @GET("me/drive/root/children")
    suspend fun getRootChildren(
        @Header("Authorization") token: String,
        @Query("\$top") top: Int? = null,
        @Query("\$orderby") orderBy: String? = null
    ): DriveItemsResponse

    @GET("me/drive/items/{itemId}/children")
    suspend fun getChildren(
        @Path("itemId") id: String,
        @Header("Authorization") token: String,
        @Query("\$top") top: Int? = null,
        @Query("\$orderby") orderBy: String? = null
    ): DriveItemsResponse

    // Try expand thumbnails when listing (may not work for Business tenants)
    @GET("me/drive/root/children")
    suspend fun getRootChildrenExpanded(
        @Header("Authorization") token: String,
        @Query("\$top") top: Int? = null,
        @Query("\$orderby") orderBy: String? = null,
        @Query("\$expand") expand: String = "thumbnails",
        @Query("\$select") select: String? = "id,name,size,parentReference,folder,file,thumbnails"
    ): DriveItemsResponse

    @GET("me/drive/items/{itemId}/children")
    suspend fun getChildrenExpanded(
        @Path("itemId") id: String,
        @Header("Authorization") token: String,
        @Query("\$top") top: Int? = null,
        @Query("\$orderby") orderBy: String? = null,
        @Query("\$expand") expand: String = "thumbnails",
        @Query("\$select") select: String? = "id,name,size,parentReference,folder,file,thumbnails"
    ): DriveItemsResponse

    // Fetch pre-authenticated download URL for a file
    @GET("me/drive/items/{itemId}?\$select=@microsoft.graph.downloadUrl")
    suspend fun getDownloadUrl(
        @Path("itemId") id: String,
        @Header("Authorization") token: String
    ): DriveItemDownloadUrlDto

    // Create a sharing link for an item
    @POST("me/drive/items/{itemId}/createLink")
    suspend fun createLink(
        @Path("itemId") id: String,
        @Header("Authorization") token: String,
        @Body body: CreateLinkRequest
    ): CreateLinkResponse

    // Upload a new small file (< 250MB) under a parent folder by ID
    // PUT /me/drive/items/{parent-id}:/{filename}:/content
    @PUT("me/drive/items/{parentId}:/{filename}:/content")
    suspend fun uploadSmallFileToParent(
        @Path("parentId") parentId: String,
        @Path("filename") fileName: String,
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String,
        @Body body: RequestBody
    ): DriveItemDto

    // Upload a new small file into root by filename
    // PUT /me/drive/root:/{filename}:/content
    @PUT("me/drive/root:/{filename}:/content")
    suspend fun uploadSmallFileToRoot(
        @Path("filename") fileName: String,
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String,
        @Body body: RequestBody
    ): DriveItemDto

    // Replace the contents of an existing file by item id
    // PUT /me/drive/items/{item-id}/content
    @PUT("me/drive/items/{itemId}/content")
    suspend fun replaceSmallFileContent(
        @Path("itemId") itemId: String,
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String,
        @Body body: RequestBody
    ): DriveItemDto

    // Create folder under a specific parent by ID
    @POST("me/drive/items/{parentId}/children")
    suspend fun createFolderUnderParent(
        @Path("parentId") parentId: String,
        @Header("Authorization") token: String,
        @Body body: CreateFolderBody
    ): DriveItemDto

    // Create folder under root
    @POST("me/drive/root/children")
    suspend fun createFolderUnderRoot(
        @Header("Authorization") token: String,
        @Body body: CreateFolderBody
    ): DriveItemDto

    // Create upload session for new file under parent
    @POST("me/drive/items/{parentId}:/{fileName}:/createUploadSession")
    suspend fun createUploadSessionForNew(
        @Path("parentId") parentId: String,
        @Path(value = "fileName", encoded = true) fileName: String,
        @Header("Authorization") token: String,
        @Body body: CreateUploadSessionRequest
    ): UploadSessionDto

    // Create upload session for new file under root
    @POST("me/drive/root:/{fileName}:/createUploadSession")
    suspend fun createUploadSessionForRoot(
        @Path(value = "fileName", encoded = true) fileName: String,
        @Header("Authorization") token: String,
        @Body body: CreateUploadSessionRequest
    ): UploadSessionDto

    // Create upload session for existing item by id
    @POST("me/drive/items/{itemId}/createUploadSession")
    suspend fun createUploadSessionForExisting(
        @Path("itemId") itemId: String,
        @Header("Authorization") token: String,
        @Body body: CreateUploadSessionRequest
    ): UploadSessionDto

    // Move item to a new parent (and/or rename) via PATCH update
    @PATCH("me/drive/items/{itemId}")
    suspend fun moveItem(
        @Path("itemId") itemId: String,
        @Header("Authorization") token: String,
        @Body body: com.lurenjia534.skydrivex.data.model.driveitem.MoveItemBody
    ): DriveItemDto

    // Fetch root item (only id)
    @GET("me/drive/root?\$select=id")
    suspend fun getRootItem(
        @Header("Authorization") token: String
    ): DriveItemDto

    // Get an item with only parentReference (to read its driveId)
    @GET("me/drive/items/{itemId}?\$select=parentReference")
    suspend fun getItemParentReference(
        @Path("itemId") itemId: String,
        @Header("Authorization") token: String
    ): DriveItemDto

    // Fetch basic fields of an item by id
    @GET("me/drive/items/{itemId}?\$select=id,name,size,parentReference,folder,file")
    suspend fun getItemBasic(
        @Path("itemId") itemId: String,
        @Header("Authorization") token: String
    ): DriveItemDto

    // Detailed fields for properties dialog
    @GET("me/drive/items/{itemId}?\$select=id,name,size,webUrl,parentReference,createdDateTime,lastModifiedDateTime,folder,file,image,photo,video")
    suspend fun getItemDetails(
        @Path("itemId") itemId: String,
        @Header("Authorization") token: String
    ): DriveItemDto

    // Copy item asynchronously
    @POST("me/drive/items/{itemId}/copy")
    suspend fun copyItem(
        @Path("itemId") itemId: String,
        @Header("Authorization") token: String,
        @Body body: com.lurenjia534.skydrivex.data.model.driveitem.CopyItemBody
    ): Response<Unit>

    // Search within current user's drive root hierarchy
    // GET /me/drive/root/search(q='{search-text}')
    @GET("me/drive/root/search(q='{q}')")
    suspend fun searchInRoot(
        @Path("q") q: String,
        @Header("Authorization") token: String,
        @Query("\$top") top: Int? = null,
        @Query("\$orderby") orderBy: String? = null,
        @Query("\$select") select: String? = null,
        @Query("\$expand") expand: String? = null,
        @Query("\$skipToken") skipToken: String? = null
    ): DriveItemsResponse

    // Search across the drive (includes items shared with the user)
    // GET /me/drive/search(q='{search-text}')
    @GET("me/drive/search(q='{q}')")
    suspend fun searchInDrive(
        @Path("q") q: String,
        @Header("Authorization") token: String,
        @Query("\$top") top: Int? = null,
        @Query("\$orderby") orderBy: String? = null,
        @Query("\$select") select: String? = null,
        @Query("\$expand") expand: String? = null,
        @Query("\$skipToken") skipToken: String? = null
    ): DriveItemsResponse

    // Search within a specific folder hierarchy
    // GET /me/drive/items/{itemId}/search(q='{search-text}')
    @GET("me/drive/items/{itemId}/search(q='{q}')")
    suspend fun searchInFolder(
        @Path("itemId") itemId: String,
        @Path("q") q: String,
        @Header("Authorization") token: String,
        @Query("\$top") top: Int? = null,
        @Query("\$orderby") orderBy: String? = null,
        @Query("\$select") select: String? = null,
        @Query("\$expand") expand: String? = null,
        @Query("\$skipToken") skipToken: String? = null
    ): DriveItemsResponse
    // Additional endpoint to fetch thumbnails of a single item (fallback path)
    @GET("me/drive/items/{itemId}/thumbnails")
    suspend fun getThumbnails(
        @Path("itemId") itemId: String,
        @Header("Authorization") token: String,
        @Query("\$select") select: String? = null
    ): ThumbnailSetsResponse

    // Delta query for incremental sync of drive items
    @GET("me/drive/root/delta")
    suspend fun getRootDelta(
        @Header("Authorization") token: String,
        @Query("token") latestToken: String? = null
    ): DriveDeltaResponse

    @GET
    suspend fun followDeltaLink(
        @Url nextLink: String,
        @Header("Authorization") token: String
    ): DriveDeltaResponse
}
