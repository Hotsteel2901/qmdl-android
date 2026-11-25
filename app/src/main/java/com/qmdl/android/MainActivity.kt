package com.qmdl.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.qmdl.android.backend.QmdlBackendService
import com.qmdl.android.ui.liquid.LiquidAmbientWallpaper
import com.qmdl.android.ui.liquid.LiquidGlassBottomTab
import com.qmdl.android.ui.liquid.LiquidGlassBottomTabs
import com.qmdl.android.ui.liquid.LiquidGlassButton
import com.qmdl.android.ui.liquid.LiquidGlassCard
import com.qmdl.android.ui.liquid.LocalLiquidBackdrop
import com.qmdl.android.ui.liquid.LocalLiquidDark
import com.qmdl.android.ui.screens.DiscoverScreen
import com.qmdl.android.ui.screens.ExpandedPlayerSheet
import com.qmdl.android.ui.screens.LibraryScreen
import com.qmdl.android.ui.screens.MiniPlayerDock
import com.qmdl.android.ui.screens.QueueScreen
import com.qmdl.android.ui.screens.SettingsScreen
import com.qmdl.android.ui.theme.QmdlLiquidTheme
import com.qmdl.android.viewmodel.MainViewModel

private data class NavTab(val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
  private val vm: MainViewModel by viewModels()

  private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { _ -> }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    requestRuntimePermissions()
    if (!(application as QmdlApp).prefs.useRemoteBackend) {
      QmdlBackendService.start(this)
    }

    setContent {
      val ui by vm.ui.collectAsStateWithLifecycle()
      val playerState by vm.player.state.collectAsStateWithLifecycle()

      QmdlLiquidTheme(
        themeMode = ui.themeMode,
        dynamicColor = ui.dynamicColor,
      ) {
        QmdlRoot(vm = vm, ui = ui, playerState = playerState)
      }
    }
  }

  private fun requestRuntimePermissions() {
    val needed = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
      ) {
        needed += Manifest.permission.POST_NOTIFICATIONS
      }
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED
      ) {
        needed += Manifest.permission.WRITE_EXTERNAL_STORAGE
      }
    }
    if (needed.isNotEmpty()) {
      permissionLauncher.launch(needed.toTypedArray())
    }
  }
}

