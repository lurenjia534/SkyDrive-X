package com.lurenjia534.skydrivex.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val player: ExoPlayer = ExoPlayer.Builder(appContext).build()

    fun player(): ExoPlayer = player

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
}

