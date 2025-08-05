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
import com.lurenjia534.skydrivex.viewmodel.UserUiState

@Composable
fun ProfileScreen(
    uiState: UserUiState,
    onRefresh: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (uiState.data == null && uiState.error == null && !uiState.isLoading) {
            onRefresh()
        }
    }

    LaunchedEffect(uiState.error, uiState.data) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
        if (uiState.error == null && uiState.data != null) {
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
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }

                uiState.error != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = uiState.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text(text = "重试")
                        }
                    }
                }

                uiState.data != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        uiState.data.displayName?.let {
                            Text(text = it, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        uiState.data.mail?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = it)
                        }
                        uiState.data.jobTitle?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = it)
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
