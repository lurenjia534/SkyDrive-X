package com.lurenjia534.skydrivex.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import com.lurenjia534.skydrivex.ui.viewmodel.FilesViewModel
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import java.net.URLDecoder

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPreviewScreen(
    itemId: String?,
    nameEncoded: String?,
    onBack: () -> Unit,
    filesViewModel: FilesViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val token by mainViewModel.token.collectAsState()
    val title = remember(nameEncoded) { runCatching { URLDecoder.decode(nameEncoded ?: "", "UTF-8") }.getOrDefault("") }

    var url by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(itemId, token) {
        if (!itemId.isNullOrEmpty() && !token.isNullOrEmpty()) {
            isLoading = true
            runCatching { filesViewModel.getDownloadUrl(itemId, token!!) }
                .onSuccess { url = it ?: run { snackbarHostState.showSnackbar("无法获取视频地址"); null } }
                .onFailure { snackbarHostState.showSnackbar(it.message ?: "加载失败") }
            isLoading = false
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = title.ifEmpty { "视频预览" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val src = url
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (src.isNullOrEmpty()) {
                Text(text = if (isLoading) "加载中…" else "无法播放")
            } else {
                val context = LocalContext.current
                // 只创建一次播放器，离开页面时释放
                val player = remember(context) { ExoPlayer.Builder(context).build() }

                DisposableEffect(Unit) {
                    onDispose { player.release() }
                }

                // 切换媒体源
                LaunchedEffect(src) {
                    player.setMediaItem(MediaItem.fromUri(src))
                    player.prepare()
                    player.playWhenReady = true
                }

                // 视频画面（Compose 原生）
                PlayerSurface(player = player, modifier = Modifier.fillMaxSize())

                // 底部播放/暂停 FAB（示例）
                val playPause = rememberPlayPauseButtonState(player)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    FloatingActionButton(onClick = playPause::onClick, modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = if (playPause.showPlay) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (playPause.showPlay) "播放" else "暂停"
                        )
                    }
                }
            }
        }
    }
}
