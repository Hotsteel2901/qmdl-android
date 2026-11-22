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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qmdl.android.backend.BackendStatus
import com.qmdl.android.data.MirrorOption
import com.qmdl.android.data.QualityOption
import com.qmdl.android.data.ThemeMode
import com.qmdl.android.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
  var showQrSheet by remember { mutableStateOf(false) }
  var showCookieDialog by remember { mutableStateOf(false) }
  var showLogsDialog by remember { mutableStateOf(false) }
  var cookieInput by remember { mutableStateOf("") }
  var remoteInput by remember(state.remoteUrl) { mutableStateOf(state.remoteUrl) }
  var bridgeInput by remember(state.health?.bridge) { mutableStateOf(state.health?.bridge.orEmpty()) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { Text("账号与设置", style = MaterialTheme.typography.headlineMedium) }

    // 1) Account Card
    item {
      val auth = state.health?.auth
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("QQ 音乐账号", style = MaterialTheme.typography.titleMedium)
          Text(
            text = if (auth?.loggedIn == true) "已登录 · uin ${auth.uin} (${auth.source})"
            else "当前为匿名模式（免费曲目直链 + 匿名镜像 128k；登录可解锁会员 320k/FLAC）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { showQrSheet = true },
              shape = CircleShape,
            ) {
              Icon(Icons.Rounded.QrCode2, null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("扫码登录")
            }
            FilledTonalButton(
              onClick = { showCookieDialog = true },
              shape = CircleShape,
            ) {
              Icon(Icons.Rounded.Key, null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("导入 Cookie")
            }
            if (auth?.loggedIn == true) {
              OutlinedButton(onClick = onLogout, shape = CircleShape) {
                Icon(Icons.Rounded.Logout, null, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }

    // 2) Backend Engine Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("CLI 后端引擎", style = MaterialTheme.typography.titleMedium)
          val statusDesc = when (val st = state.backendStatus) {
            is BackendStatus.LocalRunning -> "内置 Rust CLI 运行中 · 端口 ${st.port}\n目录：${st.musicDir}"
            is BackendStatus.RemoteConnected -> "远程模式 · ${st.baseUrl}"
            is BackendStatus.Starting -> "正在启动内置 qqmusic-dl 后端…"
            is BackendStatus.Error -> "异常：${st.message}"
          }
          Text(
            statusDesc,
            style = MaterialTheme.typography.bodySmall,
            color = if (state.backendStatus is BackendStatus.Error) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = !state.useRemoteBackend,
              onClick = onUseLocal,
              label = { Text("内置本地 CLI") },
              shape = CircleShape,
            )
            FilterChip(
              selected = state.useRemoteBackend,
              onClick = { onUseRemote(remoteInput) },
              label = { Text("远程服务端") },
              shape = CircleShape,
            )
          }
          if (state.useRemoteBackend) {
            OutlinedTextField(
              value = remoteInput,
              onValueChange = { remoteInput = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("远程 qqmusic-dl web 地址") },
              placeholder = { Text("http://192.168.1.100:8088") },
              singleLine = true,
            )
            Button(
              onClick = { onUseRemote(remoteInput) },
              shape = CircleShape,
            ) { Text("连接远程后端") }
          } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilledTonalButton(onClick = onRestartLocal, shape = CircleShape) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("重启引擎")
              }
              OutlinedButton(onClick = { showLogsDialog = true }, shape = CircleShape) {
                Icon(Icons.Rounded.Terminal, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("引擎日志")
              }
            }
          }
        }
      }
    }

    // 3) Download & Mirror & Bridge Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("下载与音源策略", style = MaterialTheme.typography.titleMedium)
          Text("默认音质", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QualityOption.entries.forEach { q ->
              FilterChip(
                selected = state.quality == q,
                onClick = { onQualityChange(q) },
                label = { Text(q.shortLabel) },
                shape = CircleShape,
              )
            }
          }

          Text("匿名镜像源", style = MaterialTheme.typography.labelLarge)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              MirrorOption.entries.take(3).forEach { m ->
                FilterChip(
                  selected = state.mirror == m,
                  onClick = { onMirrorChange(m) },
                  label = { Text(m.label) },
                  shape = CircleShape,
                )
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              MirrorOption.entries.drop(3).forEach { m ->
                FilterChip(
                  selected = state.mirror == m,
                  onClick = { onMirrorChange(m) },
                  label = { Text(m.label) },
                  shape = CircleShape,
                )
              }
            }
          }

          SwitchRow("整曲时长校验", "防止镜像下到试听片段或串版", state.verifyDuration, onVerifyChange)
          SwitchRow("仅下载 60s 试听", "跳过整曲解析，直接下载官方试听", state.previewOnly, onPreviewOnlyChange)
          SwitchRow("整曲失败回落试听", "所有整曲渠道失败时回落 60s 试听", state.fallbackPreview, onFallbackChange)
          SwitchRow("完成后自动导出系统音乐库", "自动复制到 Music/QMDL 供系统播放器扫描", state.autoExportMediaStore, onAutoExportChange)

          OutlinedTextField(
            value = bridgeInput,
            onValueChange = { bridgeInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("会员票据桥地址（可选）") },
            placeholder = { Text("http://192.168.1.10:8080") },
            singleLine = true,
          )
          FilledTonalButton(
            onClick = { onBridgeSave(bridgeInput) },
            shape = CircleShape,
          ) { Text("保存票据桥配置") }
        }
      }
    }

    // 4) Theme & Material You Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("外观与 Material 3 Expressive", style = MaterialTheme.typography.titleMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
              FilterChip(
                selected = state.themeMode == mode,
                onClick = { onThemeChange(mode) },
                label = { Text(mode.label) },
                shape = CircleShape,
              )
            }
          }
          SwitchRow(
            "动态取色 (Material You)",
            "Android 12+ 跟随壁纸取色；Android 8–11 使用翡翠专属主题",
            state.dynamicColor,
            onDynamicColorChange,
          )
        }
      }
    }

    item { Spacer(Modifier.height(84.dp)) }
  }

  // QR Login Modal Bottom Sheet
  if (showQrSheet) {
    DisposableEffect(Unit) {
      onStartLoginPolling()
      onDispose { onStopLoginPolling() }
    }
    ModalBottomSheet(onDismissRequest = { showQrSheet = false }) {
      val ls = state.loginStatus
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text("扫码登录 QQ 音乐", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("qqmusic" to "QQ音乐 App", "wx" to "微信", "qq" to "QQ").forEach { (k, label) ->
            FilterChip(
              selected = state.loginKind == k,
              onClick = { if (!ls.running) onSetLoginKind(k) },
              label = { Text(label) },
              shape = CircleShape,
            )
          }
        }

        val qrUrl = ls.fullQrUrl(state.baseUrl)
        Box(
          modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White),
          contentAlignment = Alignment.Center,
        ) {
          if (qrUrl != null) {
            AsyncImage(
              model = qrUrl,
              contentDescription = "登录二维码",
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            )
          } else if (ls.running) {
            LoadingIndicator()
          } else {
            Text("点击下方生成二维码", color = Color(0xFF445850), style = MaterialTheme.typography.bodySmall)
          }
        }

        Text(
          text = when {
            ls.running -> "等待扫码确认…（单手机可先保存二维码到相册再用扫一扫识别）"
            ls.success -> "✓ 登录成功，Cookie 已更新"
            !ls.error.isNullOrBlank() -> "失败：${ls.error}"
            else -> "选择渠道后点击开始登录"
          },
          style = MaterialTheme.typography.bodySmall,
          color = if (ls.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          if (ls.running) {
            OutlinedButton(onClick = onCancelQrLogin, shape = CircleShape) { Text("取消登录") }
          } else {
            Button(onClick = onStartQrLogin, shape = CircleShape) { Text("生成二维码") }
          }
          if (qrUrl != null) {
            FilledTonalButton(onClick = onSaveQrToGallery, shape = CircleShape) {
              Icon(Icons.Rounded.SaveAlt, null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("存到相册")
            }
          }
        }

        if (ls.lines.isNotEmpty()) {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = ls.lines.takeLast(6).joinToString("\n"),
              modifier = Modifier.padding(12.dp),
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
          }
        }
        Spacer(Modifier.height(20.dp))
      }
    }
  }

  // Cookie Dialog
  if (showCookieDialog) {
    AlertDialog(
      onDismissRequest = { showCookieDialog = false },
      title = { Text("手动导入 Cookie") },
      text = {
        OutlinedTextField(
          value = cookieInput,
          onValueChange = { cookieInput = it },
          label = { Text("粘贴包含 uin 与 qm_keyst 的 Cookie") },
          minLines = 3,
          maxLines = 5,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onSaveCookie(cookieInput)
            cookieInput = ""
            showCookieDialog = false
          },
        ) { Text("保存") }
      },
      dismissButton = {
        TextButton(onClick = { showCookieDialog = false }) { Text("取消") }
      },
    )
  }

  // Backend Logs Dialog
  if (showLogsDialog) {
    AlertDialog(
      onDismissRequest = { showLogsDialog = false },
      title = { Text("内置 CLI 引擎日志") },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text(
            text = state.backendLogs.ifEmpty { listOf("（暂无日志）") }.joinToString("\n"),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showLogsDialog = false }) { Text("关闭") }
      },
    )
  }
}

@Composable
private fun SwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.bodyMedium)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
