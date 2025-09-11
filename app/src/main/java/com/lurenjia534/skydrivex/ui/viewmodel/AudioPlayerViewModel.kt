package com.lurenjia534.skydrivex.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi

@UnstableApi
@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val player: ExoPlayer = ExoPlayer.Builder(
        appContext,
        DefaultRenderersFactory(appContext).setExtensionRendererMode(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
        )
    ).build()

    fun player(): ExoPlayer = player

    fun setMedia(url: String, mimeType: String? = null) {
        val current = player.currentMediaItem
        val currentUri = current?.localConfiguration?.uri?.toString()
        if (currentUri != url) {
            val item = if (mimeType != null) {
                MediaItem.Builder().setUri(url).setMimeType(mimeType).build()
            } else MediaItem.fromUri(url)
            player.setMediaItem(item)
            player.prepare()
        }
        player.playWhenReady = true
    }

    override fun onCleared() {
        super.onCleared()
        try { player.release() } catch (_: Throwable) {}
    }
}
