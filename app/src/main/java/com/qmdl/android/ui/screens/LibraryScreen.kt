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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qmdl.android.data.LibraryFile
import com.qmdl.android.data.formatBytes
import com.qmdl.android.ui.liquid.LiquidGlassButton
import com.qmdl.android.ui.liquid.LiquidGlassCard
import com.qmdl.android.ui.liquid.LiquidGlassChip
import com.qmdl.android.ui.liquid.LiquidGlassSpinner
import com.qmdl.android.ui.liquid.LiquidGlassTextField
import com.qmdl.android.ui.liquid.LocalLiquidDark
import com.qmdl.android.viewmodel.MainUiState

@Composable
fun LibraryScreen(
  state: MainUiState,
  onRefresh: () -> Unit,
  onPlayAt: (List<LibraryFile>, Int) -> Unit,
  onExport: (LibraryFile) -> Unit,
  onDelete: (LibraryFile) -> Unit,
) {
  var filter by remember { mutableStateOf("") }
  var formatFilter by remember { mutableStateOf("all") }
  var pendingDelete by remember { mutableStateOf<LibraryFile?>(null) }
  val isDark = LocalLiquidDark.current

  val flacCount = remember(state.library) { state.library.count { it.ext.equals("flac", true) } }
  val mp3Count = remember(state.library) { state.library.count { !it.ext.equals("flac", true) } }

  val filtered = remember(state.library, filter, formatFilter) {
    val q = filter.trim().lowercase()
    state.library.filter { f ->
      val fmtOk = when (formatFilter) {
        "flac" -> f.ext.equals("flac", true)
        "mp3" -> !f.ext.equals("flac", true)
        else -> true
      }
      val qOk = q.isEmpty() ||
        f.title.lowercase().contains(q) ||
        f.artist.lowercase().contains(q) ||
        f.name.lowercase().contains(q)
      fmtOk && qOk
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 180.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("本地曲库", style = MaterialTheme.typography.headlineMedium)
          Text(
            "${state.library.size} 首音频 · 已占用 ${formatBytes(state.libraryTotalSize)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LiquidGlassButton(
            onClick = onRefresh,
            height = 40.dp,
            horizontalPadding = 10.dp,
          ) {
            if (state.libraryLoading) {
              LiquidGlassSpinner(size = 18.dp)
            } else {
              Icon(Icons.Rounded.Refresh, contentDescription = "刷新", modifier = Modifier.size(19.dp))
            }
          }
          LiquidGlassButton(
            onClick = { onPlayAt(filtered, 0) },
            enabled = filtered.isNotEmpty(),
            tint = MaterialTheme.colorScheme.primary,
            height = 40.dp,
            horizontalPadding = 14.dp,
          ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text("连播", color = Color.White, style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }

    item {
      LiquidGlassTextField(
        value = filter,
        onValueChange = { filter = it },
        placeholder = "筛选已下载歌曲、歌手或文件名…",
        modifier = Modifier.fillMaxWidth(),
        leading = {
          Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
        },
        trailing = if (filter.isNotEmpty()) {
          {
            IconButton(onClick = { filter = "" }, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Rounded.Clear, contentDescription = "清空", modifier = Modifier.size(18.dp))
            }
          }
        } else null,
      )
    }

    if (state.library.isNotEmpty()) {
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            LiquidGlassChip(
              label = "全部 (${state.library.size})",
              selected = formatFilter == "all",
              onClick = { formatFilter = "all" },
            )
          }
          item {
            LiquidGlassChip(
              label = "FLAC 无损 ($flacCount)",
              selected = formatFilter == "flac",
              onClick = { formatFilter = "flac" },
            )
          }
          item {
            LiquidGlassChip(
              label = "MP3 / 其他 ($mp3Count)",
              selected = formatFilter == "mp3",
              onClick = { formatFilter = "mp3" },
            )
          }
        }
      }
    }

    if (filtered.isEmpty()) {
      item {
        LiquidGlassCard(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 28.dp,
          contentPadding = PaddingValues(vertical = 40.dp, horizontal = 22.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Rounded.LibraryMusic,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
              modifier = Modifier.size(44.dp),
            )
            Text(
              if (state.library.isEmpty()) "曲库暂时空空如也" else "没有符合筛选条件的音频",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              "下载完成后的歌曲会自动保存在这里，支持直接连播或流式导出到系统 Music/QMDL 文件夹",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      // Collision-proof composite key prevents LazyColumn crash if duplicate filenames ever appear
      itemsIndexed(filtered, key = { idx, f -> "${f.name}_$idx" }) { idx, file ->
        val isFlac = file.ext.equals("flac", true)
        LiquidGlassCard(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 22.dp,
          onClick = { onPlayAt(filtered, idx) },
          contentPadding = PaddingValues(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                  if (isFlac) {
                    Brush.linearGradient(
                      listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = if (isDark) 0.45f else 0.25f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.35f else 0.18f),
                      ),
                    )
                  } else {
                    Brush.linearGradient(
                      listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.42f else 0.22f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.32f else 0.16f),
                      ),
                    )
                  },
                )
                .border(
                  0.75.dp,
                  (if (isFlac) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    .copy(alpha = 0.55f),
                  RoundedCornerShape(15.dp),
                ),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = file.ext.uppercase().ifBlank { "MP3" },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isFlac) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
              )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = file.title,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false),
                )
                if (file.isPreview) {
                  Text(
                    text = "试听",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f))
                      .padding(horizontal = 5.dp, vertical = 1.dp),
                  )
                }
              }
              Text(
                text = "${file.artist.ifBlank { "未知歌手" }} · ${formatBytes(file.size)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            LiquidGlassButton(
              onClick = { onPlayAt(filtered, idx) },
              tint = MaterialTheme.colorScheme.primary,
              height = 36.dp,
              horizontalPadding = 10.dp,
            ) {
              Icon(Icons.Rounded.PlayArrow, contentDescription = "播放", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { onExport(file) }, modifier = Modifier.size(36.dp)) {
              Icon(Icons.Rounded.FileUpload, contentDescription = "导出到系统音乐")
            }
            IconButton(onClick = { pendingDelete = file }, modifier = Modifier.size(36.dp)) {
              Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }
    }
  }

  pendingDelete?.let { file ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("删除本地文件") },
      text = { Text("确定要永久删除「${file.title}」吗？此操作无法撤销。") },
      confirmButton = {
        TextButton(
          onClick = {
            onDelete(file)
            pendingDelete = null
          },
        ) { Text("确认删除", color = MaterialTheme.colorScheme.error) }
      },
      dismissButton = {
        TextButton(onClick = { pendingDelete = null }) { Text("取消") }
      },
    )
  }
}
