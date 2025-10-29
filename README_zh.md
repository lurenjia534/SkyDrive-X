# SkyDrive X - 适用于 Android 的 Material 3 OneDrive 客户端

<p align="center">
  <a href="README.md">English</a> |
  <a href="README_zh.md">中文</a>
</p>

<p align="center">
  <a href="https://github.com/lurenjia534/SkyDrive-X/releases">
    <img src="https://img.shields.io/badge/version-1.0.0-blue" alt="版本" />
  </a>
  <img src="https://img.shields.io/badge/platform-Android-brightgreen" alt="平台" />
  <img src="https://img.shields.io/badge/language-Kotlin-orange" alt="语言" />
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/license-AGPL--3.0-success" alt="许可证" />
  </a>
  <a href="https://github.com/lurenjia534/SkyDrive-X/stargazers">
    <img src="https://img.shields.io/github/stars/lurenjia534/SkyDrive-X?style=flat-square" alt="GitHub stars" />
  </a>
  <a href="https://github.com/lurenjia534/SkyDrive-X/issues">
    <img src="https://img.shields.io/github/issues-raw/lurenjia534/SkyDrive-X" alt="开放 issue" />
  </a>
  <img src="https://img.shields.io/github/last-commit/lurenjia534/SkyDrive-X" alt="最近提交" />
</p>

## 项目简介

SkyDrive X 是一个面向 Microsoft 365 / OneDrive 用户的 Android 客户端，基于 Jetpack Compose Material 3 打造现代化界面，整合 MSAL 身份认证与 Microsoft Graph API，实现文件浏览、上传下载、分享与多媒体预览等完整体验。应用面向 Android 12L（API 31）及以上设备，并针对 Android 14+ 的前台数据同步、权限模型进行了适配。

## 功能特性

- 支持 Microsoft 个人与企业账号登录（MSAL 单账号模式），自动刷新访问令牌。
- 浏览 OneDrive 根目录和任意子目录，内建面包屑导航、滚动缓存与搜索（全盘 / 当前文件夹）。
- 常见文件操作：批量选择、重命名、删除、移动、复制、创建文件夹、生成分享链接。
- 上传：自动区分小文件与大文件，前台服务展示进度，可取消、断点续传。支持系统下载目录或自定义存储树。
- 下载：利用 `DownloadManager` 或自定义保存逻辑，配合通知栏展示实时进度并持久化记录。
- 预览：图片（Coil 3）、音频（Media3 + FFmpeg 扩展）、视频（Media3 Compose UI，支持横竖屏切换与倍速）。
- 主题与偏好：DataStore 持久化深色模式、下载目录等用户偏好；Room 记录传输历史，重启后仍可追踪状态。
- 设置信息页涵盖账号信息、配额、通知授权检测等辅助功能。

## 配置速览

1. **注册 Azure 应用**：参阅 [MSAL授权指南.md](MSAL授权指南.md) 创建应用，选择“多租户 + 个人账号”，记录生成的 `client_id`。
2. **配置 Android 重定向 URI**：在“身份验证 → 添加平台 → Android”填写包名 `com.lurenjia534.skydrivex` 与签名哈希 `rZDXYaNZmghPivXu+4dDWNfayVo=`。
3. **授权权限**：在“API 权限”中添加委托权限 `Files.ReadWrite`（保留默认的 `User.Read`），如为企业租户请授予管理员同意。
4. **运行应用**：安装 SkyDrive X，首次启动在 OOBE 向导输入 `client_id` 并登录。需要切换配置时，可在设置页点击“修改登陆配置”重新进入向导。

## 技术栈

- **语言与基础**：Kotlin 2.2.20、Android Gradle Plugin 8.13.0、Compile/Target SDK 36、Min SDK 31。
- **界面层**：Jetpack Compose Material3、Compose Navigation、Kotlinx Coroutines、Coil 3、Compose Placeholder。
- **架构与依赖注入**：MVVM + Repository；Hilt（含 `hilt-navigation-compose`）、KSP 自动生成。
- **网络与数据**：Retrofit 3 + Moshi、OkHttp 5、Microsoft Graph REST API、MSAL 7 单账户应用。
- **本地存储**：Jetpack DataStore Preferences、Room 2.8（自定义转换器、前台服务数据持久化）。
- **多媒体**：AndroidX Media3（ExoPlayer、UI Compose）、Jellyfin FFmpeg 扩展解码器。
- **工具与配套**：AndroidX Activity Compose、Lifecycle Runtime/Service、NotificationCompat、DownloadManager。

## 架构说明

