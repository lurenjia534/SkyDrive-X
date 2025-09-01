package com.lurenjia534.skydrivex.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import com.lurenjia534.skydrivex.ui.viewmodel.FilesViewModel
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var controlsVisible by remember(isLandscape) { mutableStateOf(!isLandscape) }
    Scaffold(
        topBar = {
            if (!isLandscape && controlsVisible) {
                CenterAlignedTopAppBar(
                    title = { Text(text = title.ifEmpty { "视频预览" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val src = url
        val view = androidx.compose.ui.platform.LocalView.current
        val ctx = LocalContext.current
        fun findActivity(c0: android.content.Context): android.app.Activity? {
            var c = c0
            while (c is android.content.ContextWrapper) {
                if (c is android.app.Activity) return c
                c = c.baseContext
            }
            return null
        }
        // 横屏时隐藏系统栏，竖屏时显示（使用 WindowCompat 建议 API）
        LaunchedEffect(isLandscape) {
            val act = findActivity(ctx)
            if (act != null) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(act.window, view)
                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (isLandscape) controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                else controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(isLandscape) { detectTapGestures(onTap = { controlsVisible = !controlsVisible }) },
            contentAlignment = Alignment.Center
        ) {
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

                // 按视频尺寸保持宽高比显示（避免被拉伸）
                var aspect by remember { mutableFloatStateOf(16f / 9f) }
                fun ratioOf(v: VideoSize): Float {
                    val h = if (v.height == 0) 1 else v.height
                    val px = if (v.pixelWidthHeightRatio == 0f) 1f else v.pixelWidthHeightRatio
                    return ((v.width * px) / h).coerceAtLeast(0.1f)
                }
                // 监听视频尺寸变化
                LaunchedEffect(player) {
                    aspect = ratioOf(player.videoSize)
                }
                DisposableEffect(player) {
                    val l = object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                            aspect = ratioOf(videoSize)
                        }
                    }
                    player.addListener(l)
                    onDispose { player.removeListener(l) }
                }

                val videoModifier = if (androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    Modifier.fillMaxHeight().aspectRatio(aspect, matchHeightConstraintsFirst = true)
                } else {
                    Modifier.fillMaxWidth().aspectRatio(aspect)
                }
                PlayerSurface(player = player, modifier = videoModifier.align(Alignment.Center))

                // 自动隐藏控制（简单实现）：显示后3秒自动隐藏（横屏默认隐藏）
                LaunchedEffect(controlsVisible, src) {
                    if (controlsVisible) {
                        delay(3000)
                        controlsVisible = false
                    }
                }

                // 播放/暂停 FAB（右下角）
                if (controlsVisible) {
                    val playPause = rememberPlayPauseButtonState(player)
                    FloatingActionButton(
                        onClick = playPause::onClick,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        Icon(
                            imageVector = if (playPause.showPlay) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (playPause.showPlay) "播放" else "暂停"
                        )
                    }
                }

                // 控制栏（进度/音量/倍速）
                if (controlsVisible) {
                    ControlBar(
                        player = player,
                        compact = isLandscape,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = if (isLandscape) 24.dp else 80.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlBar(player: Player, compact: Boolean, modifier: Modifier = Modifier) {
    // 进度状态（每 500ms 刷新一次）
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var scrubPosMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(player) {
        while (true) {
            val d = player.duration
            durationMs = if (d != C.TIME_UNSET) d else 0L
            positionMs = player.currentPosition.coerceAtLeast(0L)
            delay(500)
        }
    }

    // 音量与静音
    var volume by remember { mutableFloatStateOf(player.volume) }
    var lastNonZeroVolume by remember { mutableFloatStateOf(if (player.volume > 0f) player.volume else 1f) }
    fun setVol(v: Float) {
        val nv = v.coerceIn(0f, 1f)
        volume = nv
        if (nv > 0f) lastNonZeroVolume = nv
        player.volume = nv
    }

    // 倍速（循环 0.5/1/1.5/2）
    val speeds = listOf(0.5f, 1f, 1.5f, 2f)
    var speedIndex by remember { mutableIntStateOf(1) }

    Surface(
        modifier = modifier,
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = if (compact) 0.6f else 0.88f),
        tonalElevation = if (compact) 2.dp else 6.dp,
        shape = RoundedCornerShape(if (compact) 10.dp else 16.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
        ) {
            // 进度 + 快退/快进
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }
                ) { Icon(Icons.Filled.FastRewind, contentDescription = "后退10秒") }

                val sliderValue = (scrubPosMs ?: positionMs).toFloat()
                Slider(
                    value = sliderValue,
                    onValueChange = { scrubPosMs = it.toLong() },
                    onValueChangeFinished = {
                        scrubPosMs?.let { player.seekTo(it) }
                        scrubPosMs = null
                    },
                    valueRange = 0f..(durationMs.takeIf { it > 0 }?.toFloat() ?: 1f),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        val target = (player.currentPosition + 10_000)
                            .let { if (durationMs > 0) it.coerceAtMost(durationMs) else it }
                        player.seekTo(target)
                    }
                ) { Icon(Icons.Filled.FastForward, contentDescription = "前进10秒") }
            }

            // 音量与倍速
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = {
                    if (volume == 0f) setVol(lastNonZeroVolume) else setVol(0f)
                }) {
                    Icon(
                        if (volume == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (volume == 0f) "取消静音" else "静音"
                    )
                }
                Slider(
                    value = volume,
                    onValueChange = ::setVol,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    speedIndex = (speedIndex + 1) % speeds.size
                    player.playbackParameters = PlaybackParameters(speeds[speedIndex])
                }) {
                    Text("${speeds[speedIndex]}x")
                }
            }
        }
    }
}
