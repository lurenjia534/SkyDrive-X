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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurenjia534.skydrivex.ui.settings.components.AccountManagerSheet
import com.lurenjia534.skydrivex.ui.settings.components.CopyableListItem
import com.lurenjia534.skydrivex.ui.settings.components.CopyableCustomItem
import com.lurenjia534.skydrivex.ui.settings.components.SectionHeader
import com.lurenjia534.skydrivex.ui.settings.components.SettingsSkeletonListItem
import com.lurenjia534.skydrivex.ui.settings.components.SettingsSkeletonStorage
import com.lurenjia534.skydrivex.ui.settings.components.formatBytes
import com.lurenjia534.skydrivex.ui.settings.components.formatTreeUri
import com.lurenjia534.skydrivex.ui.settings.LocalIndexSettingsViewModel
import com.lurenjia534.skydrivex.ui.settings.components.LocalIndexSection
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
/**
 * 设置页 Activity：只负责创建 Compose 内容并托管 [MainViewModel]，其余逻辑交由 Composable 处理。
 */
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

/**
 * 设置页主界面，集中呈现账户、OneDrive 信息、偏好设置及权限管理。
 * 内部包含多处状态收集与副作用处理（复制、SnackBar、权限申请等）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit,
    activity: ComponentActivity
) {
    val driveState by viewModel.driveState.collectAsState()
    val userState  by viewModel.userState.collectAsState()
    val activeAccount by viewModel.activeAccount.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val token by viewModel.token.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val areNotificationsEnabled by viewModel.areNotificationsEnabled.collectAsState()
    val downloadPref by viewModel.downloadPreference.collectAsState()
    val indexSettingsViewModel: LocalIndexSettingsViewModel = hiltViewModel()
    val indexState by indexSettingsViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    var pendingSnack by remember { mutableStateOf<String?>(null) }
    var pendingCopy by remember { mutableStateOf<String?>(null) }
    var showAccountManagerSheet by remember { mutableStateOf(false) }

    // 选择自定义下载目录的 launcher：申请持久化读写授权后写入偏好
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
    // 媒体权限 launcher：Android 13/14 拆分权限，回传结果后统一提示
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val anyGranted = results.values.any { it }
        val msg = if (anyGranted) "媒体访问权限已更新" else "媒体访问权限被拒绝"
        pendingSnack = msg
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        // 统一通过 LaunchedEffect 展示 Snack，避免在 Composition 期间直接 launch
        LaunchedEffect(pendingSnack) {
            pendingSnack?.let {
                snackbarHostState.showSnackbar(it)
                pendingSnack = null
            }
        }
        LaunchedEffect(Unit) {
            viewModel.refreshAccounts(triggerTokenRefresh = false)
        }
        LaunchedEffect(pendingCopy) {
            pendingCopy?.let { text ->
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("copy", text)))
                pendingCopy = null
            }
        }
        if (showAccountManagerSheet) {
            AccountManagerSheet(
                accounts = accounts,
                activeAccount = activeAccount,
                onSetActive = viewModel::setActiveAccount,
                onRemove = viewModel::removeAccount,
                onAddAccount = { viewModel.signIn(activity) },
                onDismiss = { showAccountManagerSheet = false }
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 账户分组
            item { SectionHeader("账户") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(
                        leadingContent = { Icon(imageVector = Icons.Outlined.AccountCircle, contentDescription = null) },
                        headlineContent = { Text(if (activeAccount == null) "暂无激活账户" else "当前账户") },
                        supportingContent = {
                            Text(
                                activeAccount?.username ?: "登录或选择一个账户以继续",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(1.dp)) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(
                        leadingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.Login, contentDescription = null) },
                        headlineContent = { Text("添加 / 登录账户") },
                        supportingContent = { Text("支持缓存多个账户，激活后使用") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.signIn(activity) }
                    )
                }
            }
            item { Spacer(Modifier.height(1.dp)) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(
                        leadingContent = { Icon(imageVector = Icons.Outlined.AccountCircle, contentDescription = null) },
                        headlineContent = { Text("管理账户") },
                        supportingContent = { Text("已保存 ${accounts.size} 个账户，可切换激活或移除") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccountManagerSheet = true }
                    )
                }
            }
            if (activeAccount != null) {
                item { Spacer(Modifier.height(1.dp)) }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                        ListItem(
                            leadingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                            headlineContent = {
                                Text(
                                    text = "退出当前账户",
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
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val intent = Intent(activity, OobeActivity::class.java).apply {
                                putExtra(OobeActivity.EXTRA_MODE, OobeMode.UPDATE.name)
                            }
                            activity.startActivity(intent)
                        }
                    ) {
                        Text("修改登陆配置")
                    }
                }
            }

            // OneDrive 信息
            if (activeAccount != null) {
                item { SectionHeader("OneDrive 信息") }
                when {
                    driveState.isLoading -> {
                        val detailHeadlineRatios = listOf(0.45f, 0.32f, 0.36f, 0.28f)

                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                SettingsSkeletonListItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    headlineRatio = 0.4f,
                                    supportingRatios = listOf(0.65f),
                                    showTrailing = true
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }

                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                SettingsSkeletonListItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    headlineRatio = 0.48f,
                                    supportingRatios = listOf(0.52f)
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }

                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                SettingsSkeletonStorage(modifier = Modifier.fillMaxWidth())
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }

                        detailHeadlineRatios.forEach { ratio ->
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                    SettingsSkeletonListItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        headlineRatio = ratio,
                                        supportingRatios = listOf(0.5f)
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(1.dp)) }
                        }
                    }
                    driveState.data != null -> {
                        // 访问令牌
                        item {
                            val clipboard = LocalClipboard.current
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                            CopyableListItem(
                                headline = "访问令牌",
                                supporting = if (token != null) "点击复制到剪贴板" else "当前无令牌",
                                trailing = {
                                    TextButton(onClick = {
                                        token?.let { t ->
                                            pendingCopy = t
                                            pendingSnack = "已复制到剪贴板"
                                        }
                                    }, enabled = token != null) { Text("复制") }
                                },
                                onCopy = { _ ->
                                    token?.let { t ->
                                        pendingCopy = t
                                        pendingSnack = "已复制到剪贴板"
                                    }
                                }
                            )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                        // 类型
                        driveState.data!!.driveType?.let { type ->
                            val typeText = if (type == "personal") "个人版" else "企业版"
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                    CopyableListItem(
                                    headline = "OneDrive 类型",
                                    supporting = typeText,
                                    onCopy = { value ->
                                        pendingCopy = value
                                        pendingSnack = "已复制：$value"
                                        }
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(1.dp)) }
                        }
                        // 存储
                        driveState.data!!.quota?.let { quota ->
                            if (quota.total != null && quota.used != null) {
                                item {
                                    val usedText = "${formatBytes(quota.used)} / ${formatBytes(quota.total)}"
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                    CopyableCustomItem(
                                        headline = "存储空间",
                                        copyText = usedText,
                                        onCopy = { value ->
                                            pendingCopy = value
                                            pendingSnack = "已复制：$value"
                                        }
                                    ) {
                                        Column {
                                            Text(usedText)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = { quota.used.toFloat() / quota.total.toFloat() },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    }
                                }
                                item { Spacer(Modifier.height(1.dp)) }
                            }
                            quota.remaining?.let { r ->
                                item {
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                        CopyableListItem(
                                        headline = "剩余",
                                        supporting = formatBytes(r),
                                        onCopy = { value ->
                                            pendingCopy = value
                                            pendingSnack = "已复制：$value"
                                        }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(1.dp)) }
                            }
                            quota.deleted?.let { d ->
                                item {
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                        CopyableListItem(
                                        headline = "已删除",
                                        supporting = formatBytes(d),
                                        onCopy = { value ->
                                            pendingCopy = value
                                            pendingSnack = "已复制：$value"
                                        }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(1.dp)) }
                            }
                            quota.state?.let { s ->
                                item {
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                        CopyableListItem(
                                        headline = "状态",
                                        supporting = s,
                                        onCopy = { value ->
                                            pendingCopy = value
                                            pendingSnack = "已复制：$value"
                                        }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(1.dp)) }
                            }
                            quota.storagePlanInformation?.upgradeAvailable?.let { up ->
                                item {
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                        CopyableListItem(
                                        headline = "可升级",
                                        supporting = if (up) "是" else "否",
                                        onCopy = { value ->
                                            pendingCopy = value
                                            pendingSnack = "已复制：$value"
                                        }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(1.dp)) }
                            }
                        }
                    }
                }
            }

            // 个人信息
            if (activeAccount != null) {
                item { SectionHeader("个人信息") }
                when {
                    userState.isLoading -> {
                        val userHeadlineRatios = listOf(0.28f, 0.34f, 0.3f, 0.26f, 0.32f, 0.24f)
                        userHeadlineRatios.forEach { ratio ->
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                    SettingsSkeletonListItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        headlineRatio = ratio,
                                        supportingRatios = listOf(0.9f)
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(1.dp)) }
                        }
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

                    if (!u.displayName.isNullOrBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                CopyableListItem(
                                    headline = "显示名称",
                                    supporting = u.displayName,
                                    onCopy = { copied ->
                                        pendingCopy = copied
                                        pendingSnack = "已复制：$copied"
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                    }

                    if (!u.userPrincipalName.isNullOrBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                CopyableListItem(
                                    headline = "用户主体名称",
                                    supporting = u.userPrincipalName,
                                    onCopy = { copied ->
                                        pendingCopy = copied
                                        pendingSnack = "已复制：$copied"
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                    }

                    if (!u.givenName.isNullOrBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                CopyableListItem(
                                    headline = "名",
                                    supporting = u.givenName,
                                    onCopy = { copied ->
                                        pendingCopy = copied
                                        pendingSnack = "已复制：$copied"
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                    }

                    if (!u.surname.isNullOrBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                CopyableListItem(
                                    headline = "姓",
                                    supporting = u.surname,
                                    onCopy = { copied ->
                                        pendingCopy = copied
                                        pendingSnack = "已复制：$copied"
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                    }

                    if (!u.jobTitle.isNullOrBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                CopyableListItem(
                                    headline = "职位",
                                    supporting = u.jobTitle,
                                    onCopy = { copied ->
                                        pendingCopy = copied
                                        pendingSnack = "已复制：$copied"
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                    }

                    if (!u.mail.isNullOrBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                CopyableListItem(
                                    headline = "邮箱",
                                    supporting = u.mail,
                                    onCopy = { copied ->
                                        pendingCopy = copied
                                        pendingSnack = "已复制：$copied"
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                    }

                    if (!u.mobilePhone.isNullOrBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                CopyableListItem(
                                    headline = "手机",
                                    supporting = u.mobilePhone,
                                    onCopy = { copied ->
                                        pendingCopy = copied
                                        pendingSnack = "已复制：$copied"
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                    }

                    if (!u.officeLocation.isNullOrBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                CopyableListItem(
                                    headline = "办公地点",
                                    supporting = u.officeLocation,
                                    onCopy = { copied ->
                                        pendingCopy = copied
                                        pendingSnack = "已复制：$copied"
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                    }

                    if (!u.preferredLanguage.isNullOrBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                CopyableListItem(
                                    headline = "首选语言",
                                    supporting = u.preferredLanguage,
                                    onCopy = { copied ->
                                        pendingCopy = copied
                                        pendingSnack = "已复制：$copied"
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                    }

                    val phones = u.businessPhones?.filter { it.isNotBlank() }?.joinToString()
                    if (!phones.isNullOrBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                                CopyableListItem(
                                    headline = "商务电话",
                                    supporting = phones,
                                    onCopy = { copied ->
                                        pendingCopy = copied
                                        pendingSnack = "已复制：$copied"
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(1.dp)) }
                    }
                }
            }

            // 应用设置
            item { SectionHeader("应用设置") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(
                        leadingContent = { Icon(imageVector = Icons.Outlined.PhotoLibrary, contentDescription = null) },
                        headlineContent = { Text("相册同步") },
                        supportingContent = { Text("管理本地照片的自动同步设置") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(activity, PhotoSyncActivity::class.java)
                                activity.startActivity(intent)
                            }
                    )
                }
            }
            item { Spacer(Modifier.height(1.dp)) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
            }
            item { Spacer(Modifier.height(1.dp)) }
            // 通知设置
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
            }
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
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
            }
            item { Spacer(Modifier.height(1.dp)) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
            }
            if (downloadPref.mode.name == "CUSTOM_TREE") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
            }

            item { SectionHeader("本地索引（预览）") }
            item {
                LocalIndexSection(
                    state = indexState,
                    onEnabledChange = indexSettingsViewModel::setEnabled,
                    onWifiOnlyChange = indexSettingsViewModel::setWifiOnly,
                    onChargeOnlyChange = indexSettingsViewModel::setChargeOnly,
                    onClearClicked = indexSettingsViewModel::clearIndex,
                    onRebuildNow = indexSettingsViewModel::rebuildNow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            

            // 权限
            item { SectionHeader("权限") }
            item {
                run {
                    // 按系统版本组合需申请的媒体权限，并生成状态文案
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

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
            // 其他
            item { SectionHeader("其他") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(
                    headlineContent = { Text("关于我们") },
                    supportingContent = { },
                    trailingContent = {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.Launch, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(activity, AboutActivity::class.java)
                            activity.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

// Placeholders moved to ui.settings.components.Placeholders
