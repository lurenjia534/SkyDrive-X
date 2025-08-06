package com.lurenjia534.skydrivex.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        uiState.data.id.let {
                            Text(text = "ID: $it", fontSize = 16.sp)
                        }
                        uiState.data.businessPhones?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "商务电话: ${it.joinToString(", ")}")
                        }
                        uiState.data.displayName?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = it, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        uiState.data.givenName?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "名: $it")
                        }
                        uiState.data.jobTitle?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "职位: $it")
                        }
                        uiState.data.mail?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "邮箱: $it")
                        }
                        uiState.data.mobilePhone?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "手机: $it")
                        }
                        uiState.data.officeLocation?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "办公地点: $it")
                        }
                        uiState.data.preferredLanguage?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "首选语言: $it")
                        }
                        uiState.data.surname?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "姓: $it")
                        }
                        uiState.data.userPrincipalName?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "用户主体名称: $it")
                        }

                        driveState.data.quota?.let { quota ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "存储配额",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            quota.total?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "总空间: $it")
                            }
                            quota.used?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "已使用: $it")
                            }
                            quota.remaining?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "剩余: $it")
                            }
                            quota.deleted?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "已删除: $it")
                            }
                            quota.state?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "状态: $it")
                            }
                            quota.storagePlanInformation?.upgradeAvailable?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "可升级: $it")
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
