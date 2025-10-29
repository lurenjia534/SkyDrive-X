# SkyDrive X

[中文说明 README_zh.md](README_zh.md)

## Overview

SkyDrive X is an Android client for Microsoft 365 / OneDrive users. It delivers a modern Jetpack Compose UI, integrates MSAL authentication, and talks to Microsoft Graph to offer browsing, upload/download, sharing, and media preview features. The app targets Android 12L (API 31) and later, with dedicated handling for Android 14+ foreground data sync and new permission models.

## Highlights

- Sign in with personal or work/school Microsoft accounts (MSAL single-account mode) and refresh tokens silently.
- Browse the OneDrive root and any subfolder with breadcrumb navigation, scrolling cache, and drive/folder search.
- File operations: multi-select, rename, delete, move, copy, create folders, and generate share links.
- Upload flow automatically switches between small and large file paths, runs as a foreground service with progress, supports cancel/resume, and can target the system Downloads directory or a custom tree.
- Downloads leverage `DownloadManager` or a custom saver, with rich notifications and persistent history.
- Preview photos (Coil 3), audio (Media3 + FFmpeg extension), and video (Media3 Compose UI with orientation and playback speed controls).
- Preferences and theme: DataStore for dark mode / download location, Room for transfer history to survive restarts, and a settings page for account information, quota, and notification status checks.

## Tech Stack

- **Language & base**: Kotlin 2.2.20, Android Gradle Plugin 8.13.0, compile/target SDK 36, min SDK 31.
- **UI layer**: Jetpack Compose Material 3, Navigation Compose, Kotlin Coroutines, Coil 3, Compose Placeholder.
- **Architecture & DI**: MVVM + Repository pattern with Hilt (including `hilt-navigation-compose`) and KSP.
- **Networking & data**: Retrofit 3 + Moshi, OkHttp 5, Microsoft Graph REST API, MSAL 7 single-account app.
- **Local storage**: Jetpack DataStore Preferences, Room 2.8 (custom converters, foreground service persistence).
- **Media**: AndroidX Media3 (ExoPlayer, Compose UI) with Jellyfin FFmpeg decoder extension.
- **Utilities**: AndroidX Activity Compose, Lifecycle Runtime/Service, NotificationCompat, DownloadManager.

## Architecture

- **UI layer** (`ui/...`): Compose screens/components plus Navigation. `MainActivity` injects `MainViewModel` to handle authentication, theme, and global state. `FilesScreen` manages the file list, action menus, and download notifications. Preview routes have dedicated ViewModels to control Media3 players.
- **ViewModel layer** (`ui/viewmodel/...`): Hilt-injected ViewModels expose `StateFlow`. `FilesViewModel` keeps a path stack, cache, and search state; `FolderPickerViewModel` reuses folder traversal logic; preview ViewModels manage audio/video playback.
- **Data layer** (`data/...`): `FilesRepository` wraps Graph API calls, enriches results with thumbnails, batches deletions, and implements large file uploads. `UserRepository` fetches profile/quota; preference repositories persist settings via DataStore; Room keeps transfer history.
- **Dependency injection** (`di/...`): `NetworkModule` builds Retrofit/OkHttp/Moshi, `DatabaseModule` provides Room and initializes `TransferTracker`.
- **Background service** (`ui/service/TransferService`): Foreground service for large uploads with notification channel, persistent progress, and cancel support through `TransferTracker`.

## Project Layout

```text
.
├── app
│   ├── build.gradle.kts                 # Module configuration and dependencies
│   └── src/main/java/com/lurenjia534/skydrivex
│       ├── auth                         # MSAL login & token handling
│       ├── data                         # Graph API, Room, DataStore
│       ├── di                           # Hilt modules
│       ├── ui                           # Compose activities, screens, components
│       └── SkyDriveXApp.kt              # @HiltAndroidApp entry point
├── gradle/libs.versions.toml            # Centralized dependency versions
├── MSAL授权指南.md                      # Azure AD registration & signing steps (Chinese)
├── README.md                            # English README (this file)
└── README_zh.md                         # Chinese README
```

## Getting Started

1. **Clone the repo**
   ```bash
   git clone https://github.com/lurenjia534/SkyDriveX.git
   ```
2. **Configure your Microsoft Entra app**
   - Follow the English guide [MSAL_Config.md](MSAL_Config.md) (a Chinese version is available as [MSAL授权指南.md](MSAL授权指南.md)) to register the Azure AD application, export the signing hash, and retrieve the `client_id`.
   - When SkyDrive X launches for the first time it opens an OOBE wizard; enter the `client_id` there. You can revisit the wizard from the Settings page (“Modify login configuration”) to switch apps later.
3. **Open the project**
   - Use Android Studio Koala (or newer) with Gradle 8.13.
   - Install Android 14 (API 34) SDK or newer; enable Android 15 (API 35/36) preview to build.
4. **Build & run**
   - Debug build: `./gradlew assembleDebug`
   - Install on device: `adb install app/build/outputs/apk/debug/app-debug.apk`
   - Unit tests: `./gradlew test` (requires a full local Android environment)

> **Note**: For enterprise tenants ensure the account has `Files.ReadWrite` and `User.Read` permissions and that admin consent is granted in the Azure portal.

## Development & Debug Tips

- **Token lifecycle**: `MainViewModel` refreshes tokens when the app resumes; monitor Logcat tags `MainViewModel` / `AuthManager` during troubleshooting.
- **API debugging**: `FilesRepository` uses Retrofit and an OkHttp interceptor—tools like Stetho or Charles can inspect outbound Graph requests.
- **Notifications & transfers**: `TransferTracker` persists progress states in Room. The database lives at `data/data/<package>/databases/transfer_db`, viewable via Android Studio’s Database Inspector.
- **Compose previews**: Components such as `SkyDriveXAppContent` and `FilesRow` include `@Preview` annotations for quick iteration.
- **Media3 decoding**: The Jellyfin FFmpeg extension is enabled by default. If specific formats fail, check logs from `VideoPlayerViewModel` / `AudioPlayerViewModel` to see which decoder was selected.

## FAQ

- **Blank screen or crash after sign-in**: Verify that `auth_config.json` and the `BrowserTabActivity` signature hash match the signing certificate registered in Azure.
- **Missing thumbnails**: Some enterprise tenants omit `thumbnails`. `FilesRepository` falls back to additional Graph calls; ensure the app has the necessary Graph permissions.
- **Large upload failures**: Check connectivity and device storage. Foreground notifications report error messages; inspect `TransferService` logs for details.
- **Notifications not appearing**: Android 13+ requires explicit notification permission. Enable it in system settings or use the in-app settings shortcut.

## Configuration & Usage

For step-by-step Azure registration instructions (with screenshots), see [MSAL_Config.md](MSAL_Config.md). Complete the guide, then launch the app and follow the OOBE wizard to finish sign-in.

## Roadmap / TODO

- [ ] Expand offline caching and batch download management.
- [ ] Support more Graph scenarios (Shared libraries, Recent files, Recycle bin).
- [ ] Add UI tests and finer-grained unit coverage.
- [ ] Improve localization and accessibility.

## Contributing

Contributions via issues and pull requests are welcome. Please ensure:
- `./gradlew lint` and relevant unit tests pass.
- Code follows the existing Kotlin style and package layout.
- Significant logic changes include documentation or concise comments where necessary.

## License

This project is licensed under the `GNU Affero General Public License v3.0 (AGPL-3.0)`. See the `LICENSE` file for details. When distributing, modifying, or offering the software as a network service, you must comply with the license terms and keep the same license and copyright notices in derivative works.
