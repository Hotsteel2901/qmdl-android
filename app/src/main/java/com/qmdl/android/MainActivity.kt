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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qmdl.android.backend.QmdlBackendService
import com.qmdl.android.ui.screens.DiscoverScreen
import com.qmdl.android.ui.screens.ExpandedPlayerSheet
import com.qmdl.android.ui.screens.LibraryScreen
import com.qmdl.android.ui.screens.MiniPlayerDock
import com.qmdl.android.ui.screens.QueueScreen
import com.qmdl.android.ui.screens.SettingsScreen
import com.qmdl.android.ui.theme.QmdlExpressiveTheme
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

      QmdlExpressiveTheme(
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

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbar) },
    floatingActionButton = {
      AnimatedVisibility(visible = currentTab == 0 && ui.selectedMids.isNotEmpty()) {
        ExtendedFloatingActionButton(
          onClick = { vm.enqueueSelected() },
          shape = CircleShape,
          icon = { Icon(Icons.Rounded.Download, null) },
          text = { Text("下载已选 (${ui.selectedMids.size})") },
        )
      }
    },
    bottomBar = {
      Column {
        MiniPlayerDock(
          playerState = playerState,
          onExpand = { showExpandedPlayer = true },
          onTogglePlay = { vm.player.togglePlayPause() },
          onNext = { vm.player.next() },
          onClose = { vm.player.stopAndDismiss() },
        )
        NavigationBar {
          tabs.forEachIndexed { idx, tab ->
            NavigationBarItem(
              selected = currentTab == idx,
              onClick = {
                currentTab = idx
                if (idx == 2) vm.refreshLibrary()
                if (idx == 3) vm.refreshHealth()
              },
              icon = {
                if (idx == 1 && activeJobs > 0) {
                  BadgedBox(badge = { Badge { Text("$activeJobs") } }) {
                    Icon(tab.icon, contentDescription = tab.label)
                  }
                } else {
                  Icon(tab.icon, contentDescription = tab.label)
                }
              },
              label = { Text(tab.label) },
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
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

  if (showExpandedPlayer && playerState.current != null) {
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
