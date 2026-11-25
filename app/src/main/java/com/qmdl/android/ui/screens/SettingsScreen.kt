package com.qmdl.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.qmdl.android.backend.BackendStatus
import com.qmdl.android.data.MirrorOption
import com.qmdl.android.data.QualityOption
import com.qmdl.android.data.ThemeMode
import com.qmdl.android.ui.liquid.LiquidGlassButton
import com.qmdl.android.ui.liquid.LiquidGlassCard
import com.qmdl.android.ui.liquid.LiquidGlassChip
import com.qmdl.android.ui.liquid.LiquidGlassSpinner
import com.qmdl.android.ui.liquid.LiquidGlassTextField
import com.qmdl.android.ui.liquid.LiquidGlassToggle
import com.qmdl.android.ui.liquid.LocalLiquidDark
import com.qmdl.android.viewmodel.MainUiState

@Composable
fun SettingsScreen(
  state: MainUiState,
  onSetLoginKind: (String) -> Unit,
  onStartQrLogin: () -> Unit,
  onCancelQrLogin: () -> Unit,
  onStartLoginPolling: () -> Unit,
  onStopLoginPolling: () -> Unit,
  onSaveQrToGallery: () -> Unit,
  onSaveCookie: (String) -> Unit,
  onLogout: () -> Unit,
  onQualityChange: (QualityOption) -> Unit,
  onMirrorChange: (MirrorOption) -> Unit,
  onPreviewOnlyChange: (Boolean) -> Unit,
  onFallbackChange: (Boolean) -> Unit,
  onVerifyChange: (Boolean) -> Unit,
  onBridgeSave: (String) -> Unit,
  onAutoExportChange: (Boolean) -> Unit,
  onThemeChange: (ThemeMode) -> Unit,
  onDynamicColorChange: (Boolean) -> Unit,
  onUseLocal: () -> Unit,
  onUseRemote: (String) -> Unit,
  onRestartLocal: () -> Unit,
) {
  var cookieInput by remember { mutableStateOf("") }
  var showCookieBox by remember { mutableStateOf(false) }
  var bridgeInput by remember(state.health?.bridge) { mutableStateOf(state.health?.bridge.orEmpty()) }
  var remoteInput by remember(state.remoteUrl) { mutableStateOf(state.remoteUrl) }
  var showLogs by remember { mutableStateOf(false) }
  val isDark = LocalLiquidDark.current

  DisposableEffect(Unit) {
    if (state.loginStatus.running) onStartLoginPolling()
    onDispose { onStopLoginPolling() }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 185.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Column {
        Text("设置与引擎", style = MaterialTheme.typography.headlineMedium)
        Text(
          "账号授权、下载音质策略、液态玻璃主题与后端引擎",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // 1. Account & QR Login
    item {
      val auth = state.health?.auth
      val loggedIn = auth?.loggedIn == true
      LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          SettingsSectionHeader(
            icon = Icons.Rounded.AccountCircle,
            title = "QQ 音乐账号",
            subtitle = if (loggedIn) "已登录 · ${auth?.uin.orEmpty()} (${auth?.source.orEmpty()})"
            else "当前为匿名模式，登录绿钻账号可解锁更高音质",
          )

          if (loggedIn) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              LiquidGlassButton(
                onClick = onLogout,
                tint = MaterialTheme.colorScheme.error,
                height = 40.dp,
              ) {
                Icon(Icons.Rounded.Logout, null, tint = Color.White, modifier = Modifier.size(17.dp))
                Text("退出当前账号", color = Color.White, style = MaterialTheme.typography.labelLarge)
              }
            }
          }

          // Login Kind Chips + QR Controls
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            LiquidGlassChip(
              label = "QQ 音乐扫码",
              selected = state.loginKind == "qqmusic",
              onClick = { onSetLoginKind("qqmusic") },
            )
            LiquidGlassChip(
              label = "微信扫码",
              selected = state.loginKind == "wx",
              onClick = { onSetLoginKind("wx") },
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            LiquidGlassButton(
              onClick = onStartQrLogin,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.weight(1f),
              height = 42.dp,
            ) {
              Icon(Icons.Rounded.QrCode2, null, tint = Color.White, modifier = Modifier.size(18.dp))
              Text(
                if (state.loginStatus.running) "重新获取二维码" else "获取登录二维码",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
              )
            }
            if (state.loginStatus.running) {
              LiquidGlassButton(
                onClick = onCancelQrLogin,
                height = 42.dp,
                horizontalPadding = 14.dp,
              ) {
                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(17.dp))
                Text("取消", style = MaterialTheme.typography.labelLarge)
              }
            }
          }

          val qrUrl = state.loginStatus.fullQrUrl(state.baseUrl)
          if (state.loginStatus.running || qrUrl != null || !state.loginStatus.error.isNullOrBlank()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                  if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                )
                .padding(14.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (qrUrl != null) {
                Box(
                  modifier = Modifier
                    .size(184.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(8.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  AsyncImage(
                    model = qrUrl,
                    contentDescription = "登录二维码",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
                LiquidGlassButton(onClick = onSaveQrToGallery, height = 38.dp) {
                  Icon(Icons.Rounded.Save, null, modifier = Modifier.size(16.dp))
                  Text("保存二维码到相册", style = MaterialTheme.typography.labelMedium)
                }
              } else if (state.loginStatus.running) {
                LiquidGlassSpinner(size = 28.dp)
              }
              if (!state.loginStatus.error.isNullOrBlank()) {
                Text(
                  text = state.loginStatus.error,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                )
              }
              if (state.loginStatus.lines.isNotEmpty()) {
                Text(
                  text = state.loginStatus.lines.takeLast(3).joinToString("\n"),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // Manual Cookie Entry
          LiquidGlassChip(
            label = if (showCookieBox) "收起手动导入 Cookie" else "手动粘贴 Cookie",
            selected = showCookieBox,
            onClick = { showCookieBox = !showCookieBox },
            leading = { Icon(Icons.Rounded.Key, null, modifier = Modifier.size(15.dp)) },
          )

          AnimatedVisibility(visible = showCookieBox) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              LiquidGlassTextField(
                value = cookieInput,
                onValueChange = { cookieInput = it },
                placeholder = "粘贴完整 Cookie 字符串（含 uin / qm_keyst）…",
                singleLine = false,
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
              )
              LiquidGlassButton(
                onClick = {
                  if (cookieInput.isNotBlank()) {
                    onSaveCookie(cookieInput.trim())
                    cookieInput = ""
                  }
                },
                enabled = cookieInput.isNotBlank(),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text("保存并验证 Cookie", color = Color.White, style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }
    }

    // 2. Download Preferences & Mirror
    item {
      LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          SettingsSectionHeader(
            icon = Icons.Rounded.Download,
            title = "下载与音源策略",
            subtitle = "配置默认音质、第三方镜像回退与系统曲库自动导出",
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("默认下载音质", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(QualityOption.entries) { opt ->
                LiquidGlassChip(
                  label = opt.label,
                  selected = state.quality == opt,
                  onClick = { onQualityChange(opt) },
                )
              }
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("音源镜像通道", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(MirrorOption.entries) { opt ->
                LiquidGlassChip(
                  label = opt.label,
                  selected = state.mirror == opt,
                  onClick = { onMirrorChange(opt) },
                )
              }
            }
          }

          SettingsToggleRow(
            title = "自动导出到系统音乐库",
            subtitle = "下载完成后自动流式写入 Music/QMDL，系统播放器立即可见",
            checked = state.autoExportMediaStore,
            onCheckedChange = onAutoExportChange,
          )
          SettingsToggleRow(
            title = "校验音频完整时长",
            subtitle = "下载完成后自动核对文件时长，防止截断残缺",
            checked = state.verifyDuration,
            onCheckedChange = onVerifyChange,
          )
          SettingsToggleRow(
            title = "无完整音源时回退试听",
            subtitle = "VIP 歌曲未登录且镜像不可用时允许回退为试听片段",
            checked = state.fallbackPreview,
            onCheckedChange = onFallbackChange,
          )
          SettingsToggleRow(
            title = "仅下载试听片段",
            subtitle = "始终只获取短试听音频（适合快速试音）",
            checked = state.previewOnly,
            onCheckedChange = onPreviewOnlyChange,
          )

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("票据桥地址（可选）", style = MaterialTheme.typography.labelLarge)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              LiquidGlassTextField(
                value = bridgeInput,
                onValueChange = { bridgeInput = it },
                placeholder = "留空则关闭票据桥",
                modifier = Modifier.weight(1f),
              )
              LiquidGlassButton(
                onClick = { onBridgeSave(bridgeInput.trim()) },
                height = 44.dp,
                horizontalPadding = 14.dp,
              ) {
                Text("应用", style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }
    }

    // 3. Appearance & Liquid Glass Theme
    item {
      LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          SettingsSectionHeader(
            icon = Icons.Rounded.Palette,
            title = "外观与液态玻璃",
            subtitle = "实时光学折射镜片、色散光晕与极光背景适配",
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ThemeMode.entries) { mode ->
              LiquidGlassChip(
                label = mode.label,
                selected = state.themeMode == mode,
                onClick = { onThemeChange(mode) },
              )
            }
          }
          SettingsToggleRow(
            title = "系统动态取色 (Android 12+)",
            subtitle = "跟随系统壁纸自动调谐液态玻璃极光与强调色",
            checked = state.dynamicColor,
            onCheckedChange = onDynamicColorChange,
          )
        }
      }
    }

    // 4. Backend Engine & Logs
    item {
      LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          val statusDesc = when (val st = state.backendStatus) {
            is BackendStatus.Starting -> "内置 CLI 后端正在启动…"
            is BackendStatus.LocalRunning -> "内置 CLI 运行中 · ${st.baseUrl}"
            is BackendStatus.RemoteConnected -> "远程服务 · ${st.baseUrl}"
            is BackendStatus.Error -> "异常：${st.message}"
          }
          SettingsSectionHeader(
            icon = Icons.Rounded.Cloud,
            title = "后端引擎",
            subtitle = statusDesc,
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LiquidGlassChip(
              label = "内置本地引擎",
              selected = !state.useRemoteBackend,
              onClick = onUseLocal,
            )
            LiquidGlassChip(
              label = "远程服务端",
              selected = state.useRemoteBackend,
              onClick = {
                if (remoteInput.isNotBlank()) onUseRemote(remoteInput)
              },
            )
          }

          if (!state.useRemoteBackend) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "CLI 版本：${state.health?.version?.ifBlank { "内置" } ?: "检测中"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              LiquidGlassButton(
                onClick = onRestartLocal,
                height = 38.dp,
                horizontalPadding = 14.dp,
              ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                Text("重启内置引擎", style = MaterialTheme.typography.labelMedium)
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            LiquidGlassTextField(
              value = remoteInput,
              onValueChange = { remoteInput = it },
              placeholder = "远程地址如 http://192.168.1.10:18088",
              modifier = Modifier.weight(1f),
            )
            LiquidGlassButton(
              onClick = { onUseRemote(remoteInput) },
              tint = MaterialTheme.colorScheme.primary,
              height = 44.dp,
              horizontalPadding = 14.dp,
            ) {
              Text("连接", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
          }

          // Collapsible CLI Logs
          LiquidGlassChip(
            label = if (showLogs) "收起后端运行日志 (${state.backendLogs.size})" else "展开后端运行日志 (${state.backendLogs.size})",
            selected = showLogs,
            onClick = { showLogs = !showLogs },
            leading = {
              Icon(
                if (showLogs) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
          )

          AnimatedVisibility(visible = showLogs) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0xFF060C10) else Color(0xFF101C22))
                .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            ) {
              val text = if (state.backendLogs.isEmpty()) "暂无日志输出"
              else state.backendLogs.takeLast(60).joinToString("\n")
              Text(
                text = text,
                color = Color(0xFFD6F2E6),
                style = MaterialTheme.typography.bodySmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp,
                  lineHeight = 15.5.sp,
                ),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionHeader(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SettingsToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    LiquidGlassToggle(
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
  }
}
