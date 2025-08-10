需要我再补一个面包屑（Breadcrumb）展示当前路径，或添加下拉刷新/错误重试按钮吗？

下载功能说明：
- 数据层：新增 Graph 接口获取 @microsoft.graph.downloadUrl（仅文件）。
- 仓库：提供 getDownloadUrl(itemId, token) 返回直链。
- ViewModel：暴露 suspend fun getDownloadUrl(...) 供 UI 调用。
- UI：在文件项“更多”中加入“下载”，使用 Android DownloadManager 将文件保存到“下载”目录，并在下载开始/失败时通过 Snackbar 提示。

注意与假设：
- DownloadManager 目标目录使用 setDestinationInExternalPublicDir，API 29+ 会提示弃用但仍可用；更完善的实现可改用 MediaStore。
- 直链为短期有效的匿名预签名 URL，不需要 Authorization，请勿长期缓存或分享。
- 分片下载（Range）未实现；如需断点续传可在拿到直链后自行构建带 Range 的请求。

Added selectable download location in Settings and adapted downloads accordingly
.

What I implemented
- New preferences:
    - `DownloadPreferenceRepository` (DataStore) to persist download mode and cust
      om folder:
        - Modes: system Downloads or a custom SAF folder.
- Settings UI:
    - In SettingsActivity, added “下载位置” with:
        - “系统默认” button to use the system Downloads directory.
        - “选择文件夹” to pick a folder via SAF (persistable permission).
    - Shows current mode summary.
- Download behavior:
    - FilesScreen now reads the download preference:
        - System Downloads: uses Android DownloadManager (same as before).
        - Custom folder: streams the file directly into the chosen folder via SAF us
          ing ContentResolver and HttpURLConnection.

Files changed/added
- Added: `app/src/main/java/com/lurenjia534/skydrivex/data/local/DownloadPrefere
nceRepository.kt`
- Updated: `app/src/main/java/com/lurenjia534/skydrivex/viewmodel/MainViewModel.
kt`
    - Injects and exposes `downloadPreference`.
    - Adds `setDownloadToSystem()` and `setDownloadToCustom(...)`.
- Updated: `app/src/main/java/com/lurenjia534/skydrivex/ui/activity/SettingsActi
vity.kt`
    - New “下载位置” setting with OpenDocumentTree picker.
- Updated: `app/src/main/java/com/lurenjia534/skydrivex/ui/screens/FilesScreen.k
t`
    - Reads `downloadPreference` (via `MainViewModel`).
    - Branches to DownloadManager or custom SAF write.
    - Adds helper `saveToTree(...)` to write into the selected folder.

Notes
- Custom folder downloads use a direct HTTP stream to the selected directory (pr
  e-authenticated `downloadUrl` needs no headers).
- DownloadManager cannot easily target arbitrary SAF folders; hence custom mode
  uses streaming to OutputStream.
- No build/tests were executed per your environment instructions.

Want me to:
- Show the selected custom folder path under “下载位置”?
- Add simple progress notification for custom downloads?
- Allow renaming before saving when using the custom folder?