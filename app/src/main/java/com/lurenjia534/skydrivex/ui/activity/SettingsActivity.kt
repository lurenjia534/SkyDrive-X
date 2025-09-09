package com.lurenjia534.skydrivex.ui.activity

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.lurenjia534.skydrivex.ui.settings.components.SectionHeader
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            SkyDriveXTheme(darkTheme = isDarkMode) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    activity = this
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkNotificationStatus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit,
    activity: ComponentActivity
) {
    val driveState by viewModel.driveState.collectAsState()
    val userState  by viewModel.userState.collectAsState()
    val account by viewModel.account.collectAsState()
    val token by viewModel.token.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val areNotificationsEnabled by viewModel.areNotificationsEnabled.collectAsState()
    val downloadPref by viewModel.downloadPreference.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                // persist permission
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    activity.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: SecurityException) {
                    // If persist fails, still save uri; operations may fail later and be surfaced to user
                }
                viewModel.setDownloadToCustom(uri.toString())
            }
        }
    )

    // Launcher to request media permissions (photos/videos, with partial access on Android 14+)
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val anyGranted = results.values.any { it }
        val msg = if (anyGranted) "媒体访问权限已更新" else "媒体访问权限被拒绝"
        scope.launch { snackbarHostState.showSnackbar(message = msg) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 账户分组
            item { SectionHeader("账户") }
            if (account == null) {
                item {
                    ListItem(
                        leadingContent = { Icon(imageVector = Icons.Outlined.AccountCircle, contentDescription = null) },
                        headlineContent = { Text("您尚未登录") },
                        supportingContent = { Text("登录以使用完整功能") }
                    )
                }
                item {
                    ListItem(
                        leadingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.Login, contentDescription = null) },
                        headlineContent = { Text("登录") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.signIn(activity) }
                    )
                }
            } else {
                item {
                    ListItem(
                        leadingContent = { Icon(imageVector = Icons.Outlined.AccountCircle, contentDescription = null) },
                        headlineContent = { Text("当前账户") },
                        supportingContent = {
                            Text(
                                account!!.username,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        leadingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                        headlineContent = {
                            Text(
                                text = "退出登录",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.signOut() }
                    )
                }
            }

            // OneDrive 信息
            if (account != null) {
                item { SectionHeader("OneDrive 信息") }
                when {
                    driveState.isLoading -> {
                        item { DriveInfoPlaceholder(modifier = Modifier.fillMaxWidth()) }
                    }
                    driveState.data != null -> {
                        // 访问令牌
                        item {
                            val clipboard = LocalClipboard.current
                            ListItem(
                                headlineContent = { Text("访问令牌") },
                                supportingContent = { Text(if (token != null) "点击复制到剪贴板" else "当前无令牌") },
                                trailingContent = {
                                    TextButton(onClick = {
                                        token?.let { t ->
                                            scope.launch {
                                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("token", t)))
                                                snackbarHostState.showSnackbar(message = "已复制到剪贴板")
                                            }
                                        }
                                    }, enabled = token != null) {
                                        Text("复制")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (token != null) Modifier.clickable {
                                            scope.launch {
                                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("token", token!!)))
                                                snackbarHostState.showSnackbar(message = "已复制到剪贴板")
                                            }
                                        } else Modifier
                                    )
                            )
                        }
                        // 类型
                        driveState.data!!.driveType?.let { type ->
                            val typeText = if (type == "personal") "个人版" else "企业版"
                            item {
                                ListItem(
                                    headlineContent = { Text("OneDrive 类型") },
                                    supportingContent = { Text(typeText) }
                                )
                            }
                        }
                        // 存储
                        driveState.data!!.quota?.let { quota ->
                            if (quota.total != null && quota.used != null) {
                                item {
                                    ListItem(
                                        headlineContent = { Text("存储空间") },
                                        supportingContent = {
                                            Column {
                                                Text("${formatBytes(quota.used)} / ${formatBytes(quota.total)}")
                                                Spacer(modifier = Modifier.height(8.dp))
                                                LinearProgressIndicator(
                                                    progress = { quota.used.toFloat() / quota.total.toFloat() },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            quota.remaining?.let { r ->
                                item { ListItem(headlineContent = { Text("剩余") }, supportingContent = { Text(formatBytes(r)) }) }
                            }
                            quota.deleted?.let { d ->
                                item { ListItem(headlineContent = { Text("已删除") }, supportingContent = { Text(formatBytes(d)) }) }
                            }
                            quota.state?.let { s ->
                                item { ListItem(headlineContent = { Text("状态") }, supportingContent = { Text(s) }) }
                            }
                            quota.storagePlanInformation?.upgradeAvailable?.let { up ->
                                item { ListItem(headlineContent = { Text("可升级") }, supportingContent = { Text(if (up) "是" else "否") }) }
                            }
                        }
                    }
                }
            }

            // 个人信息（合并原 ProfileScreen 信息到设置页风格）
            if (account != null) {
                item { SectionHeader("个人信息") }
                when {
                    userState.isLoading -> {
                        // 简单占位（保持设置风格）
                        item { ListItem(headlineContent = { Text("正在加载个人信息…") }) }
                    }
                    userState.data != null -> {
                        val u = userState.data!!
                        fun addItem(label: String, value: String?) {
                            if (!value.isNullOrBlank()) {
                                @Suppress("UNUSED_EXPRESSION")
                                run {
                                    // 使用 apply_patch 追加 item 需要在 Kotlin 内部构造 LazyList DSL，改用 when 块内多 item {}
                                }
                            }
                        }
                    }
                    userState.error != null -> {
                        item { ListItem(headlineContent = { Text("加载失败") }, supportingContent = { Text(userState.error ?: "") }) }
                    }
                }
                // 依次加入字段（用可空判断）
                if (userState.data != null) {
                    val u = userState.data!!
                    if (!u.displayName.isNullOrBlank()) item { ListItem(headlineContent = { Text("显示名称") }, supportingContent = { Text(
                        u.displayName
                    ) }) }
                    if (!u.userPrincipalName.isNullOrBlank()) item { ListItem(headlineContent = { Text("用户主体名称") }, supportingContent = { Text(
                        u.userPrincipalName
                    ) }) }
                    if (!u.givenName.isNullOrBlank()) item { ListItem(headlineContent = { Text("名") }, supportingContent = { Text(
                        u.givenName
                    ) }) }
                    if (!u.surname.isNullOrBlank()) item { ListItem(headlineContent = { Text("姓") }, supportingContent = { Text(
                        u.surname
                    ) }) }
                    if (!u.jobTitle.isNullOrBlank()) item { ListItem(headlineContent = { Text("职位") }, supportingContent = { Text(
                        u.jobTitle
                    ) }) }
                    if (!u.mail.isNullOrBlank()) item { ListItem(headlineContent = { Text("邮箱") }, supportingContent = { Text(
                        u.mail
                    ) }) }
                    if (!u.mobilePhone.isNullOrBlank()) item { ListItem(headlineContent = { Text("手机") }, supportingContent = { Text(
                        u.mobilePhone
                    ) }) }
                    if (!u.officeLocation.isNullOrBlank()) item { ListItem(headlineContent = { Text("办公地点") }, supportingContent = { Text(
                        u.officeLocation
                    ) }) }
                    if (!u.preferredLanguage.isNullOrBlank()) item { ListItem(headlineContent = { Text("首选语言") }, supportingContent = { Text(
                        u.preferredLanguage
                    ) }) }
                    val phones = u.businessPhones?.filter { it.isNotBlank() }?.joinToString()
                    if (!phones.isNullOrBlank()) item { ListItem(headlineContent = { Text("商务电话") }, supportingContent = { Text(phones) }) }
                }
            }

            // 应用设置
            item { SectionHeader("应用设置") }
            // 下载位置
            item {
                Text(
                    text = "下载位置",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("系统下载目录") },
                    supportingContent = { Text("使用系统公共下载目录") },
                    trailingContent = {
                        RadioButton(
                            selected = downloadPref.mode.name == "SYSTEM_DOWNLOADS",
                            onClick = { viewModel.setDownloadToSystem() }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setDownloadToSystem() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("自定义目录") },
                    supportingContent = { Text("使用你选择的目录保存下载内容") },
                    trailingContent = {
                        RadioButton(
                            selected = downloadPref.mode.name == "CUSTOM_TREE",
                            onClick = { viewModel.setDownloadModeCustom() }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setDownloadModeCustom() }
                )
            }
            if (downloadPref.mode.name == "CUSTOM_TREE") {
                item {
                    ListItem(
                        headlineContent = { Text("选择文件夹") },
                        supportingContent = { Text("当前：" + formatTreeUri(downloadPref.treeUri)) },
                        trailingContent = {
                            TextButton(onClick = { pickFolderLauncher.launch(null) }) {
                                Text("选择文件夹")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickFolderLauncher.launch(null) }
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text("深色模式") },
                    supportingContent = { Text("管理是否开启暗色模式") },
                    trailingContent = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = viewModel::setDarkMode
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setDarkMode(!isDarkMode) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("通知设置") },
                    supportingContent = { Text("在系统设置中管理应用通知权限") },
                    trailingContent = {
                        Switch(
                            checked = areNotificationsEnabled,
                            onCheckedChange = null,
                            enabled = false
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openNotificationSettings() }
                )
            }

            // 权限
            item { SectionHeader("权限") }
            item {
                run {
                    val hasImages = if (Build.VERSION.SDK_INT >= 33) {
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    } else {
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    }
                    val hasVideos = if (Build.VERSION.SDK_INT >= 33) {
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                    } else {
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    }
                    val hasSelected = if (Build.VERSION.SDK_INT >= 34) {
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
                    } else false

                    val statusText = when {
                        Build.VERSION.SDK_INT >= 34 && hasSelected && !hasImages && !hasVideos -> "已授予部分访问（精选的照片/视频）"
                        hasImages && hasVideos -> "已授予全部照片与视频访问"
                        hasImages && !hasVideos -> "仅授予照片访问"
                        !hasImages && hasVideos -> "仅授予视频访问"
                        else -> "未授予媒体访问权限"
                    }

                    val requestPerms = when {
                        Build.VERSION.SDK_INT >= 34 -> arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                        )
                        Build.VERSION.SDK_INT >= 33 -> arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                        )
                        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }

                    ListItem(
                        headlineContent = { Text("媒体访问权限") },
                        supportingContent = { Text(statusText) },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { mediaPermissionLauncher.launch(requestPerms) }) {
                                    Text("授予/更新")
                                }
                                TextButton(onClick = {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", activity.packageName, null)
                                    )
                                    activity.startActivity(intent)
                                }) {
                                    Text("在系统中管理")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mediaPermissionLauncher.launch(requestPerms) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DriveInfoPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 占位行 1
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .placeholder(
                        visible = true,
                        highlight = PlaceholderHighlight.shimmer(),
                        shape = MaterialTheme.shapes.small
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(18.dp)
                        .placeholder(
                            visible = true,
                            highlight = PlaceholderHighlight.shimmer(),
                            shape = MaterialTheme.shapes.small
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .placeholder(
                            visible = true,
                            highlight = PlaceholderHighlight.shimmer(),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
        // 占位行 2
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .placeholder(
                        visible = true,
                        highlight = PlaceholderHighlight.shimmer(),
                        shape = MaterialTheme.shapes.small
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(18.dp)
                        .placeholder(
                            visible = true,
                            highlight = PlaceholderHighlight.shimmer(),
                            shape = MaterialTheme.shapes.small
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .placeholder(
                            visible = true,
                            highlight = PlaceholderHighlight.shimmer(),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
        // 进度条占位
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .placeholder(
                    visible = true,
                    highlight = PlaceholderHighlight.shimmer(),
                    shape = MaterialTheme.shapes.medium
                )
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024L
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> "%.2f GB".format(java.util.Locale.US, bytes.toDouble() / gb)
        bytes >= mb -> "%.2f MB".format(java.util.Locale.US, bytes.toDouble() / mb)
        bytes >= kb -> "%.2f KB".format(java.util.Locale.US, bytes.toDouble() / kb)
        else -> "$bytes B"
    }
}

// 将 SAF treeUri 更友好地显示，例如
// content://.../tree/primary%3ADownload%2FOneDrive -> 内部存储/Download/OneDrive
private fun formatTreeUri(uri: String?): String {
    if (uri.isNullOrBlank()) return "未选择目录"
    return try {
        val u = uri.toUri()
        val last = u.lastPathSegment ?: return uri
        val decoded = java.net.URLDecoder.decode(last, "UTF-8")
        val core = decoded.substringAfter("tree/", decoded)
        val path = core.substringAfter(":", core)
        val prefix = if (core.startsWith("primary:")) "内部存储/" else ""
        prefix + path
    } catch (_: Exception) {
        uri
    }
}
