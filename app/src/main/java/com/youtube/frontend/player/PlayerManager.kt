package com.youtube.frontend.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentVideoId = MutableStateFlow<String?>(null)
    val currentVideoId: StateFlow<String?> = _currentVideoId

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition

    fun getOrCreatePlayer(): ExoPlayer {
        if (_exoPlayer == null) {
            _exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                        }
                    }
                })
            }
        }
        return _exoPlayer!!
    }

    fun playVideo(videoId: String, streamUrl: String) {
        val player = getOrCreatePlayer()
        _currentVideoId.value = videoId
        val mediaItem = MediaItem.fromUri(streamUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun playHls(hlsUrl: String, videoId: String) {
        val player = getOrCreatePlayer()
        _currentVideoId.value = videoId
        val mediaItem = MediaItem.Builder()
            .setUri(hlsUrl)
            .setMimeType("application/x-mpegURL")
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun pause() {
        _exoPlayer?.pause()
    }

    fun resume() {
        _exoPlayer?.play()
    }

    fun seekTo(positionMs: Long) {
        _exoPlayer?.seekTo(positionMs)
    }

    fun release() {
        _exoPlayer?.release()
        _exoPlayer = null
    }

    fun stop() {
        _exoPlayer?.stop()
        _currentVideoId.value = null
    }
}
