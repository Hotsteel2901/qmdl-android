package com.qmdl.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qmdl.android.backend.BackendStatus
import com.qmdl.android.data.QualityOption
import com.qmdl.android.data.SongItem
import com.qmdl.android.data.formatBytes
import com.qmdl.android.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiscoverScreen(
  state: MainUiState,
  onQueryChange: (String) -> Unit,
  onSearch: (String?, Int) -> Unit,
  onClearHistory: () -> Unit,
  onQualityChange: (QualityOption) -> Unit,
  onToggleSelect: (String) -> Unit,
  onToggleSelectAll: () -> Unit,
  onDownloadOne: (SongItem) -> Unit,
  onResolve: (SongItem) -> Unit,
  onDismissResolve: () -> Unit,
  onPlayResolved: () -> Unit,
  onOpenAccount: () -> Unit,
) {
  val keyboard = LocalSoftwareKeyboardController.current
  val context = LocalContext.current

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Expressive Hero Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "QMDL 音乐",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = when (state.backendStatus) {
              is BackendStatus.LocalRunning -> "内置 CLI 引擎运行中"
              is BackendStatus.RemoteConnected -> "已连接远程服务端"
              is BackendStatus.Starting -> "正在启动内置引擎…"
              is BackendStatus.Error -> "引擎未就绪"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        val loggedIn = state.health?.auth?.loggedIn == true
        Surface(
          onClick = onOpenAccount,
          shape = CircleShape,
          color = if (loggedIn) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                  if (loggedIn) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.tertiary,
                ),
            )
            Text(
              text = if (loggedIn) "已登录" else "匿名模式",
              style = MaterialTheme.typography.labelLarge,
              color = if (loggedIn) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // Expressive Pill Search Bar
    item {
      OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        singleLine = true,
        placeholder = { Text("搜索歌名 / 歌手，或粘贴 QQ 音乐链接") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.query.isNotEmpty()) {
              IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Rounded.Close, contentDescription = "清空")
              }
            }
            FilledTonalIconButton(
              onClick = {
                keyboard?.hide()
                onSearch(null, 1)
              },
              modifier = Modifier.padding(end = 4.dp),
            ) {
              Icon(Icons.Rounded.Search, contentDescription = "搜索")
            }
          }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
          onSearch = {
            keyboard?.hide()
            onSearch(null, 1)
          },
        ),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
      )
    }

    // Quick Quality Picker + History Chips
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          QualityOption.entries.forEach { q ->
            FilterChip(
              selected = state.quality == q,
              onClick = { onQualityChange(q) },
              label = { Text(q.label) },
              shape = CircleShape,
            )
          }
        }

        if (state.searchHistory.isNotEmpty()) {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            items(state.searchHistory.take(8)) { term ->
              AssistChip(
                onClick = {
                  keyboard?.hide()
                  onSearch(term, 1)
                },
                label = { Text(term, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                shape = CircleShape,
              )
            }
            item {
              TextButton(onClick = onClearHistory) {
                Text("清空历史")
              }
            }
          }
        }
      }
    }

    // Loading / Error / Empty / Results
    if (state.searching) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingIndicator()
            Spacer(Modifier.height(12.dp))
            Text("正在检索曲库…", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    } else if (state.searchError != null) {
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
          Text(
            text = state.searchError,
            modifier = Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }
    } else if (state.songs.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Surface(
              shape = RoundedCornerShape(22.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(64.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Rounded.MusicNote,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(32.dp),
                )
              }
            }
            Spacer(Modifier.height(14.dp))
            Text("探索与收藏高品质音乐", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
              "支持 QQ 原生直链、会员票据桥与多平台匿名镜像，点击单曲即可多选批量下载",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "第 ${state.page} 页 · ${state.songs.size} 首",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          TextButton(onClick = onToggleSelectAll) {
            Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (state.selectedMids.size == state.songs.size) "取消全选" else "全选")
          }
        }
      }

      items(state.songs, key = { it.mid }) { song ->
        val selected = song.mid in state.selectedMids
        SongExpressiveCard(
          song = song,
          baseUrl = state.baseUrl,
          selected = selected,
          onClick = { onToggleSelect(song.mid) },
          onDownload = { onDownloadOne(song) },
          onResolve = { onResolve(song) },
        )
      }

      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          OutlinedButton(
            onClick = { onSearch(state.lastQuery, state.page - 1) },
            enabled = state.page > 1 && !state.searching,
            shape = CircleShape,
          ) { Text("上一页") }
          OutlinedButton(
            onClick = { onSearch(state.lastQuery, state.page + 1) },
            enabled = state.hasMore && !state.searching,
            shape = CircleShape,
          ) { Text("下一页") }
        }
      }
    }

    item { Spacer(Modifier.height(84.dp)) }
  }

  // Stream Resolve / Direct Audition Bottom Sheet
  if (state.resolvingSong != null) {
    ModalBottomSheet(onDismissRequest = onDismissResolve) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(state.resolvingSong.name, style = MaterialTheme.typography.titleLarge)
        Text(
          "${state.resolvingSong.artist} · ${state.resolvingSong.album}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.resolvingLoading) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
          ) { LoadingIndicator() }
        } else if (state.resolveError != null) {
          Text(state.resolveError, color = MaterialTheme.colorScheme.error)
        } else if (state.resolveResult != null) {
          val r = state.resolveResult
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                "${r.qualityLabel}${if (r.preview) " · 官方60s试听" else ""} · ${r.source}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
              if (r.expectedSize > 0) {
                Text("大小：${formatBytes(r.expectedSize)}", style = MaterialTheme.typography.bodySmall)
              }
              Text(
                r.url,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = {
                onPlayResolved()
                onDismissResolve()
              },
              modifier = Modifier.weight(1f),
              shape = CircleShape,
            ) {
              Icon(Icons.Rounded.PlayArrow, null)
              Spacer(Modifier.width(4.dp))
              Text("在线试听")
            }
            FilledTonalButton(
              onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("qmdl-url", r.url))
              },
              shape = CircleShape,
            ) {
              Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("复制直链")
            }
          }
          OutlinedButton(
            onClick = {
              onDownloadOne(state.resolvingSong)
              onDismissResolve()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
          ) {
            Icon(Icons.Rounded.Download, null)
            Spacer(Modifier.width(6.dp))
            Text("加入下载队列")
          }
        }
        Spacer(Modifier.height(18.dp))
      }
    }
  }
}

