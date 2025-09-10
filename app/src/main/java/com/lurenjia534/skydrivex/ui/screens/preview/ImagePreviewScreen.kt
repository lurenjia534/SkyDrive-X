package com.lurenjia534.skydrivex.ui.screens.preview

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.SubcomposeAsyncImage
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.lurenjia534.skydrivex.ui.viewmodel.FilesViewModel
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImagePreviewScreen(
    itemId: String?,
    nameEncoded: String?,
    onBack: () -> Unit,
    filesViewModel: FilesViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val token by mainViewModel.token.collectAsState()
    val name = remember(nameEncoded) { runCatching { URLDecoder.decode(nameEncoded ?: "", "UTF-8") }.getOrDefault("") }

    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(itemId, token) {
        if (!itemId.isNullOrEmpty() && !token.isNullOrEmpty()) {
            isLoading = true
            runCatching {
                filesViewModel.getDownloadUrl(itemId, token!!)
            }.onSuccess {
                imageUrl = it
                if (it == null) snackbarHostState.showSnackbar("无法获取图片地址")
            }.onFailure {
                snackbarHostState.showSnackbar(it.message ?: "加载失败")
            }
            isLoading = false
        }
    }

    // 缩放/位移状态
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        // 缩放后允许拖拽
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f; offsetY = 0f
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = name.ifEmpty { "图片预览" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                imageUrl.isNullOrEmpty() -> {
                    if (isLoading) {
                        LoadingIndicator()
                    } else {
                        Text(text = "无法加载图片")
                    }
                }
                else -> {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Fit,
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LoadingIndicator()
                            }
                        },
                        error = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("加载失败")
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .transformable(transformableState)
                            .pointerInput(scale) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        // 双击重置/放大
                                        if (scale > 1f) {
                                            scale = 1f; offsetX = 0f; offsetY = 0f
                                        } else {
                                            scale = 2f
                                        }
                                    }
                                )
                            }
                    )
                }
            }
        }
    }
}
