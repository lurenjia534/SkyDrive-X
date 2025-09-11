package com.lurenjia534.skydrivex.ui.screens.preview

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import com.lurenjia534.skydrivex.ui.viewmodel.AudioPlayerViewModel
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.lurenjia534.skydrivex.ui.viewmodel.FilesViewModel
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.net.URLDecoder

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPreviewScreen(
    itemId: String?,
    nameEncoded: String?,
    onBack: () -> Unit,
    filesViewModel: FilesViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
    audioVM: AudioPlayerViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val token by mainViewModel.token.collectAsState()
    val title = remember(nameEncoded) { runCatching { URLDecoder.decode(nameEncoded ?: "", "UTF-8") }.getOrDefault("") }

    var url by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(itemId, token) {
        if (!itemId.isNullOrEmpty() && !token.isNullOrEmpty()) {
            runCatching { filesViewModel.getDownloadUrl(itemId, token!!) }
                .onSuccess { url = it ?: run { snackbarHostState.showSnackbar("无法获取音频地址"); null } }
                .onFailure { snackbarHostState.showSnackbar(it.message ?: "加载失败") }
        }
    }

    val player = audioVM.player()
    LaunchedEffect(url) {
        url?.let {
            val lower = (title).lowercase()
            val mime = when {
                lower.endsWith(".flac") -> MimeTypes.AUDIO_FLAC
                lower.endsWith(".m4a") -> MimeTypes.AUDIO_AAC
                lower.endsWith(".wav") -> MimeTypes.AUDIO_WAV
                lower.endsWith(".ogg") -> MimeTypes.AUDIO_OGG
                lower.endsWith(".opus") -> MimeTypes.AUDIO_OPUS
                else -> null
            }
            audioVM.setMedia(it, mime)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = title.ifEmpty { "音频预览" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 简单的标题/占位
            Text(text = title.ifEmpty { "音频" }, style = MaterialTheme.typography.titleLarge)
            if (url == null) {
                CircularProgressIndicator()
                return@Column
            }

            // 进度与控制
            AudioControls(player = player)
        }
    }
}

@Composable
private fun AudioControls(player: androidx.media3.exoplayer.ExoPlayer) {
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var scrubPosMs by remember { mutableStateOf<Long?>(null) }
    val speeds = listOf(0.5f, 1f, 1.25f, 1.5f, 2f)
    var speedIndex by remember { mutableIntStateOf(1) }

    LaunchedEffect(player) {
        while (true) {
            val d = player.duration
            durationMs = if (d != C.TIME_UNSET) d else 0L
            positionMs = player.currentPosition.coerceAtLeast(0L)
            delay(500)
        }
    }

    // 播放/暂停行
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }) {
            Icon(Icons.Filled.FastRewind, contentDescription = "后退10秒")
        }
        // 用 playWhenReady + playbackState 判断
        val playing = player.playWhenReady && player.playbackState == androidx.media3.common.Player.STATE_READY
        FilledIconButton(onClick = { player.playWhenReady = !playing }) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (playing) "暂停" else "播放")
        }
        IconButton(onClick = {
            val target = (player.currentPosition + 10_000)
                .let { if (durationMs > 0) it.coerceAtMost(durationMs) else it }
            player.seekTo(target)
        }) {
            Icon(Icons.Filled.FastForward, contentDescription = "前进10秒")
        }
    }

    // 进度条
    Slider(
        value = (scrubPosMs ?: positionMs).toFloat(),
        onValueChange = { scrubPosMs = it.toLong() },
        onValueChangeFinished = {
            scrubPosMs?.let { player.seekTo(it) }
            scrubPosMs = null
        },
        valueRange = 0f..(durationMs.takeIf { it > 0 }?.toFloat() ?: 1f),
        modifier = Modifier.fillMaxWidth()
    )

    // 倍速
    TextButton(onClick = {
        speedIndex = (speedIndex + 1) % speeds.size
        player.playbackParameters = PlaybackParameters(speeds[speedIndex])
    }) { Text(text = "倍速 ${speeds[speedIndex]}x") }
}
