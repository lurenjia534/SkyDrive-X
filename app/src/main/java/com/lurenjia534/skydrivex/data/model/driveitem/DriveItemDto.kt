package com.lurenjia534.skydrivex.data.model.driveitem

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveItemDto(
    val id: String?,
    val name: String?,
    val size: Long?,
    val folder: FolderFacet?,
    val file: FileFacet?,
    // 图片分面（位图宽高）
    val image: ImageFacet? = null,
    // 照片/EXIF 分面（相机/曝光/拍摄时间等）
    val photo: PhotoFacet? = null,
    // 视频分面（包含分辨率、帧率、码率、时长、编码 fourCC 以及部分音频相关字段）
    val video: VideoFacet? = null,
    @param:Json(name = "parentReference") val parentReference: ItemReference?,
    val searchResult: SearchResultFacet? = null,
    val remoteItem: RemoteItemFacet? = null,
    // 可选的缩略图集合（当使用 $expand=thumbnails 或后续补充时）
    val thumbnails: List<com.lurenjia534.skydrivex.data.model.thumbnail.ThumbnailSet>? = null,
    // 详情对话框需要的额外字段（可选，未选择时为 null）
    val webUrl: String? = null,
    val createdDateTime: String? = null,
    val lastModifiedDateTime: String? = null
)

@JsonClass(generateAdapter = true)
data class FolderFacet(
    val childCount: Int?
)

@JsonClass(generateAdapter = true)
data class FileFacet(
    val mimeType: String?
)

@JsonClass(generateAdapter = true)
data class ImageFacet(
    val width: Int? = null,
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class PhotoFacet(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val fNumber: Double? = null,
    val focalLength: Double? = null,
    val iso: Int? = null,
    val exposureNumerator: Double? = null,
    val exposureDenominator: Double? = null,
    val orientation: Int? = null,
    val takenDateTime: String? = null
)

@JsonClass(generateAdapter = true)
data class VideoFacet(
    val bitrate: Int? = null,                 // bps
    val duration: Long? = null,               // 毫秒（Graph 文档）
    val height: Int? = null,                  // 像素
    val width: Int? = null,                   // 像素
    val frameRate: Double? = null,            // fps
    val fourCC: String? = null,               // 编码标识
    // 下列字段在部分返回中存在（音频相关）
    val audioBitsPerSample: Int? = null,
    val audioChannels: Int? = null,
    val audioFormat: String? = null,
    val audioSamplesPerSecond: Int? = null
)

@JsonClass(generateAdapter = true)
data class ItemReference(
    val driveId: String?,
    val id: String?,
    val path: String?
)

@JsonClass(generateAdapter = true)
data class SearchResultFacet(
    val onClickTelemetryUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteItemFacet(
    val id: String?,
    @param:Json(name = "parentReference") val parentReference: ItemReference?
)
