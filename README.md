# SkyDrive X - Material 3 OneDrive client for Android

<p align="center">
  <a href="README.md">English</a> |
  <a href="README_zh.md">中文</a>
</p>

<p align="center">
  <a href="https://github.com/lurenjia534/SkyDrive-X/releases">
    <img src="https://img.shields.io/badge/version-1.0.0-blue" alt="version" />
  </a>
  <img src="https://img.shields.io/badge/platform-Android-brightgreen" alt="platform" />
  <img src="https://img.shields.io/badge/language-Kotlin-orange" alt="language" />
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/license-AGPL--3.0-success" alt="license" />
  </a>
  <a href="https://github.com/lurenjia534/SkyDrive-X/stargazers">
    <img src="https://img.shields.io/github/stars/lurenjia534/SkyDrive-X?style=flat-square" alt="GitHub stars" />
  </a>
  <a href="https://github.com/lurenjia534/SkyDrive-X/issues">
    <img src="https://img.shields.io/github/issues-raw/lurenjia534/SkyDrive-X" alt="open issues" />
  </a>
  <img src="https://img.shields.io/github/last-commit/lurenjia534/SkyDrive-X" alt="last commit" />
</p>

[中文说明 README_zh.md](README_zh.md)

## Overview

SkyDrive X is an Android client for Microsoft 365 / OneDrive users. It delivers a modern Jetpack Compose **Material 3** UI, integrates MSAL authentication, and talks to Microsoft Graph to offer browsing, upload/download, sharing, and media preview features. The app targets Android 12L (API 31) and later, with dedicated handling for Android 14+ foreground data sync and new permission models.

## Highlights

- Sign in with personal or work/school Microsoft accounts (MSAL single-account mode) and refresh tokens silently.
- Browse the OneDrive root and any subfolder with breadcrumb navigation, scrolling cache, and drive/folder search.
- File operations: multi-select, rename, delete, move, copy, create folders, and generate share links.
- Upload flow automatically switches between small and large file paths, runs as a foreground service with progress, supports cancel/resume, and can target the system Downloads directory or a custom tree.
- Downloads leverage `DownloadManager` or a custom saver, with rich notifications and persistent history.
- Preview photos (Coil 3), audio (Media3 + FFmpeg extension), and video (Media3 Compose UI with orientation and playback speed controls).
- Preferences and theme: DataStore for dark mode / download location, Room for transfer history to survive restarts, and a settings page for account information, quota, and notification status checks.

## Configuration Quick Reference

1. **Register Azure AD app** – Follow [MSAL_Config.md](MSAL_Config.md) to create the application, choose multitenant + personal accounts, and keep the generated `client_id`.
2. **Add Android redirect URI** – Configure package name `com.lurenjia534.skydrivex` and signature hash `rZDXYaNZmghPivXu+4dDWNfayVo=` under *Authentication → Add platform → Android*.
3. **Grant permissions** – In *API permissions*, add delegated `Files.ReadWrite` (keep `User.Read`) and grant admin consent if required by your tenant.
4. **Run the app** – Install SkyDrive X, enter the `client_id` in the OOBE wizard, and sign in. You can revisit the wizard from Settings → *Modify login configuration* whenever you need to switch apps.

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
│   ├── build.gradle.kts                 # Android module configuration
│   └── src/main/java/com/lurenjia534/skydrivex
│       ├── SkyDriveXApp.kt              # @HiltAndroidApp application class
│       ├── auth                         # MSAL login & token management
│       ├── data                         # Graph API, repositories, Room/DataStore
│       ├── di                           # Hilt modules
│       ├── work                         # WorkManager jobs (index sync, etc.)
│       └── ui                           # Compose activities, screens, components
├── gradle/libs.versions.toml            # Centralized dependency versions
├── MSAL_Config.md                       # Azure AD setup guide (English)
├── MSAL授权指南.md                      # Azure AD setup guide (Chinese)
├── README.md                            # English README
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

## FAQ

- **Blank screen or crash after sign-in**: Ensure the package name and signature hash you registered in Azure match the build you are installing, and re-run the OOBE wizard from Settings if you need to update the `client_id`.
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

SkyDrive X is distributed under the **GNU Affero General Public License v3.0**. See [LICENSE](LICENSE) for the full text. If you distribute modified versions or offer the software as a service, you must make the source available under the same AGPL-3.0 terms and preserve the original copyright and license notices.