@Composable
private fun QmdlRoot(
  vm: MainViewModel,
  ui: com.qmdl.android.viewmodel.MainUiState,
  playerState: com.qmdl.android.player.PlayerUiState,
) {
  var currentTab by rememberSaveable { mutableIntStateOf(0) }
  var showExpandedPlayer by remember { mutableStateOf(false) }
  val snackbar = remember { SnackbarHostState() }
  val isDark = LocalLiquidDark.current

  // Root backdrop captures the ambient aurora wallpaper + scrolling page content for true Liquid Glass refraction
  val ambientBackdrop = rememberLayerBackdrop()
  val chromeBackdrop = rememberLayerBackdrop()

  LaunchedEffect(Unit) {
    vm.toast.collect { msg ->
      snackbar.showSnackbar(msg)
    }
  }

  val tabs = remember {
    listOf(
      NavTab("探索", Icons.Rounded.Explore),
      NavTab("队列", Icons.Rounded.QueueMusic),
      NavTab("曲库", Icons.Rounded.LibraryMusic),
      NavTab("设置", Icons.Rounded.Tune),
    )
  }
  val activeJobs = ui.jobs.count { it.isActive }

  Box(modifier = Modifier.fillMaxSize()) {
    // Layer 1: Luminous ambient wallpaper captured for all in-page glass cards
    LiquidAmbientWallpaper(
      dark = isDark,
      modifier = Modifier
        .fillMaxSize()
        .layerBackdrop(ambientBackdrop),
    )

    // Layer 2: Scrolling page content (also captured by chromeBackdrop so floating bottom bar & mini-player refract live content)
    CompositionLocalProvider(LocalLiquidBackdrop provides ambientBackdrop) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .layerBackdrop(chromeBackdrop)
          .statusBarsPadding(),
      ) {
        when (currentTab) {
          0 -> DiscoverScreen(
            state = ui,
            onQueryChange = vm::setQuery,
            onSearch = vm::search,
            onClearHistory = vm::clearHistory,
            onQualityChange = vm::setQuality,
            onToggleSelect = vm::toggleSelect,
            onToggleSelectAll = vm::toggleSelectAll,
            onDownloadOne = { vm.enqueueSongs(listOf(it)) },
            onResolve = vm::resolveSong,
            onDismissResolve = vm::dismissResolve,
            onPlayResolved = { ui.resolveResult?.let(vm::playResolved) },
            onOpenAccount = { currentTab = 3 },
          )
          1 -> QueueScreen(
            state = ui,
            onFilterChange = vm::setQueueFilter,
            onCancel = vm::cancelJob,
            onRetry = vm::retryJob,
            onRetryAllFailed = vm::retryAllFailedJobs,
            onDelete = vm::deleteJob,
            onPlay = vm::playJob,
            onExport = vm::exportJobToMusic,
            onClearFinished = vm::clearFinishedJobs,
          )
          2 -> LibraryScreen(
            state = ui,
            onRefresh = vm::refreshLibrary,
            onPlayAt = vm::playLibrary,
            onExport = vm::exportLibraryFileToMusic,
            onDelete = vm::deleteLibraryFile,
          )
          3 -> SettingsScreen(
            state = ui,
            onSetLoginKind = vm::setLoginKind,
            onStartQrLogin = vm::startQrLogin,
            onCancelQrLogin = vm::cancelQrLogin,
            onStartLoginPolling = vm::startLoginPolling,
            onStopLoginPolling = vm::stopLoginPolling,
            onSaveQrToGallery = vm::saveQrImageToGallery,
            onSaveCookie = vm::saveCookie,
            onLogout = vm::logout,
            onQualityChange = vm::setQuality,
            onMirrorChange = vm::setMirror,
            onPreviewOnlyChange = vm::setPreviewOnly,
            onFallbackChange = vm::setFallbackPreview,
            onVerifyChange = vm::setVerifyDuration,
            onBridgeSave = vm::setBridgeUrl,
            onAutoExportChange = vm::setAutoExportMediaStore,
            onThemeChange = vm::setThemeMode,
            onDynamicColorChange = vm::setDynamicColor,
            onUseLocal = vm::switchToLocalBackend,
            onUseRemote = vm::switchToRemoteBackend,
            onRestartLocal = vm::restartLocalBackend,
          )
        }
      }
    }

    // Layer 3: Floating Liquid Glass Bottom Chrome (Snackbar + Batch FAB + MiniPlayer + Draggable Glass Tab Bar)
    CompositionLocalProvider(LocalLiquidBackdrop provides chromeBackdrop) {
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SnackbarHost(hostState = snackbar) { data ->
          LiquidGlassCard(
            cornerRadius = 20.dp,
            tint = MaterialTheme.colorScheme.primary,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = data.visuals.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        AnimatedVisibility(
          visible = currentTab == 0 && ui.selectedMids.isNotEmpty(),
          enter = fadeIn() + slideInVertically { it / 2 },
          exit = fadeOut() + slideOutVertically { it / 2 },
          modifier = Modifier.align(Alignment.End),
        ) {
          LiquidGlassButton(
            onClick = { vm.enqueueSelected() },
            tint = MaterialTheme.colorScheme.primary,
            height = 48.dp,
            horizontalPadding = 20.dp,
          ) {
            Icon(
              Icons.Rounded.Download,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(19.dp),
            )
            Text(
              text = "下载已选 (${ui.selectedMids.size})",
              color = Color.White,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        MiniPlayerDock(
          playerState = playerState,
          onExpand = { showExpandedPlayer = true },
          onTogglePlay = { vm.player.togglePlayPause() },
          onNext = { vm.player.next() },
          onClose = { vm.player.stopAndDismiss() },
        )

        LiquidGlassBottomTabs(
          selectedTabIndex = { currentTab },
          onTabSelected = { idx ->
            currentTab = idx
            if (idx == 2) vm.refreshLibrary()
            if (idx == 3) vm.refreshHealth()
          },
          tabsCount = tabs.size,
          modifier = Modifier.fillMaxWidth(),
        ) {
          tabs.forEachIndexed { idx, tab ->
            val selected = currentTab == idx
            val contentTint = if (selected) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.72f else 0.68f)
            }
            LiquidGlassBottomTab(
              onClick = {
                currentTab = idx
                if (idx == 2) vm.refreshLibrary()
                if (idx == 3) vm.refreshHealth()
              },
            ) {
              if (idx == 1 && activeJobs > 0) {
                BadgedBox(
                  badge = {
                    Badge(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                      Text("$activeJobs", fontSize = 10.sp)
                    }
                  },
                ) {
                  Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = contentTint,
                    modifier = Modifier.size(22.dp),
                  )
                }
              } else {
                Icon(
                  tab.icon,
                  contentDescription = tab.label,
                  tint = contentTint,
                  modifier = Modifier.size(22.dp),
                )
              }
              Text(
                text = tab.label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentTint,
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }

  if (showExpandedPlayer && playerState.current != null) {
    CompositionLocalProvider(LocalLiquidBackdrop provides chromeBackdrop) {
      ExpandedPlayerSheet(
        playerState = playerState,
        onDismiss = { showExpandedPlayer = false },
        onTogglePlay = { vm.player.togglePlayPause() },
        onPrev = { vm.player.previous() },
        onNext = { vm.player.next() },
        onSeek = { vm.player.seekTo(it) },
      )
    }
  }
}
