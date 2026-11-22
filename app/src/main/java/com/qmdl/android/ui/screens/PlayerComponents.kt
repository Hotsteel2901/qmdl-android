package com.qmdl.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qmdl.android.data.formatMillis
import com.qmdl.android.player.PlayerUiState

@Composable
fun MiniPlayerDock(
  playerState: PlayerUiState,
  onExpand: () -> Unit,
  onTogglePlay: () -> Unit,
  onNext: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val track = playerState.current ?: return
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(22.dp))
      .clickable(onClick = onExpand),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    tonalElevation = 6.dp,
    shadowElevation = 8.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        if (!track.coverUrl.isNullOrBlank()) {
          AsyncImage(
            model = track.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          track.title,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          track.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      FilledIconButton(onClick = onTogglePlay, shape = CircleShape) {
        Icon(
          if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
          contentDescription = "播放/暂停",
        )
      }
      if (playerState.playlist.size > 1) {
        IconButton(onClick = onNext) {
          Icon(Icons.Rounded.SkipNext, contentDescription = "下一首")
        }
      }
      IconButton(onClick = onClose) {
        Icon(Icons.Rounded.Close, contentDescription = "关闭")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayerSheet(
  playerState: PlayerUiState,
  onDismiss: () -> Unit,
  onTogglePlay: () -> Unit,
  onPrev: () -> Unit,
  onNext: () -> Unit,
  onSeek: (Long) -> Unit,
) {
  val track = playerState.current ?: return
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier
          .size(220.dp)
          .clip(RoundedCornerShape(32.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        if (!track.coverUrl.isNullOrBlank()) {
          AsyncImage(
            model = track.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            Icons.Rounded.MusicNote,
            null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(64.dp),
          )
        }
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(track.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          track.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      val dur = playerState.durationMs.coerceAtLeast(1L)
      val pos = playerState.positionMs.coerceIn(0L, dur)
      Column(Modifier.fillMaxWidth()) {
        Slider(
          value = pos.toFloat() / dur.toFloat(),
          onValueChange = { frac -> onSeek((frac * dur).toLong()) },
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(formatMillis(pos), style = MaterialTheme.typography.labelSmall)
          Text(formatMillis(playerState.durationMs), style = MaterialTheme.typography.labelSmall)
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onPrev, enabled = playerState.playlist.size > 1) {
          Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(32.dp))
        }
        FilledIconButton(
          onClick = onTogglePlay,
          modifier = Modifier.size(64.dp),
          shape = RoundedCornerShape(22.dp),
        ) {
          Icon(
            if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = "播放/暂停",
            modifier = Modifier.size(34.dp),
          )
        }
        IconButton(onClick = onNext, enabled = playerState.playlist.size > 1) {
          Icon(Icons.Rounded.SkipNext, contentDescription = "下一首", modifier = Modifier.size(32.dp))
        }
      }

      Spacer(Modifier.height(24.dp))
    }
  }
}