@Composable
private fun SongExpressiveCard(
  song: SongItem,
  baseUrl: String,
  selected: Boolean,
  onClick: () -> Unit,
  onDownload: () -> Unit,
  onResolve: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.large)
      .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
      else MaterialTheme.colorScheme.surfaceContainer,
    ),
    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
      ) {
        val cover = song.resolvedCover(baseUrl)
        if (cover != null) {
          AsyncImage(
            model = cover,
            contentDescription = song.album,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        androidx.compose.animation.AnimatedVisibility(visible = selected) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              Icons.Rounded.CheckCircle,
              contentDescription = "已选",
              tint = MaterialTheme.colorScheme.onPrimary,
            )
          }
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = song.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = listOf(song.artist, song.album).filter { it.isNotBlank() }.joinToString(" · "),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          QualityBadgePill(song.bestBadge)
          Text(
            text = song.durationText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      IconButton(onClick = onResolve) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = "试听/直链")
      }
      FilledTonalIconButton(onClick = onDownload) {
        Icon(Icons.Rounded.Download, contentDescription = "下载")
      }
    }
  }
}

@Composable
private fun QualityBadgePill(badge: String) {
  if (badge == "-") return
  val (bg, fg) = when (badge) {
    "FLAC" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    "320k" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
  }
  Surface(shape = RoundedCornerShape(6.dp), color = bg) {
    Text(
      text = badge,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = fg,
    )
  }
}
