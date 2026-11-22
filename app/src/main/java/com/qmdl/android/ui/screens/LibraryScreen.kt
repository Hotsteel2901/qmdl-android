package com.qmdl.android.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qmdl.android.data.LibraryFile
import com.qmdl.android.data.formatBytes
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
  var pendingDelete by remember { mutableStateOf<LibraryFile?>(null) }

  val filtered = remember(state.library, filter) {
    val q = filter.trim().lowercase()
    if (q.isEmpty()) state.library
    else state.library.filter {
      it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.name.lowercase().contains(q)
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("本地曲库", style = MaterialTheme.typography.headlineMedium)
          Text(
            "${state.library.size} 首 · 共 ${formatBytes(state.libraryTotalSize)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          IconButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
          }
          Button(
            onClick = { onPlayAt(filtered, 0) },
            enabled = filtered.isNotEmpty(),
            shape = CircleShape,
          ) {
            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("连播")
          }
        }
      }
    }

    item {
      OutlinedTextField(
        value = filter,
        onValueChange = { filter = it },
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        singleLine = true,
        placeholder = { Text("筛选已下载歌曲…") },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
      )
    }

    if (filtered.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 42.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text("曲库为空", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
              "下载完成后的音频会自动保存在此处，支持直接播放或导出到系统 Music/QMDL 文件夹",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      itemsIndexed(filtered, key = { _, f -> f.name }) { idx, file ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (file.ext.equals("flac", true)) MaterialTheme.colorScheme.tertiaryContainer
              else MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(48.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = file.ext.uppercase().ifBlank { "MP3" },
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (file.ext.equals("flac", true)) MaterialTheme.colorScheme.onTertiaryContainer
                  else MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                file.title + if (file.isPreview) " [试听]" else "",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                "${file.artist.ifBlank { "未知歌手" }} · ${formatBytes(file.size)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            FilledTonalIconButton(onClick = { onPlayAt(filtered, idx) }) {
              Icon(Icons.Rounded.PlayArrow, contentDescription = "播放")
            }
            IconButton(onClick = { onExport(file) }) {
              Icon(Icons.Rounded.FileUpload, contentDescription = "导出到系统音乐")
            }
            IconButton(onClick = { pendingDelete = file }) {
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

    item { Spacer(Modifier.height(84.dp)) }
  }

  pendingDelete?.let { file ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("删除本地文件") },
      text = { Text("确定要永久删除「${file.name}」吗？") },
      confirmButton = {
        TextButton(
          onClick = {
            onDelete(file)
            pendingDelete = null
          },
        ) { Text("删除", color = MaterialTheme.colorScheme.error) }
      },
      dismissButton = {
        TextButton(onClick = { pendingDelete = null }) { Text("取消") }
      },
    )
  }
}
