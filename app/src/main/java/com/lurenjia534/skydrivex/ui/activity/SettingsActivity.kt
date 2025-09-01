package com.lurenjia534.skydrivex.ui.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.ClipData
import android.widget.Toast
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.input.nestedscroll.nestedScroll
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

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
    val account by viewModel.account.collectAsState()
    val token by viewModel.token.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val areNotificationsEnabled by viewModel.areNotificationsEnabled.collectAsState()
    val scrollState = rememberScrollState()
    val downloadPref by viewModel.downloadPreference.collectAsState()

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
        Toast.makeText(
            activity,
            if (anyGranted) "媒体访问权限已更新" else "媒体访问权限被拒绝",
            Toast.LENGTH_SHORT
        ).show()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "账户",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (account == null) {
                        // 未登录状态
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "您尚未登录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { viewModel.signIn(activity) },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "登录",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // 已登录状态
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "当前账户",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = account!!.username,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.signOut() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "退出登录",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 可以添加更多设置项
            // OneDrive 信息卡片
            if (account != null) {
                when {
                    driveState.isLoading -> {
                        DriveInfoPlaceholder(modifier = Modifier.fillMaxWidth())
                    }
                    driveState.data != null -> {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "OneDrive 信息",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // 复制令牌：使用 ListItem，整行可点 + 尾部轻量 TextButton
                                val clipboard = LocalClipboard.current
                                ListItem(
                                    leadingContent = { Icon(imageVector = Icons.Outlined.Key, contentDescription = null) },
                                    headlineContent = { Text("访问令牌") },
                                    supportingContent = { Text(if (token != null) "点击复制到剪贴板" else "当前无令牌") },
                                    trailingContent = {
                                        TextButton(onClick = {
                                            token?.let { t ->
                                                scope.launch {
                                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("token", t)))
                                                    Toast.makeText(activity, "已复制", Toast.LENGTH_SHORT).show()
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
                                                    Toast.makeText(activity, "已复制", Toast.LENGTH_SHORT).show()
                                                }
                                            } else Modifier
                                        )
                                )

                                driveState.data!!.driveType?.let { type ->
                                    val typeText = if (type == "personal") "个人版" else "企业版"
                                    ListItem(
                                        leadingContent = {
                                            Icon(
                                                imageVector = if (type == "business") Icons.Outlined.Work else Icons.Outlined.Person,
                                                contentDescription = null,
                                            )
                                        },
                                        headlineContent = { Text("OneDrive 类型") },
                                        supportingContent = { Text(typeText) },
                                    )
                                }

                                driveState.data!!.quota?.let { quota ->
                                    if (quota.total != null && quota.used != null) {
                                        ListItem(
                                            leadingContent = { Icon(Icons.Outlined.Storage, contentDescription = null) },
                                            headlineContent = { Text("存储空间") },
                                            supportingContent = {
                                                Text("${formatBytes(quota.used)} / ${formatBytes(quota.total)}")
                                            },
                                        )

                                        LinearProgressIndicator(
                                            progress = { quota.used.toFloat() / quota.total.toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "应用设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // 下载位置：单选（系统/自定义）。自定义时显示“选择文件夹”二级行。
                    Text(
                        text = "下载位置",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // 系统下载目录
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
                    // 自定义目录：切换到自定义模式，不直接打开选择器
                    ListItem(
                        headlineContent = { Text("自定义目录") },
                        supportingContent = { Text(downloadPref.treeUri?.let { "当前：$it" } ?: "未选择") },
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
                    // 二级行：仅在自定义模式下出现“选择文件夹”操作，使用尾部 TextButton
                    if (downloadPref.mode.name == "CUSTOM_TREE") {
                        ListItem(
                            headlineContent = { Text("选择文件夹") },
                            supportingContent = { Text(downloadPref.treeUri ?: "未选择目录") },
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
                    ListItem(
                        headlineContent = { Text("深色模式") },
                        supportingContent = { Text("管理是否开启暗色模式") },
                        trailingContent = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = viewModel::setDarkMode
                            )
                        }
                    )
                    // 通知设置：整行可点跳系统设置，Switch 仅作状态指示
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
                    // 媒体访问权限（照片/视频），支持 Android 14+ 的“部分访问（精选）”
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

                        // 媒体权限：整行可点请求权限 + 尾部轻量操作
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
                    Text(
                        text = "其他设置选项即将推出...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DriveInfoPlaceholder(modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(24.dp)
                    .placeholder(
                        visible = true,
                        highlight = PlaceholderHighlight.shimmer(),
                        shape = MaterialTheme.shapes.small
                    )
            )
            repeat(2) {
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
                    Column {
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(16.dp)
                                .placeholder(
                                    visible = true,
                                    highlight = PlaceholderHighlight.shimmer(),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }
            }
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
