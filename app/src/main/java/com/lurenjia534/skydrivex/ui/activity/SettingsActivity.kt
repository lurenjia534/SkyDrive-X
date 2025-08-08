package com.lurenjia534.skydrivex.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.viewmodel.MainViewModel
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
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val areNotificationsEnabled by viewModel.areNotificationsEnabled.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
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
                    ListItem(
                        headlineContent = { Text("通知设置") },
                        supportingContent = { Text("系统设置中管理通知权限") },
                        trailingContent = {
                            Switch(
                                checked = areNotificationsEnabled,
                                onCheckedChange = { viewModel.openNotificationSettings() }
                            )
                        }
                    )
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
