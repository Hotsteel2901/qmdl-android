package com.qmdl.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qmdl.android.backend.BackendStatus
import com.qmdl.android.data.QualityOption
import com.qmdl.android.data.SongItem
import com.qmdl.android.data.formatBytes
import com.qmdl.android.ui.liquid.LiquidGlassButton
import com.qmdl.android.ui.liquid.LiquidGlassCard
import com.qmdl.android.ui.liquid.LiquidGlassChip
import com.qmdl.android.ui.liquid.LiquidGlassSpinner
import com.qmdl.android.ui.liquid.LiquidGlassTextField
import com.qmdl.android.ui.liquid.LocalLiquidDark
import com.qmdl.android.viewmodel.MainUiState

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
  val context = LocalContext.current
  val keyboard = LocalSoftwareKeyboardController.current
  val isDark = LocalLiquidDark.current

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 190.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Header + Engine / Account Pill
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("QMDL 音乐", style = MaterialTheme.typography.headlineMedium)
          val statusLine = when (val st = state.backendStatus) {
            is BackendStatus.Starting -> "内置引擎启动中…"
            is BackendStatus.LocalRunning -> "内置引擎就绪 · 端口 ${st.port}"
            is BackendStatus.RemoteConnected -> "远程服务已连接"
            is BackendStatus.Error -> "引擎异常，请前往设置查看"
          }
          val statusColor = when (state.backendStatus) {
            is BackendStatus.LocalRunning, is BackendStatus.RemoteConnected -> MaterialTheme.colorScheme.primary
            is BackendStatus.Error -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(statusColor),
            )
            Text(
              text = statusLine,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        val auth = state.health?.auth
        val accountLabel = if (auth?.loggedIn == true) {
          auth.uin.ifBlank { "已登录" }
        } else {
          "匿名 · 登录"
        }
        LiquidGlassChip(
          label = accountLabel,
          selected = auth?.loggedIn == true,
          onClick = onOpenAccount,
          leading = {
            Icon(
              Icons.Rounded.AccountCircle,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = if (auth?.loggedIn == true) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
        )
      }
    }

    // Liquid Glass Search Bar + Paste Link + Search Button
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LiquidGlassTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = "搜索歌名、歌手，或粘贴 QQ 音乐链接…",
            modifier = Modifier.weight(1f),
            leading = {
              Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            },
            trailing = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.query.isNotEmpty()) {
                  IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Clear, contentDescription = "清空", modifier = Modifier.size(17.dp))
                  }
                } else {
                  IconButton(
                    onClick = {
                      val cm = context.getSystemService(ClipboardManager::class.java)
                      val clip = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                      if (!clip.isNullOrEmpty()) {
                        onQueryChange(clip)
                        onSearch(clip, 1)
                      } else {
                        Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
                      }
                    },
                    modifier = Modifier.size(28.dp),
                  ) {
                    Icon(
                      Icons.Rounded.ContentPaste,
                      contentDescription = "粘贴并搜索",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(17.dp),
                    )
                  }
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
          )

          LiquidGlassButton(
            onClick = {
              keyboard?.hide()
              onSearch(null, 1)
            },
            tint = MaterialTheme.colorScheme.primary,
            height = 46.dp,
            horizontalPadding = 16.dp,
          ) {
            Text("搜索", color = Color.White, style = MaterialTheme.typography.labelLarge)
          }
        }

        // Quality Selector Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(QualityOption.entries) { opt ->
            LiquidGlassChip(
              label = opt.label,
              selected = state.quality == opt,
              onClick = { onQualityChange(opt) },
              leading = if (opt == QualityOption.FLAC) {
                {
                  Icon(
                    Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (state.quality == opt) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary,
                  )
                }
              } else null,
            )
          }
        }
      }
    }

    // Recent Search History
    if (state.searchHistory.isNotEmpty() && state.songs.isEmpty() && !state.searching) {
      item {
        LiquidGlassCard(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 22.dp,
          contentPadding = PaddingValues(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text("最近搜索", style = MaterialTheme.typography.titleSmall)
            }
            Text(
              text = "清空",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClearHistory)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          Spacer(Modifier.height(10.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.searchHistory) { h ->
              LiquidGlassChip(
                label = h.take(22),
                selected = false,
                onClick = {
                  onQueryChange(h)
                  onSearch(h, 1)
                },
              )
            }
          }
        }
      }
    }

    // Error Banner
    if (!state.searchError.isNullOrBlank()) {
      item {
        LiquidGlassCard(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 20.dp,
          tint = MaterialTheme.colorScheme.error,
          contentPadding = PaddingValues(14.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
              text = state.searchError,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    // Loading State
    if (state.searching) {
      item {
        LiquidGlassCard(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 26.dp,
          contentPadding = PaddingValues(vertical = 36.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            LiquidGlassSpinner(size = 38.dp)
            Text(
              "正在搜索曲库…",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // Search Results Header + Select All
    if (state.songs.isNotEmpty() && !state.searching) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "「${state.lastQuery}」第 ${state.page} 页 · ${state.songs.size} 首",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          LiquidGlassChip(
            label = if (state.selectedMids.size == state.songs.size && state.songs.isNotEmpty()) {
              "取消全选"
            } else {
              "全选本页"
            },
            selected = state.selectedMids.isNotEmpty(),
            onClick = onToggleSelectAll,
          )
        }
      }

      // Collision-proof composite key prevents LazyColumn crash on duplicate mid results
      itemsIndexed(state.songs, key = { idx, s -> "${s.mid}_$idx" }) { _, song ->
        val selected = song.mid in state.selectedMids
        val coverUrl = song.resolvedCover(state.baseUrl)
        val badge = song.bestBadge
        val isFlac = badge == "FLAC"
        val sizeHint = when (state.quality) {
          QualityOption.FLAC -> song.sizeFlac.takeIf { it > 0 } ?: song.size320
          QualityOption.MP3_320 -> song.size320.takeIf { it > 0 } ?: song.size128
          QualityOption.MP3_128 -> song.size128
        }

        LiquidGlassCard(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 22.dp,
          tint = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
          onClick = { onToggleSelect(song.mid) },
          contentPadding = PaddingValues(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            // Cover Art with Selection Overlay
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                  Brush.linearGradient(
                    listOf(
                      MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                      MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f),
                    ),
                  ),
                )
                .border(
                  width = if (selected) 1.5.dp else 0.75.dp,
                  color = if (selected) MaterialTheme.colorScheme.primary
                  else Color.White.copy(alpha = if (isDark) 0.18f else 0.7f),
                  shape = RoundedCornerShape(15.dp),
                ),
              contentAlignment = Alignment.Center,
            ) {
              if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                  model = coverUrl,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              }
              if (selected) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(Icons.Rounded.Check, contentDescription = "已选中", tint = Color.White)
                }
              }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                text = song.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = buildString {
                  append(song.artist.ifBlank { "未知歌手" })
                  if (song.album.isNotBlank()) append(" · ${song.album}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                if (badge != "-") {
                  Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isFlac) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(
                        (if (isFlac) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                          .copy(alpha = 0.16f),
                      )
                      .padding(horizontal = 6.dp, vertical = 1.dp),
                  )
                }
                if (song.durationText.isNotBlank() && song.durationText != "00:00") {
                  Text(
                    text = song.durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (sizeHint > 0) {
                  Text(
                    text = "≈ ${formatBytes(sizeHint)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            IconButton(onClick = { onResolve(song) }, modifier = Modifier.size(36.dp)) {
              Icon(
                Icons.Rounded.Headphones,
                contentDescription = "解析试听",
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
            LiquidGlassButton(
              onClick = { onDownloadOne(song) },
              tint = MaterialTheme.colorScheme.primary,
              height = 36.dp,
              horizontalPadding = 10.dp,
            ) {
              Icon(
                Icons.Rounded.Download,
                contentDescription = "下载",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      // Pagination Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LiquidGlassButton(
            onClick = { onSearch(state.lastQuery, (state.page - 1).coerceAtLeast(1)) },
            enabled = state.page > 1,
            height = 40.dp,
            horizontalPadding = 16.dp,
          ) {
            Text("上一页", style = MaterialTheme.typography.labelLarge)
          }
          Text(
            text = "第 ${state.page} 页",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          LiquidGlassButton(
            onClick = { onSearch(state.lastQuery, state.page + 1) },
            enabled = state.hasMore,
            height = 40.dp,
            horizontalPadding = 16.dp,
          ) {
            Text("下一页", style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    } else if (!state.searching && state.songs.isEmpty()) {
      // Friendly Empty Welcome Card
      item {
        LiquidGlassCard(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 28.dp,
          contentPadding = PaddingValues(vertical = 36.dp, horizontal = 22.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Rounded.MusicNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
              modifier = Modifier.size(44.dp),
            )
            Text("搜索曲库或粘贴歌曲链接", style = MaterialTheme.typography.titleMedium)
            Text(
              "支持按歌名、歌手检索，或直接粘贴 QQ 音乐分享链接一键加入下载队列。点击卡片可多选批量下载。",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }

  // Resolve Stream Bottom Sheet
  if (state.resolvingSong != null) {
    ResolveSongSheet(
      state = state,
      onDismiss = onDismissResolve,
      onPlay = onPlayResolved,
      onDownload = {
        onDownloadOne(state.resolvingSong)
        onDismissResolve()
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolveSongSheet(
  state: MainUiState,
  onDismiss: () -> Unit,
  onPlay: () -> Unit,
  onDownload: () -> Unit,
) {
  val song = state.resolvingSong ?: return
  val context = LocalContext.current
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
        .padding(horizontal = 22.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(song.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          "${song.artist} · ${song.album.ifBlank { "单曲" }}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      when {
        state.resolvingLoading -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            LiquidGlassSpinner(size = 28.dp)
            Text("正在解析直链音源…", style = MaterialTheme.typography.bodyMedium)
          }
        }
        !state.resolveError.isNullOrBlank() -> {
          LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            tint = MaterialTheme.colorScheme.error,
          ) {
            Text(
              "解析失败：${state.resolveError}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
        state.resolveResult != null -> {
          val res = state.resolveResult
          LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(14.dp),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                "音质：${res.qualityLabel}" +
                  (if (res.preview) " · 试听片段" else "") +
                  (if (res.degraded) " · 已自动降级" else ""),
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                "来源：${res.source.ifBlank { "默认引擎" }}" +
                  if (res.expectedSize > 0) " · 预计大小 ${formatBytes(res.expectedSize)}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (res.messages.isNotEmpty()) {
                Text(
                  res.messages.joinToString("\n"),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            LiquidGlassButton(
              onClick = onPlay,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
              Text("在线试听", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            LiquidGlassButton(
              onClick = onDownload,
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
              Text("加入下载", style = MaterialTheme.typography.labelLarge)
            }
          }

          LiquidGlassButton(
            onClick = {
              val cm = context.getSystemService(ClipboardManager::class.java)
              if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("qmdl_url", res.url))
                Toast.makeText(context, "直链已复制到剪贴板", Toast.LENGTH_SHORT).show()
              }
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(17.dp))
            Text("复制音频直链", style = MaterialTheme.typography.labelLarge)
          }
        }
      }

      Spacer(Modifier.height(20.dp))
    }
  }
}
