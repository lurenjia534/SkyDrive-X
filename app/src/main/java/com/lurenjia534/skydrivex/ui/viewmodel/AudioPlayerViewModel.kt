package com.lurenjia534.skydrivex.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    data class AudioMeta(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val artwork: ByteArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AudioMeta

            if (title != other.title) return false
            if (artist != other.artist) return false
            if (album != other.album) return false
            if (!artwork.contentEquals(other.artwork)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = title?.hashCode() ?: 0
            result = 31 * result + (artist?.hashCode() ?: 0)
            result = 31 * result + (album?.hashCode() ?: 0)
            result = 31 * result + (artwork?.contentHashCode() ?: 0)
            return result
        }
    }

    private val _meta = MutableStateFlow(AudioMeta())
    val meta: StateFlow<AudioMeta> = _meta

    private val _decoderName = MutableStateFlow<String?>(null)
    val decoderName: StateFlow<String?> = _decoderName

    fun player(): ExoPlayer = player

    init {
        // 记录实际使用的解码器，便于排查
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                Log.i("AudioPlayerVM", "Audio decoder: $decoderName ffmpegAvailable=${FfmpegLibrary.isAvailable()}")
                _decoderName.value = decoderName
            }
        })
        // 提取媒体元数据（标题/歌手/专辑/封面）
        player.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                _meta.value = AudioMeta(
                    title = mediaMetadata.title?.toString(),
                    artist = mediaMetadata.artist?.toString(),
                    album = mediaMetadata.albumTitle?.toString(),
                    artwork = mediaMetadata.artworkData
                )
            }
        })
    }

    fun setMedia(url: String, mimeType: String? = null) {
        val current = player.currentMediaItem
        val currentUri = current?.localConfiguration?.uri?.toString()
        if (currentUri != url) {
            val item = if (mimeType != null) {
                MediaItem.Builder().setUri(url).setMimeType(mimeType).build()
            } else MediaItem.fromUri(url)
            player.setMediaItem(item)
            player.prepare()
            _meta.value = AudioMeta() // 重置，等待解析出的新元数据
        }
        player.playWhenReady = true
    }

    override fun onCleared() {
        super.onCleared()
        try { player.release() } catch (_: Throwable) {}
    }
}
