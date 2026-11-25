package com.qmdl.android.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayTrack(
  val title: String,
  val subtitle: String,
  val url: String,
  val coverUrl: String? = null,
)

data class PlayerUiState(
  val current: PlayTrack? = null,
  val playlist: List<PlayTrack> = emptyList(),
  val currentIndex: Int = -1,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val error: String? = null,
)

class AudioPlayerController(context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()
  @Volatile
  private var released = false

  private val _state = MutableStateFlow(PlayerUiState())
  val state: StateFlow<PlayerUiState> = _state.asStateFlow()

  init {
    exoPlayer.addListener(object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _state.update { it.copy(isPlaying = isPlaying, error = if (isPlaying) null else it.error) }
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        _state.update {
          it.copy(
            isBuffering = playbackState == Player.STATE_BUFFERING,
            durationMs = runCatching { exoPlayer.duration.coerceAtLeast(0L) }.getOrDefault(it.durationMs),
          )
        }
        if (playbackState == Player.STATE_ENDED && _state.value.playlist.size > 1) {
          next()
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        val friendly = when (error.errorCode) {
          PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
          PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
          -> "网络连接超时，音频加载失败"
          PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "音频链接已失效或无访问权限"
          PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
          PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
          -> "当前音频格式不受设备解码器支持"
          else -> error.localizedMessage?.takeIf { it.isNotBlank() } ?: "播放失败，请重试"
        }
        _state.update {
          it.copy(
            isPlaying = false,
            isBuffering = false,
            error = friendly,
          )
        }
      }
    })
    scope.launch {
      while (isActive) {
        if (!released && _state.value.current != null) {
          runCatching {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            val playing = exoPlayer.isPlaying
            _state.update {
              it.copy(
                positionMs = pos,
                durationMs = dur,
                isPlaying = playing,
              )
            }
          }
        }
        delay(350)
      }
    }
  }

  fun playQueue(tracks: List<PlayTrack>, startIndex: Int = 0) {
    if (tracks.isEmpty() || released) return
    val idx = startIndex.coerceIn(0, tracks.lastIndex)
    val track = tracks[idx]
    scope.launch {
      if (released) return@launch
      _state.value = PlayerUiState(
        current = track,
        playlist = tracks,
        currentIndex = idx,
        isBuffering = true,
        positionMs = 0L,
        durationMs = 0L,
        error = null,
      )
      runCatching {
        exoPlayer.setMediaItem(MediaItem.fromUri(track.url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
      }.onFailure { e ->
        _state.update {
          it.copy(
            isBuffering = false,
            isPlaying = false,
            error = e.message ?: "无法加载音频",
          )
        }
      }
    }
  }

  fun playSingle(track: PlayTrack) = playQueue(listOf(track), 0)

  fun togglePlayPause() {
    if (released) return
    scope.launch {
      if (released) return@launch
      runCatching {
        if (exoPlayer.isPlaying) {
          exoPlayer.pause()
        } else {
          if (_state.value.error != null) {
            exoPlayer.prepare()
          }
          exoPlayer.play()
        }
      }
    }
  }

  fun seekTo(ms: Long) {
    if (released) return
    val target = ms.coerceAtLeast(0L)
    scope.launch {
      if (released) return@launch
      runCatching {
        exoPlayer.seekTo(target)
        _state.update { it.copy(positionMs = target) }
      }
    }
  }

  fun next() {
    val st = _state.value
    if (st.playlist.isEmpty()) return
    playQueue(st.playlist, (st.currentIndex + 1) % st.playlist.size)
  }

  fun previous() {
    val st = _state.value
    if (st.playlist.isEmpty()) return
    val prev = if (st.currentIndex - 1 < 0) st.playlist.lastIndex else st.currentIndex - 1
    playQueue(st.playlist, prev)
  }

  fun stopAndDismiss() {
    if (released) return
    scope.launch {
      if (released) return@launch
      runCatching {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
      }
      _state.value = PlayerUiState()
    }
  }

  fun release() {
    if (released) return
    released = true
    scope.launch {
      runCatching { exoPlayer.release() }
      scope.cancel()
    }
  }
}
