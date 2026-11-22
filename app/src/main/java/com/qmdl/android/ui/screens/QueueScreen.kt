package com.qmdl.android.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qmdl.android.data.JobItem
import com.qmdl.android.data.formatBytes
import com.qmdl.android.data.formatSpeed
import com.qmdl.android.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QueueScreen(
  state: MainUiState,
  onFilterChange: (String) -> Unit,
  onCancel: (Long) -> Unit,
  onRetry: (Long) -> Unit,
  onDelete: (Long) -> Unit,
  onPlay: (JobItem) -> Unit,
  onExport: (JobItem) -> Unit,
  onClearFinished: () -> Unit,
) {
  var logDialogJob by remember { mutableStateOf<JobItem?>(null) }
  val activeCount = state.jobs.count { it.isActive }
  val filtered = state.jobs.filter { j ->
    when (state.queueFilter) {
      "active" -> j.isActive
      "done" -> j.status == "done"
      "failed" -> j.status == "failed" || j.status == "cancelled"
      else -> true
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
          Text("下载队列", style = MaterialTheme.typography.headlineMedium)
          Text(
            text = if (state.jobs.isEmpty()) "暂无任务"
            else "进行中 $activeCount · 共 ${state.jobs.size} 个任务",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        FilledTonalButton(
          onClick = onClearFinished,
          enabled = state.jobs.any { !it.isActive },
          shape = CircleShape,
        ) {
          Icon(Icons.Rounded.CleaningServices, null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("清理已结束")
        }
      }
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("all" to "全部", "active" to "进行中", "done" to "已完成", "failed" to "失败").forEach { (key, label) ->
          FilterChip(
            selected = state.queueFilter == key,
            onClick = { onFilterChange(key) },
            label = { Text(label) },
            shape = CircleShape,
          )
        }
      }
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
            Text("当前筛选下没有任务", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
              "在「探索」页搜索歌曲并点击下载即可加入队列",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(filtered, key = { it.id }) { job ->
        JobExpressiveCard(
          job = job,
          baseUrl = state.baseUrl,
          onCancel = { onCancel(job.id) },
          onRetry = { onRetry(job.id) },
          onDelete = { onDelete(job.id) },
          onPlay = { onPlay(job) },
          onExport = { onExport(job) },
          onShowLogs = { logDialogJob = job },
        )
      }
    }

    item { Spacer(Modifier.height(84.dp)) }
  }

  logDialogJob?.let { job ->
    AlertDialog(
      onDismissRequest = { logDialogJob = null },
      title = { Text("${job.name} · 日志") },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          if (!job.error.isNullOrBlank()) {
            Text(
              job.error,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
          }
          Text(
            text = job.messages.ifEmpty { listOf("（暂无日志）") }.joinToString("\n"),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { logDialogJob = null }) { Text("关闭") }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun JobExpressiveCard(
  job: JobItem,
  baseUrl: String,
  onCancel: () -> Unit,
  onRetry: () -> Unit,
  onDelete: () -> Unit,
  onPlay: () -> Unit,
  onExport: () -> Unit,
  onShowLogs: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = when (job.status) {
        "failed" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
        "downloading" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
        else -> MaterialTheme.colorScheme.surfaceContainer
      },
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
          contentAlignment = Alignment.Center,
        ) {
          val cover = job.resolvedCover(baseUrl)
          if (cover != null) {
            AsyncImage(
              model = cover,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            job.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            listOf(job.artist, job.qualityLabel + if (job.preview) " 试听" else "", job.source)
              .filter { it.isNotBlank() }
              .joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Surface(
          shape = CircleShape,
          color = when (job.status) {
            "done", "downloading" -> MaterialTheme.colorScheme.primaryContainer
            "failed" -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
          },
        ) {
          Text(
            job.statusText(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }

      // Expressive Wavy Progress Bar for active downloads
      if (job.isActive) {
        val ratio = job.progressRatio
        if (ratio != null && job.total > 0) {
          LinearWavyProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = job.error?.lines()?.firstOrNull() ?: job.stage,
          style = MaterialTheme.typography.bodySmall,
          color = if (job.status == "failed") MaterialTheme.colorScheme.error
          else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        val metrics = buildList {
          if (job.total > 0) {
            add("${formatBytes(job.downloaded)} / ${formatBytes(job.total)}")
          } else if (job.downloaded > 0) {
            add(formatBytes(job.downloaded))
          }
          val spd = formatSpeed(job.speed)
          if (spd.isNotEmpty() && job.isActive) add(spd)
          job.verified?.let { add("%.0fs ✓".format(it)) }
        }.joinToString(" · ")
        if (metrics.isNotEmpty()) {
          Text(
            text = metrics,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (job.canCancel) {
          TextButton(onClick = onCancel) {
            Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("取消")
          }
        }
        if (job.status == "done" && !job.path.isNullOrBlank()) {
          FilledTonalButton(onClick = onPlay, shape = CircleShape) {
            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("播放")
          }
          TextButton(onClick = onExport) {
            Icon(Icons.Rounded.FileUpload, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("导出音乐库")
          }
        }
        if (job.canRetry) {
          FilledTonalButton(onClick = onRetry, shape = CircleShape) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("重试")
          }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onShowLogs) {
          Icon(Icons.Rounded.Terminal, contentDescription = "查看日志")
        }
        if (!job.isActive) {
          IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "移除")
          }
        }
      }
    }
  }
}
