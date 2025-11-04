package com.lurenjia534.skydrivex.ui.screens.preview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.SubcomposeAsyncImage
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import com.lurenjia534.skydrivex.ui.viewmodel.preview.ImagePreviewViewModel
import java.net.URLDecoder
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewScreen(
    itemId: String?,
    nameEncoded: String?,
    imageIds: List<String> = emptyList(),
    imageNames: List<String> = emptyList(),
    initialIndex: Int = 0,
    onBack: () -> Unit,
    previewViewModel: ImagePreviewViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val token by mainViewModel.token.collectAsState()
    val name = remember(nameEncoded) { runCatching { URLDecoder.decode(nameEncoded ?: "", "UTF-8") }.getOrDefault("") }

    val imageUrlMap by previewViewModel.imageUrls.collectAsState()
    val loadingIds by previewViewModel.loadingIds.collectAsState()
    val errorMap by previewViewModel.errors.collectAsState()

    val sanitizedImageIds = remember(imageIds) { imageIds.filter { it.isNotBlank() } }
    val sanitizedNames = remember(sanitizedImageIds, imageNames) {
        if (imageNames.size == sanitizedImageIds.size) imageNames else sanitizedImageIds.map { it }
    }
    val hasPager = sanitizedImageIds.isNotEmpty()
    val pagerInitialIndex = remember(sanitizedImageIds) {
        if (sanitizedImageIds.isEmpty()) 0 else initialIndex.coerceIn(0, sanitizedImageIds.lastIndex)
    }
    val pagerState = if (hasPager) rememberPagerState(initialPage = pagerInitialIndex, pageCount = { sanitizedImageIds.size }) else null
    val currentPage by remember { derivedStateOf { pagerState?.currentPage ?: 0 } }
    val currentImageId = if (hasPager) sanitizedImageIds.getOrNull(currentPage) else itemId
    val currentName = when {
        hasPager -> sanitizedNames.getOrNull(currentPage) ?: name
        else -> name
    }

    LaunchedEffect(currentImageId, token) {
        val t = token
        if (!currentImageId.isNullOrEmpty() && !t.isNullOrEmpty()) {
            previewViewModel.ensureLoaded(currentImageId, t!!)
        }
    }

    LaunchedEffect(currentImageId, errorMap) {
        val msg = currentImageId?.let { errorMap[it] }
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            previewViewModel.clearError(currentImageId)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentName.ifEmpty { "图片预览" },
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (hasPager && pagerState != null) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val id = sanitizedImageIds[page]
                    val displayName = sanitizedNames.getOrNull(page) ?: name
                    val url = imageUrlMap[id]
                    val loading = loadingIds.contains(id)
                    val errorMessage = errorMap[id]
                    ZoomableImage(
                        imageUrl = url,
                        isLoading = loading,
                        errorMessage = errorMessage,
                        name = displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                val id = itemId
                val url = id?.let { imageUrlMap[it] }
                val loading = id?.let { loadingIds.contains(it) } == true
                val errorMessage = id?.let { errorMap[it] }
                ZoomableImage(
                    imageUrl = url,
                    isLoading = loading,
                    errorMessage = errorMessage,
                    name = name,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ZoomableImage(
    imageUrl: String?,
    isLoading: Boolean,
    errorMessage: String?,
    name: String,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var pointerCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(imageUrl) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
    val allowTransform by remember { derivedStateOf { pointerCount > 1 || scale > 1.02f } }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        if (allowTransform || zoomChange != 1f) {
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            val isZooming = newScale > 1f
            scale = newScale
            if (isZooming) {
                offsetX += panChange.x
                offsetY += panChange.y
            } else {
                offsetX = 0f
                offsetY = 0f
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            isLoading -> LoadingIndicator()
            !errorMessage.isNullOrBlank() -> Text(text = errorMessage)
            imageUrl.isNullOrEmpty() -> Text(text = "无法加载图片")
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
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                pointerCount = 0
                                awaitFirstDown(requireUnconsumed = false)
                                pointerCount = 1
                                do {
                                    val event = awaitPointerEvent()
                                    pointerCount = event.changes.count { it.pressed }
                                } while (event.changes.any { it.pressed })
                                pointerCount = 0
                            }
                        }
                        .then(if (allowTransform) Modifier.transformable(transformableState) else Modifier)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(scale) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
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
