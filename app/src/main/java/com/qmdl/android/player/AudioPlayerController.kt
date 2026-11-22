package com.qmdl.android.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

class AudioPlayerController(context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

  private val _state = MutableStateFlow(PlayerUiState())
  val state: StateFlow<PlayerUiState> = _state.asStateFlow()

  init {
    exoPlayer.addListener(object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _state.value = _state.value.copy(isPlaying = isPlaying)
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        _state.value = _state.value.copy(
          isBuffering = playbackState == Player.STATE_BUFFERING,
          durationMs = exoPlayer.duration.coerceAtLeast(0L),
        )
        if (playbackState == Player.STATE_ENDED && _state.value.playlist.size > 1) {
          next()
        }
      }
    })
    scope.launch {
      while (isActive) {
        if (_state.value.current != null) {
          _state.value = _state.value.copy(
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = exoPlayer.duration.coerceAtLeast(0L),
            isPlaying = exoPlayer.isPlaying,
          )
        }
        delay(350)
      }
    }
  }

  fun playQueue(tracks: List<PlayTrack>, startIndex: Int = 0) {
    if (tracks.isEmpty()) return
    val idx = startIndex.coerceIn(0, tracks.lastIndex)
    val track = tracks[idx]
    _state.value = _state.value.copy(
      current = track,
      playlist = tracks,
      currentIndex = idx,
      positionMs = 0L,
      durationMs = 0L,
    )
    exoPlayer.setMediaItem(MediaItem.fromUri(track.url))
    exoPlayer.prepare()
    exoPlayer.playWhenReady = true
  }

  fun playSingle(track: PlayTrack) = playQueue(listOf(track), 0)

  fun togglePlayPause() {
    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
  }

  fun seekTo(ms: Long) {
    exoPlayer.seekTo(ms.coerceAtLeast(0L))
    _state.value = _state.value.copy(positionMs = ms.coerceAtLeast(0L))
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
    exoPlayer.stop()
    exoPlayer.clearMediaItems()
    _state.value = PlayerUiState()
  }

  fun release() {
    exoPlayer.release()
  }
}
