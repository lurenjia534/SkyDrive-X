package com.lurenjia534.skydrivex.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@UnstableApi
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val appContext = context.applicationContext
    private val player: ExoPlayer = ExoPlayer.Builder(
        appContext,
        DefaultRenderersFactory(appContext).setExtensionRendererMode(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
        )
    ).build()

    private val _aspectRatio = MutableStateFlow(16f / 9f)
    val aspectRatio: StateFlow<Float> = _aspectRatio

    init {
        // seed from current size
        _aspectRatio.value = ratioOf(player.videoSize)
        player.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _aspectRatio.value = ratioOf(videoSize)
            }
        })
    }

    fun getPlayer(): ExoPlayer = player

    fun setMedia(url: String) {
        val current = player.currentMediaItem
        val currentUri = current?.localConfiguration?.uri?.toString()
        if (currentUri != url) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }
        player.playWhenReady = true
    }

    override fun onCleared() {
        super.onCleared()
        try { player.release() } catch (_: Throwable) {}
    }

    private fun ratioOf(v: VideoSize): Float {
        val h = if (v.height == 0) 1 else v.height
        val px = if (v.pixelWidthHeightRatio == 0f) 1f else v.pixelWidthHeightRatio
        return ((v.width * px) / h).coerceAtLeast(0.1f)
    }
}
