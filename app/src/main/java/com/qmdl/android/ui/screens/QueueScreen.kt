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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.qmdl.android.data.JobItem
import com.qmdl.android.data.formatBytes
import com.qmdl.android.data.formatSpeed
import com.qmdl.android.ui.liquid.LiquidGlassButton
import com.qmdl.android.ui.liquid.LiquidGlassCard
import com.qmdl.android.ui.liquid.LiquidGlassChip
import com.qmdl.android.ui.liquid.LiquidGlassProgressBar
import com.qmdl.android.viewmodel.MainUiState
import kotlin.math.roundToInt

@Composable
fun QueueScreen(
  state: MainUiState,
  onFilterChange: (String) -> Unit,
  onCancel: (Long) -> Unit,
  onRetry: (Long) -> Unit,
  onRetryAllFailed: () -> Unit,
  onDelete: (Long) -> Unit,
  onPlay: (JobItem) -> Unit,
  onExport: (JobItem) -> Unit,
  onClearFinished: () -> Unit,
) {
  var logsJob by remember { mutableStateOf<JobItem?>(null) }

  val activeCount = remember(state.jobs) { state.jobs.count { it.isActive } }
  val doneCount = remember(state.jobs) { state.jobs.count { it.status == "done" } }
  val failedCount = remember(state.jobs) { state.jobs.count { it.canRetry } }

  val filtered = remember(state.jobs, state.queueFilter) {
    when (state.queueFilter) {
      "active" -> state.jobs.filter { it.isActive }
      "done" -> state.jobs.filter { it.status == "done" }
      "failed" -> state.jobs.filter { it.canRetry }
      else -> state.jobs
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 185.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("下载队列", style = MaterialTheme.typography.headlineMedium)
          Text(
            if (activeCount > 0) "正在高速下载 $activeCount 首音频"
            else "共 ${state.jobs.size} 个任务记录",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (failedCount > 0) {
            LiquidGlassButton(
              onClick = onRetryAllFailed,
              tint = MaterialTheme.colorScheme.tertiary,
              height = 38.dp,
              horizontalPadding = 12.dp,
            ) {
              Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
              Text("重试失败", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
          }
          LiquidGlassButton(
            onClick = onClearFinished,
            enabled = doneCount + failedCount > 0,
            height = 38.dp,
            horizontalPadding = 12.dp,
          ) {
            Icon(Icons.Rounded.CleaningServices, null, modifier = Modifier.size(16.dp))
            Text("清理", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    // Liquid Glass Summary Metrics Card
    item {
      LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          QueueStatColumn(
            label = "进行中",
            count = activeCount,
            accent = MaterialTheme.colorScheme.primary,
          )
          QueueStatColumn(
            label = "已完成",
            count = doneCount,
            accent = MaterialTheme.colorScheme.secondary,
          )
          QueueStatColumn(
            label = "失败/取消",
            count = failedCount,
            accent = if (failedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Filter Pills
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          LiquidGlassChip(
            label = "全部 (${state.jobs.size})",
            selected = state.queueFilter == "all",
            onClick = { onFilterChange("all") },
          )
        }
        item {
          LiquidGlassChip(
            label = "进行中 ($activeCount)",
            selected = state.queueFilter == "active",
            onClick = { onFilterChange("active") },
          )
        }
        item {
          LiquidGlassChip(
            label = "已完成 ($doneCount)",
            selected = state.queueFilter == "done",
            onClick = { onFilterChange("done") },
          )
        }
        item {
          LiquidGlassChip(
            label = "失败 ($failedCount)",
            selected = state.queueFilter == "failed",
            onClick = { onFilterChange("failed") },
          )
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
              Icons.Rounded.QueueMusic,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
              modifier = Modifier.size(44.dp),
            )
            Text("当前分类下没有任务", style = MaterialTheme.typography.titleMedium)
            Text(
              "在「探索」页搜索并点选歌曲后，下载进度、实时速率与预计剩余时间会显示在这里",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      // Collision-proof composite key
      itemsIndexed(filtered, key = { idx, j -> "${j.id}_$idx" }) { _, job ->
        JobLiquidCard(
          job = job,
          baseUrl = state.baseUrl,
          onCancel = { onCancel(job.id) },
          onRetry = { onRetry(job.id) },
          onDelete = { onDelete(job.id) },
          onPlay = { onPlay(job) },
          onExport = { onExport(job) },
          onShowLogs = { logsJob = job },
        )
      }
    }
  }

  logsJob?.let { job ->
    AlertDialog(
      onDismissRequest = { logsJob = null },
      title = { Text("任务详情 · ${job.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "状态：${job.statusText()} · 音质：${job.qualityLabel} · 来源：${job.source.ifBlank { "默认" }}",
            style = MaterialTheme.typography.labelMedium,
          )
          if (!job.error.isNullOrBlank()) {
            Text(
              "错误：${job.error}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
          if (job.messages.isNotEmpty()) {
            Text(
              job.messages.joinToString("\n"),
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.5.sp),
            )
          } else {
            Text("暂无更多日志输出", style = MaterialTheme.typography.bodySmall)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { logsJob = null }) { Text("关闭") }
      },
    )
  }
}

@Composable
private fun QueueStatColumn(
  label: String,
  count: Int,
  accent: Color,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = "$count",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.ExtraBold,
      color = accent,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun JobLiquidCard(
  job: JobItem,
  baseUrl: String,
  onCancel: () -> Unit,
  onRetry: () -> Unit,
  onDelete: () -> Unit,
  onPlay: () -> Unit,
  onExport: () -> Unit,
  onShowLogs: () -> Unit,
) {
  val statusColor = when (job.status) {
    "done" -> MaterialTheme.colorScheme.primary
    "failed" -> MaterialTheme.colorScheme.error
    "cancelled" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.secondary
  }
  val cover = job.resolvedCover(baseUrl)

  LiquidGlassCard(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 22.dp,
    tint = if (job.status == "failed") MaterialTheme.colorScheme.error else Color.Unspecified,
    contentPadding = PaddingValues(13.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center,
        ) {
          if (!cover.isNullOrBlank()) {
            AsyncImage(
              model = cover,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = job.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = job.statusText(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = statusColor,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(statusColor.copy(alpha = 0.16f))
                .padding(horizontal = 6.dp, vertical = 1.dp),
            )
            Text(
              text = "${job.artist} · ${job.qualityLabel}" + if (job.preview) " (试听)" else "",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        IconButton(onClick = onShowLogs, modifier = Modifier.size(34.dp)) {
          Icon(
            Icons.Rounded.Terminal,
            contentDescription = "查看日志",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      // Active Progress + Speed + Humanized ETA
      if (job.isActive) {
        val ratio = job.progressRatio
        LiquidGlassProgressBar(progress = ratio, modifier = Modifier.fillMaxWidth())

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val leftInfo = buildString {
            if (ratio != null) append("${(ratio * 100f).roundToInt()}%")
            else append(job.stage.ifBlank { job.statusText() })
            if (job.total > 0) {
              append(" · ${formatBytes(job.downloaded)} / ${formatBytes(job.total)}")
            } else if (job.downloaded > 0) {
              append(" · ${formatBytes(job.downloaded)}")
            }
          }
          val rightInfo = buildString {
            val spd = formatSpeed(job.speed)
            if (spd.isNotEmpty()) append(spd)
            val eta = formatEta(job.downloaded, job.total, job.speed)
            if (eta.isNotEmpty()) {
              if (isNotEmpty()) append(" · ")
              append(eta)
            }
          }
          Text(
            text = leftInfo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (rightInfo.isNotEmpty()) {
            Text(
              text = rightInfo,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      // Friendly Error Explanation
      if (!job.error.isNullOrBlank()) {
        val hint = humanizeJobError(job.error)
        Text(
          text = hint,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Action Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (job.status == "done") {
          if (job.verified != null && job.verified > 0) {
            Text(
              text = "时长校验通过 · ${job.verified.roundToInt()}s",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.weight(1f),
            )
          }
          LiquidGlassButton(
            onClick = onPlay,
            tint = MaterialTheme.colorScheme.primary,
            height = 34.dp,
            horizontalPadding = 12.dp,
          ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text("播放", color = Color.White, style = MaterialTheme.typography.labelMedium)
          }
          LiquidGlassButton(
            onClick = onExport,
            height = 34.dp,
            horizontalPadding = 12.dp,
          ) {
            Icon(Icons.Rounded.FileUpload, null, modifier = Modifier.size(16.dp))
            Text("导出", style = MaterialTheme.typography.labelMedium)
          }
        }

        if (job.canCancel) {
          LiquidGlassButton(
            onClick = onCancel,
            height = 34.dp,
            horizontalPadding = 12.dp,
          ) {
            Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(16.dp))
            Text("取消", style = MaterialTheme.typography.labelMedium)
          }
        }

        if (job.canRetry) {
          LiquidGlassButton(
            onClick = onRetry,
            tint = MaterialTheme.colorScheme.primary,
            height = 34.dp,
            horizontalPadding = 12.dp,
          ) {
            Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text("重试", color = Color.White, style = MaterialTheme.typography.labelMedium)
          }
        }

        if (!job.isActive) {
          IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
              Icons.Rounded.DeleteOutline,
              contentDescription = "删除记录",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

private fun formatEta(downloaded: Long, total: Long, speedBytesPerSec: Double): String {
  if (total <= 0L || downloaded >= total || !speedBytesPerSec.isFinite() || speedBytesPerSec < 1024.0) {
    return ""
  }
  val remainingSec = ((total - downloaded) / speedBytesPerSec).roundToInt().coerceAtLeast(1)
  return when {
    remainingSec < 60 -> "剩余约 ${remainingSec}秒"
    remainingSec < 3600 -> "剩余约 ${remainingSec / 60}分${remainingSec % 60}秒"
    else -> "剩余约 ${remainingSec / 3600}小时"
  }
}

private fun humanizeJobError(raw: String): String {
  val lower = raw.lowercase()
  return when {
    "cookie" in lower || "vip" in lower || "403" in lower ->
      "音源受限：$raw（建议在「设置」中登录账号或开启镜像源）"
    "timeout" in lower || "connection" in lower ->
      "网络波动导致下载中断，可点击重试恢复"
    else -> raw
  }
}
