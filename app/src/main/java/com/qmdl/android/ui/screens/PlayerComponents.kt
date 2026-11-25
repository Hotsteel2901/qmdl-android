package com.qmdl.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qmdl.android.data.formatMillis
import com.qmdl.android.player.PlayerUiState
import com.qmdl.android.ui.liquid.LiquidGlassButton
import com.qmdl.android.ui.liquid.LiquidGlassCard
import com.qmdl.android.ui.liquid.LiquidGlassProgressBar
import com.qmdl.android.ui.liquid.LiquidGlassSpinner
import com.qmdl.android.ui.liquid.LocalLiquidDark

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
  val isDark = LocalLiquidDark.current
  val dur = playerState.durationMs.coerceAtLeast(0L)
  val progress = if (dur > 0L) (playerState.positionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else null

  LiquidGlassCard(
    modifier = modifier.fillMaxWidth(),
    cornerRadius = 24.dp,
    blurRadius = 14.dp,
    refractionHeight = 14.dp,
    refractionAmount = 22.dp,
    onClick = onExpand,
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(13.dp))
          .background(
            Brush.linearGradient(
              listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f),
              ),
            ),
          )
          .border(
            0.75.dp,
            Color.White.copy(alpha = if (isDark) 0.2f else 0.7f),
            RoundedCornerShape(13.dp),
          ),
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
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (!playerState.error.isNullOrBlank()) {
          Text(
            text = playerState.error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        } else {
          val sub = if (playerState.isBuffering) "正在缓冲音频…" else track.subtitle
          Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (playerState.isBuffering) {
        LiquidGlassSpinner(size = 28.dp)
      } else {
        LiquidGlassButton(
          onClick = onTogglePlay,
          tint = MaterialTheme.colorScheme.primary,
          height = 38.dp,
          horizontalPadding = 10.dp,
        ) {
          Icon(
            if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = "播放/暂停",
            tint = Color.White,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      if (playerState.playlist.size > 1) {
        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
          Icon(Icons.Rounded.SkipNext, contentDescription = "下一首")
        }
      }
      IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
        Icon(Icons.Rounded.Close, contentDescription = "关闭")
      }
    }

    if (progress != null || playerState.isBuffering) {
      Spacer(Modifier.height(8.dp))
      LiquidGlassProgressBar(
        progress = if (playerState.isBuffering && progress == null) null else progress,
        modifier = Modifier.fillMaxWidth(),
      )
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
  val isDark = LocalLiquidDark.current
  val sheetBg = if (isDark) Color(0xFF0E1A22) else Color(0xFFF3FAF7)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = sheetBg,
    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Luminous Glass Cover Art Frame
      Box(
        modifier = Modifier
          .size(236.dp)
          .clip(RoundedCornerShape(32.dp))
          .background(
            Brush.linearGradient(
              listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
              ),
            ),
          )
          .border(
            1.dp,
            Color.White.copy(alpha = if (isDark) 0.20f else 0.75f),
            RoundedCornerShape(32.dp),
          ),
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
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
          )
        }
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = track.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (playerState.playlist.size > 1) {
          Text(
            text = "播放列表第 ${playerState.currentIndex + 1} / ${playerState.playlist.size} 首",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      if (!playerState.error.isNullOrBlank()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = playerState.error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }

      val dur = playerState.durationMs.coerceAtLeast(1L)
      val pos = playerState.positionMs.coerceIn(0L, dur)
      Column(Modifier.fillMaxWidth()) {
        Slider(
          value = pos.toFloat() / dur.toFloat(),
          onValueChange = { frac -> onSeek((frac * dur).toLong()) },
          colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
          ),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            formatMillis(pos),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            formatMillis(playerState.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onPrev,
          enabled = playerState.playlist.size > 1,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        ) {
          Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(28.dp))
        }

        LiquidGlassButton(
          onClick = onTogglePlay,
          tint = MaterialTheme.colorScheme.primary,
          height = 60.dp,
          horizontalPadding = 28.dp,
        ) {
          Icon(
            if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = "播放/暂停",
            tint = Color.White,
            modifier = Modifier.size(32.dp),
          )
        }

        IconButton(
          onClick = onNext,
          enabled = playerState.playlist.size > 1,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        ) {
          Icon(Icons.Rounded.SkipNext, contentDescription = "下一首", modifier = Modifier.size(28.dp))
        }
      }

      Spacer(Modifier.height(22.dp))
    }
  }
}
