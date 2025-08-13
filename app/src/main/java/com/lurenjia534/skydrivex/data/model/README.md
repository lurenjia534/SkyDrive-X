# Data Model Packages

本目录用于定义与 Microsoft Graph 接口相对应的数据模型（DTO）。模型按照资源维度进行分包，确保引用关系清晰、维护成本低、扩展方便。

## 目录结构

```
com/lurenjia534/skydrivex/data/model/
├─ user/             # /me 相关：用户信息
│  └─ UserDto.kt
├─ drive/            # /me/drive 相关：驱动器与配额
│  └─ DriveDto.kt    # 内含 Quota、StoragePlanInformation
└─ driveitem/        # /me/drive 下的条目：文件/文件夹
   ├─ DriveItemDto.kt           # 内含 FolderFacet、FileFacet、ItemReference
   ├─ DriveItemsResponse.kt     # 列表/分页响应壳（value: List<DriveItemDto>）
   └─ DriveItemDownloadUrlDto.kt
```

序列化库：Moshi（`@JsonClass(generateAdapter = true)`）。对于字段名不一致的情况使用 `@Json(name = "...")` 指定映射（示例：`@microsoft.graph.downloadUrl`）。

## 与 GraphApiService 的映射

- `GET me` → `user.UserDto`
- `GET me/drive?$select=id,driveType,quota` → `drive.DriveDto`
- `GET me/drive/root/children` → `driveitem.DriveItemsResponse`
- `GET me/drive/items/{itemId}/children` → `driveitem.DriveItemsResponse`
- `GET me/drive/items/{itemId}?$select=@microsoft.graph.downloadUrl` → `driveitem.DriveItemDownloadUrlDto`

对应实现参见：`data/remote/GraphApiService.kt`。

## 约定与规范

- 命名
  - 统一以 `Dto` 后缀命名网络层数据模型。
  - 仅在必要时使用 `@Json(name = "...")`（如特殊字段名）。
- 包职责
  - `user/` 仅放置用户相关模型。
  - `drive/` 仅放置驱动器与配额相关模型。
  - `driveitem/` 放置条目（文件/文件夹）及其列表/下载链接相关模型。
- 可空性
  - 遵循 Graph 返回的可空性：不确定或可缺省字段标注为可空类型（如 `String?`、`Long?`）。
- 壳模型
  - 仅列表接口使用 `DriveItemsResponse`（`value: List<DriveItemDto>`）。
  - 如后续接入分页（`@odata.nextLink`），可在 `DriveItemsResponse` 中扩展对应字段。
- 依赖方向
  - 上层（Repository / ViewModel）只依赖必要的子包类型，不跨包混用。

## 扩展指引（新增接口）

1. 在对应子包新增或复用 DTO：
   - 用户 → `user/`
   - 驱动器 → `drive/`
   - 条目/文件/文件夹 → `driveitem/`
2. 在 `GraphApiService` 增加方法签名，返回相应 DTO。
3. 在 Repository 中封装业务方法（必要时解包 `Response` 或壳模型）。
4. 在调用侧（ViewModel/UI）导入新 DTO 并处理 UI 状态。

## 现有引用示例

- `UserRepository` 使用 `user.UserDto` 与 `drive.DriveDto`。
- `FilesRepository`、`FilesViewModel`、`FilesUiState` 使用 `driveitem.DriveItemDto` 及相关响应类型。
- `GraphApiService` 统一从新子包导入对应模型类型。

## 备注

- 大文件下载当前通过获取预授权 `downloadUrl` 实现，模型为 `DriveItemDownloadUrlDto`。
- 若后续引入更多 facet（例如 `photo`、`audio`），建议仍放在 `driveitem/` 内并按职责内聚。
