package com.lurenjia534.skydrivex.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.lurenjia534.skydrivex.viewmodel.DriveUiState
import com.lurenjia534.skydrivex.viewmodel.UserUiState
import java.util.Locale
import androidx.compose.ui.platform.ClipEntry
import android.content.ClipData
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: UserUiState,
    driveState: DriveUiState,
    token: String?,
    onRefresh: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (
            (uiState.data == null || driveState.data == null) &&
            uiState.error == null &&
            driveState.error == null &&
            !uiState.isLoading &&
            !driveState.isLoading
        ) {
            onRefresh()
        }
    }

    LaunchedEffect(uiState.error, uiState.data, driveState.error, driveState.data) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
        driveState.error?.let { snackbarHostState.showSnackbar(it) }
        if (
            uiState.error == null &&
            driveState.error == null &&
            uiState.data != null &&
            driveState.data != null
        ) {
            snackbarHostState.showSnackbar("加载成功")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("个人中心") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
                actions = {
                    IconButton(
                        onClick = {
                            token?.let { t ->
                                scope.launch {
                                    // 写入纯文本到剪贴板（label 可随便起）
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText("token", t))
                                    )
                                    // 可选：给个提示
                                    snackbarHostState.showSnackbar("已复制")
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Key, contentDescription = "复制令牌")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val isSuccess = data.visuals.message.contains("成功")
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isSuccess) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading || driveState.isLoading -> {
                    ProfileLoadingPlaceholder(modifier = Modifier.align(Alignment.TopCenter))
                }

                uiState.error != null || driveState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = uiState.error ?: driveState.error ?: "")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text(text = "重试")
                        }
                    }
                }

                uiState.data != null && driveState.data != null -> {
                    val drive = driveState.data
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.TopCenter),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 4.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        stickyHeader {
                            Text(
                                text = "个人信息",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = 8.dp)
                            )
                        }
                        item {
                            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    InfoItem(
                                        label = "ID",
                                        value = uiState.data.id,
                                        leadingIcon = Icons.Outlined.Badge
                                    )
                                    uiState.data.businessPhones?.let {
                                        InfoItem(
                                            label = "商务电话",
                                            value = it.joinToString(", "),
                                            leadingIcon = Icons.Outlined.Phone
                                        )
                                    }
                                    uiState.data.displayName?.let {
                                        InfoItem(
                                            label = "显示名称",
                                            value = it,
                                            leadingIcon = Icons.Outlined.Person
                                        )
                                    }
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        maxItemsInEachRow = 2
                                    ) {
                                        uiState.data.givenName?.let {
                                            InfoItem(
                                                label = "名",
                                                value = it,
                                                leadingIcon = Icons.Outlined.AccountBox,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        uiState.data.surname?.let {
                                            InfoItem(
                                                label = "姓",
                                                value = it,
                                                leadingIcon = Icons.Outlined.AccountBox,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    uiState.data.jobTitle?.let {
                                        InfoItem(
                                            label = "职位",
                                            value = it,
                                            leadingIcon = Icons.Outlined.Work
                                        )
                                    }
                                    uiState.data.mail?.let {
                                        InfoItem(
                                            label = "邮箱",
                                            value = it,
                                            leadingIcon = Icons.Outlined.Email
                                        )
                                    }
                                    uiState.data.mobilePhone?.let {
                                        InfoItem(
                                            label = "手机",
                                            value = it,
                                            leadingIcon = Icons.Outlined.Smartphone
                                        )
                                    }
                                    uiState.data.officeLocation?.let {
                                        InfoItem(
                                            label = "办公地点",
                                            value = it,
                                            leadingIcon = Icons.Outlined.LocationOn
                                        )
                                    }
                                    uiState.data.preferredLanguage?.let {
                                        InfoItem(
                                            label = "首选语言",
                                            value = it,
                                            leadingIcon = Icons.Outlined.Language
                                        )
                                    }
                                    uiState.data.userPrincipalName?.let {
                                        InfoItem(
                                            label = "用户主体名称",
                                            value = it,
                                            leadingIcon = Icons.Outlined.AccountCircle
                                        )
                                    }
                                }
                            }
                        }
                        drive.quota?.let { quota ->
                            stickyHeader {
                                Text(
                                    text = "存储配额",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(vertical = 8.dp)
                                )
                            }
                            item {
                                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        drive.driveType?.let { type ->
                                            val typeText = if (type == "personal") "个人版" else "企业版"
                                            InfoItem(
                                                label = "OneDrive 类型",
                                                value = typeText,
                                                leadingIcon = if (type == "business") Icons.Outlined.Work else Icons.Outlined.Person,
                                            )
                                        }
                                        quota.total?.let { total ->
                                            quota.used?.let { used ->
                                                InfoItem(
                                                    label = "已使用",
                                                    value = "${formatBytes(used)} / ${
                                                        formatBytes(
                                                            total
                                                        )
                                                    }",
                                                    leadingIcon = Icons.Outlined.DataUsage
                                                )
                                                LinearProgressIndicator(
                                                    progress = { used.toFloat() / total.toFloat() },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                )
                                            }
                                        }
                                        quota.remaining?.let {
                                            InfoItem(
                                                label = "剩余",
                                                value = formatBytes(it),
                                                leadingIcon = Icons.Outlined.Storage
                                            )
                                        }
                                        quota.deleted?.let {
                                            InfoItem(
                                                label = "已删除",
                                                value = formatBytes(it),
                                                leadingIcon = Icons.Outlined.Delete
                                            )
                                        }
                                        quota.state?.let {
                                            InfoItem(
                                                label = "状态",
                                                value = it,
                                                leadingIcon = Icons.Outlined.Info
                                            )
                                        }
                                        quota.storagePlanInformation?.upgradeAvailable?.let {
                                            InfoItem(
                                                label = "可升级",
                                                value = "$it",
                                                leadingIcon = Icons.Outlined.SystemUpdate
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "请先登录或刷新")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text(text = "刷新")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    ListItem(
        modifier = modifier,
        leadingContent = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        headlineContent = { Text(label) },
        supportingContent = { Text(value) },
    )
}

@Composable
private fun ProfileLoadingPlaceholder(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题占位符
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(28.dp)
                    .padding(vertical = 4.dp)
                    .placeholder(
                        visible = true,
                        highlight = PlaceholderHighlight.shimmer(),
                        shape = MaterialTheme.shapes.medium
                    )
            )
        }

        // 个人信息卡片占位符
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 模拟ListItem的骨架结构
                    repeat(5) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 图标占位符
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(24.dp)
                                    .placeholder(
                                        visible = true,
                                        highlight = PlaceholderHighlight.shimmer(),
                                        shape = MaterialTheme.shapes.small
                                    )
                            )

                            Column {
                                // 标题占位符
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.3f)
                                        .height(18.dp)
                                        .placeholder(
                                            visible = true,
                                            highlight = PlaceholderHighlight.shimmer(),
                                            shape = MaterialTheme.shapes.small
                                        )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // 内容占位符
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
                }
            }
        }

        // 存储配额标题占位符
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(28.dp)
                    .padding(vertical = 4.dp)
                    .placeholder(
                        visible = true,
                        highlight = PlaceholderHighlight.shimmer(),
                        shape = MaterialTheme.shapes.medium
                    )
            )
        }

        // 存储配额卡片占位符
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 类型信息占位符
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp)
                                .placeholder(
                                    visible = true,
                                    highlight = PlaceholderHighlight.shimmer(),
                                    shape = MaterialTheme.shapes.small
                                )
                        )

                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.3f)
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
                                    .fillMaxWidth(0.5f)
                                    .height(16.dp)
                                    .placeholder(
                                        visible = true,
                                        highlight = PlaceholderHighlight.shimmer(),
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                        }
                    }

                    // 使用量信息占位符
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp)
                                .placeholder(
                                    visible = true,
                                    highlight = PlaceholderHighlight.shimmer(),
                                    shape = MaterialTheme.shapes.small
                                )
                        )

                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.3f)
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
                                    .fillMaxWidth(0.6f)
                                    .height(16.dp)
                                    .placeholder(
                                        visible = true,
                                        highlight = PlaceholderHighlight.shimmer(),
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                        }
                    }

                    // 进度条占位符
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .padding(vertical = 4.dp)
                            .placeholder(
                                visible = true,
                                highlight = PlaceholderHighlight.shimmer(),
                                shape = MaterialTheme.shapes.medium
                            )
                    )

                    // 额外的两个信息项占位符
                    repeat(2) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(24.dp)
                                    .placeholder(
                                        visible = true,
                                        highlight = PlaceholderHighlight.shimmer(),
                                        shape = MaterialTheme.shapes.small
                                    )
                            )

                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.3f)
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
                                        .fillMaxWidth(0.4f)
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
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024L
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> "%.2f GB".format(Locale.US, bytes.toDouble() / gb)
        bytes >= mb -> "%.2f MB".format(Locale.US, bytes.toDouble() / mb)
        bytes >= kb -> "%.2f KB".format(Locale.US, bytes.toDouble() / kb)
        else -> "$bytes B"

    }
}
