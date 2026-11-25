package com.qmdl.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qmdl.android.QmdlApp
import com.qmdl.android.backend.BackendStatus
import com.qmdl.android.data.HealthInfo
import com.qmdl.android.data.JobItem
import com.qmdl.android.data.LibraryFile
import com.qmdl.android.data.LoginStatus
import com.qmdl.android.data.MediaStoreExporter
import com.qmdl.android.data.MirrorOption
import com.qmdl.android.data.QualityOption
import com.qmdl.android.data.ResolveResult
import com.qmdl.android.data.SongItem
import com.qmdl.android.data.ThemeMode
import com.qmdl.android.player.PlayTrack
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections

data class MainUiState(
  val backendStatus: BackendStatus = BackendStatus.Starting,
  val baseUrl: String = "",
  val health: HealthInfo? = null,
  val query: String = "",
  val lastQuery: String = "",
  val searchHistory: List<String> = emptyList(),
  val songs: List<SongItem> = emptyList(),
  val page: Int = 1,
  val hasMore: Boolean = false,
  val searching: Boolean = false,
  val searchError: String? = null,
  val selectedMids: Set<String> = emptySet(),
  val jobs: List<JobItem> = emptyList(),
  val queueFilter: String = "all",
  val library: List<LibraryFile> = emptyList(),
  val libraryTotalSize: Long = 0L,
  val libraryLoading: Boolean = false,
  val quality: QualityOption = QualityOption.MP3_320,
  val mirror: MirrorOption = MirrorOption.AUTO,
  val previewOnly: Boolean = false,
  val fallbackPreview: Boolean = true,
  val verifyDuration: Boolean = true,
  val useRemoteBackend: Boolean = false,
  val remoteUrl: String = "",
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val dynamicColor: Boolean = true,
  val autoExportMediaStore: Boolean = false,
  val resolvingSong: SongItem? = null,
  val resolvingLoading: Boolean = false,
  val resolveResult: ResolveResult? = null,
  val resolveError: String? = null,
  val loginStatus: LoginStatus = LoginStatus(),
  val loginKind: String = "qqmusic",
  val backendLogs: List<String> = emptyList(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as QmdlApp
  private val api = app.api
  private val prefs = app.prefs
  val backendManager = app.backendManager
  val player = app.playerController

  private val coroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
    notify("操作异常: ${throwable.message ?: "未知错误"}")
  }

  private val _ui = MutableStateFlow(
    MainUiState(
      searchHistory = prefs.searchHistory(),
      quality = prefs.quality,
      mirror = prefs.mirror,
      previewOnly = prefs.previewOnly,
      fallbackPreview = prefs.fallbackPreview,
      verifyDuration = prefs.verifyDuration,
      useRemoteBackend = prefs.useRemoteBackend,
      remoteUrl = prefs.remoteUrl,
      themeMode = prefs.themeMode,
      dynamicColor = prefs.dynamicColor,
      autoExportMediaStore = prefs.autoExportMediaStore,
    ),
  )
  val ui: StateFlow<MainUiState> = _ui.asStateFlow()

  private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 16)
  val toast: SharedFlow<String> = _toast.asSharedFlow()

  private var loginPollJob: Job? = null
  private val exportedJobIds: MutableSet<Long> = Collections.synchronizedSet(mutableSetOf())

  init {
    viewModelScope.launch(coroutineErrorHandler) {
      backendManager.status.collect { st ->
        val url = when (st) {
          is BackendStatus.LocalRunning -> st.baseUrl
          is BackendStatus.RemoteConnected -> st.baseUrl
          else -> ""
        }
        _ui.update { it.copy(backendStatus = st, baseUrl = url) }
        if (url.isNotEmpty()) {
          refreshHealth()
          refreshJobs(silent = true)
          refreshLibrary()
        }
      }
    }
    viewModelScope.launch(coroutineErrorHandler) {
      backendManager.logs.collect { lines ->
        _ui.update { it.copy(backendLogs = lines) }
      }
    }
    // Serialized polling loop: awaits each refresh before sleeping so HTTP polls never stack up
    viewModelScope.launch(coroutineErrorHandler) {
      while (isActive) {
        val url = _ui.value.baseUrl
        if (url.isNotEmpty()) {
          refreshJobsInternal(url = url, silent = true)
        }
        delay(1500)
      }
    }
  }

  private fun notify(msg: String) {
    _toast.tryEmit(msg)
  }

  fun setQuery(q: String) {
    _ui.update { it.copy(query = q) }
  }

  fun search(queryOverride: String? = null, page: Int = 1) {
    val q = (queryOverride ?: _ui.value.query).trim()
    if (q.isEmpty()) {
      notify("请输入歌名、歌手或 QQ 音乐链接")
      return
    }
    val url = _ui.value.baseUrl
    if (url.isEmpty()) {
      notify("后端尚未就绪，请稍候")
      return
    }

    if (q.contains("/songDetail/") || q.contains("songmid=")) {
      val hist = prefs.pushSearchHistory(q)
      _ui.update { it.copy(query = q, searchHistory = hist) }
      viewModelScope.launch(coroutineErrorHandler) {
        runCatching {
          val st = _ui.value
          api.enqueueRaw(
            url, q, st.quality.slug, st.previewOnly, !st.fallbackPreview, st.mirror.slug, st.verifyDuration,
          )
        }.onSuccess {
          notify("检测到歌曲链接，已加入下载队列")
          refreshJobs()
        }.onFailure {
          notify("入队失败: ${it.message}")
        }
      }
      return
    }

    val hist = prefs.pushSearchHistory(q)
    _ui.update {
      it.copy(
        query = q,
        searchHistory = hist,
        searching = true,
        searchError = null,
        selectedMids = emptySet(),
      )
    }
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching {
        api.search(url, q, limit = 25, page = page)
      }.onSuccess { resp ->
        _ui.update {
          it.copy(
            songs = resp.songs,
            page = resp.page,
            hasMore = resp.hasMore,
            lastQuery = q,
            searching = false,
          )
        }
        if (resp.songs.isEmpty()) notify("没有找到匹配歌曲")
      }.onFailure { e ->
        _ui.update { it.copy(searching = false, searchError = e.message ?: "搜索失败") }
      }
    }
  }

  fun clearHistory() {
    prefs.clearSearchHistory()
    _ui.update { it.copy(searchHistory = emptyList()) }
  }

  fun toggleSelect(mid: String) {
    _ui.update { st ->
      val next = if (mid in st.selectedMids) st.selectedMids - mid else st.selectedMids + mid
      st.copy(selectedMids = next)
    }
  }

  fun toggleSelectAll() {
    _ui.update { st ->
      val all = st.songs.map { it.mid }.toSet()
      st.copy(selectedMids = if (st.selectedMids.size == all.size) emptySet() else all)
    }
  }

  fun clearSelection() {
    _ui.update { it.copy(selectedMids = emptySet()) }
  }

  fun enqueueSongs(songs: List<SongItem>) {
    val url = _ui.value.baseUrl
    if (url.isEmpty() || songs.isEmpty()) return
    val st = _ui.value
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching {
        api.enqueue(
          baseUrl = url,
          songs = songs,
          quality = st.quality.slug,
          preview = st.previewOnly,
          noPreview = !st.fallbackPreview,
          mirror = st.mirror.slug,
          verify = st.verifyDuration,
        )
      }.onSuccess { ids ->
        _ui.update { it.copy(selectedMids = emptySet()) }
        notify("已加入队列：${ids.size} 首 (${st.quality.label})")
        refreshJobs()
      }.onFailure { e ->
        notify("加入队列失败: ${e.message}")
      }
    }
  }

  fun enqueueSelected() {
    val st = _ui.value
    val list = st.songs.filter { it.mid in st.selectedMids }
    enqueueSongs(list)
  }

  fun resolveSong(song: SongItem) {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    val st = _ui.value
    _ui.update {
      it.copy(
        resolvingSong = song,
        resolvingLoading = true,
        resolveResult = null,
        resolveError = null,
      )
    }
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching {
        api.resolveStream(
          baseUrl = url,
          mid = song.mid,
          quality = st.quality.slug,
          preview = st.previewOnly,
          noPreview = !st.fallbackPreview,
          mirror = st.mirror.slug,
        )
      }.onSuccess { res ->
        _ui.update { it.copy(resolvingLoading = false, resolveResult = res) }
      }.onFailure { e ->
        _ui.update { it.copy(resolvingLoading = false, resolveError = e.message ?: "解析失败") }
      }
    }
  }

  fun dismissResolve() {
    _ui.update { it.copy(resolvingSong = null, resolveResult = null, resolveError = null, resolvingLoading = false) }
  }

  fun setQueueFilter(filter: String) {
    _ui.update { it.copy(queueFilter = filter) }
  }

  fun refreshHealth() {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.health(url) }
        .onSuccess { info -> _ui.update { it.copy(health = info) } }
    }
  }

  fun refreshJobs(silent: Boolean = false) {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      refreshJobsInternal(url = url, silent = silent)
    }
  }

  private suspend fun refreshJobsInternal(url: String, silent: Boolean) {
    runCatching { api.jobs(url) }
      .onSuccess { list ->
        val prevMap = _ui.value.jobs.associateBy { it.id }
        var newlyDone = false
        for (job in list) {
          val prev = prevMap[job.id]
          if (prev != null && prev.status != job.status && job.status == "done") {
            newlyDone = true
            notify("下载完成：${job.name}")
            if (_ui.value.autoExportMediaStore && exportedJobIds.add(job.id)) {
              exportJobToMusic(job)
            }
          }
        }
        _ui.update { it.copy(jobs = list) }
        if (newlyDone) {
          refreshLibrary()
          refreshHealth()
        }
      }
      .onFailure { e ->
        if (!silent) notify("获取队列失败: ${e.message}")
      }
  }

  fun cancelJob(id: Long) {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.cancelJob(url, id) }
        .onSuccess { refreshJobs() }
        .onFailure { notify("取消失败: ${it.message}") }
    }
  }

  fun retryJob(id: Long) {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.retryJob(url, id) }
        .onSuccess {
          notify("已重新入队")
          refreshJobs()
        }
        .onFailure { notify("重试失败: ${it.message}") }
    }
  }

  fun retryAllFailedJobs() {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    val failedIds = _ui.value.jobs
      .filter { it.canRetry }
      .map { it.id }
    if (failedIds.isEmpty()) {
      notify("没有需要重试的任务")
      return
    }
    viewModelScope.launch(coroutineErrorHandler) {
      var retried = 0
      for (id in failedIds) {
        if (runCatching { api.retryJob(url, id) }.isSuccess) {
          retried++
        }
      }
      notify("已重新提交 $retried 个失败任务")
      refreshJobs()
    }
  }

  fun deleteJob(id: Long) {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.deleteJob(url, id) }
        .onSuccess { refreshJobs() }
        .onFailure { notify("移除失败: ${it.message}") }
    }
  }

  fun clearFinishedJobs() {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.clearFinishedJobs(url) }
        .onSuccess { n ->
          notify(if (n > 0) "已清理 $n 个已结束任务" else "没有可清理的任务")
          refreshJobs()
        }
        .onFailure { notify("清理失败: ${it.message}") }
    }
  }

  fun refreshLibrary() {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    _ui.update { it.copy(libraryLoading = true) }
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.library(url) }
        .onSuccess { resp ->
          _ui.update {
            it.copy(
              library = resp.files,
              libraryTotalSize = resp.totalSize,
              libraryLoading = false,
            )
          }
        }
        .onFailure {
          _ui.update { it.copy(libraryLoading = false) }
        }
    }
  }

  fun deleteLibraryFile(file: LibraryFile) {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.deleteLibraryFile(url, file.name) }
        .onSuccess {
          notify("已删除：${file.title}")
          refreshLibrary()
          refreshHealth()
        }
        .onFailure { notify("删除失败: ${it.message}") }
    }
  }

  fun exportLibraryFileToMusic(file: LibraryFile) {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      notify("正在流式导出到系统音乐库…")
      runCatching {
        api.streamTo(file.fullStreamUrl(url)) { input ->
          MediaStoreExporter.exportAudioStream(app, file.name, input)
        }
      }.onSuccess { dest ->
        notify("已导出：$dest")
      }.onFailure { e ->
        notify("导出失败: ${e.message}")
      }
    }
  }

  fun exportJobToMusic(job: JobItem) {
    val url = _ui.value.baseUrl
    val fileName = job.fileName ?: "${job.artist} - ${job.name}.mp3"
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching {
        api.streamTo("$url/api/jobs/${job.id}/file?download=1") { input ->
          MediaStoreExporter.exportAudioStream(app, fileName, input)
        }
      }.onSuccess { dest ->
        notify("已导出到系统音乐：$dest")
      }.onFailure { e ->
        notify("导出失败: ${e.message}")
      }
    }
  }

  fun playJob(job: JobItem) {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    player.playSingle(
      PlayTrack(
        title = job.name,
        subtitle = "${job.artist} · ${job.qualityLabel}",
        url = "$url/api/jobs/${job.id}/file",
        coverUrl = job.resolvedCover(url),
      ),
    )
  }

  fun playLibrary(files: List<LibraryFile>, startIndex: Int) {
    val url = _ui.value.baseUrl
    if (url.isEmpty() || files.isEmpty()) return
    val tracks = files.map { f ->
      PlayTrack(
        title = f.title,
        subtitle = "${f.artist.ifBlank { "本地曲库" }} · ${f.ext.uppercase()}",
        url = f.fullStreamUrl(url),
      )
    }
    player.playQueue(tracks, startIndex)
  }

  fun playResolved(res: ResolveResult) {
    val url = _ui.value.baseUrl
    player.playSingle(
      PlayTrack(
        title = res.song.name,
        subtitle = "${res.song.artist} · ${res.qualityLabel} · 在线直链",
        url = res.url,
        coverUrl = res.song.resolvedCover(url),
      ),
    )
  }

  // ---------- Login & Cookie ----------

  fun setLoginKind(kind: String) {
    _ui.update { it.copy(loginKind = kind) }
  }

  fun startQrLogin() {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    val kind = _ui.value.loginKind
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.startLogin(url, kind) }
        .onSuccess {
          startLoginPolling()
        }
        .onFailure { e ->
          notify("启动扫码登录失败: ${e.message}")
        }
    }
  }

  fun cancelQrLogin() {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.cancelLogin(url) }
      pollLoginOnce()
    }
  }

  fun startLoginPolling() {
    loginPollJob?.cancel()
    loginPollJob = viewModelScope.launch(coroutineErrorHandler) {
      while (isActive) {
        val st = pollLoginOnce()
        if (st != null && !st.running) {
          if (st.success) {
            notify("登录成功，Cookie 已保存")
            refreshHealth()
          }
          break
        }
        delay(1200)
      }
    }
  }

  fun stopLoginPolling() {
    loginPollJob?.cancel()
    loginPollJob = null
  }

  private suspend fun pollLoginOnce(): LoginStatus? {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return null
    return runCatching { api.loginStatus(url) }
      .onSuccess { st -> _ui.update { it.copy(loginStatus = st) } }
      .getOrNull()
  }

  fun saveQrImageToGallery() {
    val qrUrl = _ui.value.loginStatus.fullQrUrl(_ui.value.baseUrl) ?: return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching {
        val bytes = api.downloadBytes(qrUrl)
        MediaStoreExporter.saveQrToGallery(app, bytes, "qmdl_${_ui.value.loginKind}")
      }.onSuccess { path ->
        notify("二维码已保存到相册：$path，可直接用 QQ音乐/微信 扫一扫相册识别")
      }.onFailure { e ->
        notify("保存二维码失败: ${e.message}")
      }
    }
  }

  fun saveCookie(cookie: String) {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.saveCookie(url, cookie) }
        .onSuccess { auth ->
          notify("Cookie 已生效（${auth.uin}）")
          refreshHealth()
        }
        .onFailure { e -> notify("保存失败: ${e.message}") }
    }
  }

  fun logout() {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching { api.logout(url) }
        .onSuccess {
          notify("已退出登录，恢复匿名模式")
          refreshHealth()
        }
        .onFailure { e -> notify("退出失败: ${e.message}") }
    }
  }

  // ---------- Settings ----------

  fun setQuality(q: QualityOption) {
    prefs.quality = q
    _ui.update { it.copy(quality = q) }
    syncBackendConfig()
  }

  fun setMirror(m: MirrorOption) {
    prefs.mirror = m
    _ui.update { it.copy(mirror = m) }
    syncBackendConfig()
  }

  fun setPreviewOnly(v: Boolean) {
    prefs.previewOnly = v
    _ui.update { it.copy(previewOnly = v) }
  }

  fun setFallbackPreview(v: Boolean) {
    prefs.fallbackPreview = v
    _ui.update { it.copy(fallbackPreview = v) }
  }

  fun setVerifyDuration(v: Boolean) {
    prefs.verifyDuration = v
    _ui.update { it.copy(verifyDuration = v) }
    syncBackendConfig()
  }

  fun setBridgeUrl(bridge: String) {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching {
        api.updateConfig(url, bridge = bridge, updateBridge = true)
      }.onSuccess {
        notify(if (bridge.isBlank()) "已关闭票据桥" else "票据桥已更新")
        refreshHealth()
      }.onFailure { e ->
        notify("更新票据桥失败: ${e.message}")
      }
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    prefs.themeMode = mode
    _ui.update { it.copy(themeMode = mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    prefs.dynamicColor = enabled
    _ui.update { it.copy(dynamicColor = enabled) }
  }

  fun setAutoExportMediaStore(enabled: Boolean) {
    prefs.autoExportMediaStore = enabled
    _ui.update { it.copy(autoExportMediaStore = enabled) }
  }

  fun switchToLocalBackend() {
    prefs.useRemoteBackend = false
    _ui.update { it.copy(useRemoteBackend = false) }
    backendManager.restartLocal(prefs.quality.slug, prefs.mirror.slug)
    notify("已切换到内置 CLI 引擎")
  }

  fun switchToRemoteBackend(url: String) {
    val clean = url.trim().trimEnd('/')
    if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
      notify("远程地址需以 http:// 或 https:// 开头")
      return
    }
    prefs.useRemoteBackend = true
    prefs.remoteUrl = clean
    _ui.update { it.copy(useRemoteBackend = true, remoteUrl = clean) }
    backendManager.useRemote(clean)
    notify("已连接远程服务端：$clean")
  }

  fun restartLocalBackend() {
    backendManager.restartLocal(prefs.quality.slug, prefs.mirror.slug)
    notify("正在重启内置 CLI 后端…")
  }

  private fun syncBackendConfig() {
    val url = _ui.value.baseUrl
    if (url.isEmpty()) return
    val st = _ui.value
    viewModelScope.launch(coroutineErrorHandler) {
      runCatching {
        api.updateConfig(
          baseUrl = url,
          defaultQuality = st.quality.slug,
          mirror = st.mirror.slug,
          verify = st.verifyDuration,
        )
      }
    }
  }
}
