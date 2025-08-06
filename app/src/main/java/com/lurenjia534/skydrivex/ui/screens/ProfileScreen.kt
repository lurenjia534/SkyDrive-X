package com.lurenjia534.skydrivex.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurenjia534.skydrivex.viewmodel.DriveUiState
import com.lurenjia534.skydrivex.viewmodel.UserUiState

@Composable
fun ProfileScreen(
    uiState: UserUiState,
    driveState: DriveUiState,
    onRefresh: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading || driveState.isLoading -> {
                    CircularProgressIndicator()
                }

                uiState.error != null || driveState.error != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = uiState.error ?: driveState.error ?: "")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text(text = "重试")
                        }
                    }
                }
                uiState.data != null && driveState.data != null -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "个人信息",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    InfoItem(label = "ID", value = uiState.data.id)
                                    uiState.data.businessPhones?.let {
                                        InfoItem(
                                            label = "商务电话",
                                            value = it.joinToString(", ")
                                        )
                                    }
                                    uiState.data.displayName?.let {
                                        InfoItem(label = "显示名称", value = it)
                                    }
                                    uiState.data.givenName?.let {
                                        InfoItem(label = "名", value = it)
                                    }
                                    uiState.data.jobTitle?.let {
                                        InfoItem(label = "职位", value = it)
                                    }
                                    uiState.data.mail?.let {
                                        InfoItem(label = "邮箱", value = it)
                                    }
                                    uiState.data.mobilePhone?.let {
                                        InfoItem(label = "手机", value = it)
                                    }
                                    uiState.data.officeLocation?.let {
                                        InfoItem(label = "办公地点", value = it)
                                    }
                                    uiState.data.preferredLanguage?.let {
                                        InfoItem(label = "首选语言", value = it)
                                    }
                                    uiState.data.surname?.let {
                                        InfoItem(label = "姓", value = it)
                                    }
                                    uiState.data.userPrincipalName?.let {
                                        InfoItem(label = "用户主体名称", value = it)
                                    }
                                }
                            }
                        }
                        driveState.data.quota?.let { quota ->
                            item {
                                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "存储配额",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        quota.total?.let {
                                            InfoItem(label = "总空间", value = "$it")
                                        }
                                        quota.used?.let {
                                            InfoItem(label = "已使用", value = "$it")
                                        }
                                        quota.remaining?.let {
                                            InfoItem(label = "剩余", value = "$it")
                                        }
                                        quota.deleted?.let {
                                            InfoItem(label = "已删除", value = "$it")
                                        }
                                        quota.state?.let {
                                            InfoItem(label = "状态", value = it)
                                        }
                                        quota.storagePlanInformation?.upgradeAvailable?.let {
                                            InfoItem(label = "可升级", value = "$it")
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
private fun InfoItem(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(value) }
    )
}