- **UI 层** (`ui/...`)：Compose Screen / Component + Navigation。`MainActivity` 注入 `MainViewModel` 处理认证、主题与全局状态；`FilesScreen` 负责文件列表、操作菜单与下载通知；预览页由专用 ViewModel 管理 Media3 播放器。
- **ViewModel 层** (`ui/viewmodel/...`)：基于 Hilt 注入，使用 `StateFlow` 发布 UI 状态。`FilesViewModel` 维护目录栈、缓存与搜索；`FolderPickerViewModel` 复用列表逻辑；预览 VM 控制音视频。
- **数据层** (`data/...`)：`FilesRepository` 封装 Graph API 调用，自动补全缩略图、大文件分片上传、批量删除等；`UserRepository` 获取用户与配额；`DownloadPreferenceRepository` / `ThemePreferenceRepository` 使用 DataStore 持久化偏好；Room 持久化传输记录。
- **依赖注入** (`di/...`)：`NetworkModule` 构建 Retrofit、OkHttp、Moshi；`DatabaseModule` 提供 Room、`TransferTracker` 初始化；Hilt 统一管理作用域。
- **后台服务** (`ui/service/TransferService`)：前台服务负责大文件上传，结合通知渠道、`TransferTracker` 持久化状态并支持取消操作。

## 目录结构

```text
.
├── app
│   ├── build.gradle.kts                 # 模块配置、依赖声明
│   └── src/main/java/com/lurenjia534/skydrivex
│       ├── auth                         # MSAL 登录与令牌管理
│       ├── data                         # 数据层（Graph API、Room、DataStore）
│       ├── di                           # Hilt Module
│       ├── ui                           # Compose Activity / Screen / Component
│       └── SkyDriveXApp.kt              # @HiltAndroidApp 启动入口
├── gradle/libs.versions.toml            # 版本统一管理
├── MSAL授权指南.md                      # 详细的 Azure AD 注册与签名配置步骤
└── README.md
```

## 快速开始

1. **获取代码**
   ```bash
   git clone https://github.com/lurenjia534/SkyDriveX.git
   ```
2. **配置 Microsoft Entra 应用**
   - 请阅读仓库内的 [MSAL授权指南.md](MSAL授权指南.md)，按照步骤在 Azure Portal 注册应用、导出签名哈希并获取 `client_id`。
   - 首次启动 SkyDrive X 时会进入 OOBE 向导，输入 `client_id` 后即可完成绑定；若需更换配置，可在应用设置页选择“修改登陆配置”。
3. **打开工程**
   - 使用 Android Studio Koala (或更高) / Gradle 8.13。
   - 确保安装 Android 14 (API 34) 及以上 SDK，并启用 Android 15 (API 35/36) 预览以便编译。
4. **构建 & 运行**
   - 编译调试版：`./gradlew assembleDebug`
   - 安装到设备：`adb install app/build/outputs/apk/debug/app-debug.apk`
   - 如需单元测试：`./gradlew test`（需本地具备完整 Android 环境）

> **提示**：若是企业租户，请确认账号具备 `Files.ReadWrite`、`User.Read` 权限，并在 Azure Portal 中授予管理员同意。

## 常见问题

- **登录后空白或闪退**：请确认安装包的包名与签名哈希与 Azure 中登记的信息一致，如需更换 `client_id` 可在设置页重新进入 OOBE 向导。
- **缩略图缺失**：企业租户可能不返回 `thumbnails` 字段，`FilesRepository` 已实现回退逻辑，但需要 Graph API 正确授权。
- **上传大文件失败**：检查网络状况及可用空间；服务会在失败时通过通知提示错误原因，可在 `TransferService` 日志定位。
- **通知无法显示**：Android 13+ 需要显式授予通知权限，可在应用设置中开启，或在设置页点击检测按钮跳转授权。

## 如何配置并使用 App

详细的 Azure 应用注册与客户端配置步骤，请参阅 [MSAL授权指南.md](MSAL授权指南.md)。文档中包含所有界面示意与常见问题解答，按步骤完成后首次启动应用即可通过 OOBE 向导完成登录。

## 路线图 / TODO

- [ ] 扩展离线缓存与批量下载任务管理。
- [ ] 支持更多 Graph API 场景（共享库、最近使用、回收站）。
- [ ] 引入 UI 测试与更细粒度的单元测试。
- [ ] 多语言本地化与可访问性增强。

## 贡献指南

欢迎通过 Issue 与 Pull Request 参与共建。提交 PR 时请确保：
- 通过 `./gradlew lint` 与基础单元测试。
- 遵循现有的 Kotlin 代码风格与包结构。
- 对新增的核心逻辑补充说明或必要的注释。

## 许可证

SkyDrive X 以 **GNU Affero General Public License v3.0** 发布。完整条款请参阅 [LICENSE](LICENSE)。如果你修改后分发或以服务形式提供本项目，必须将源代码以相同的 AGPL-3.0 条款公开，并保留原始的版权与许可声明。
