package com.qmdl.android.backend

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

sealed interface BackendStatus {
  data object Starting : BackendStatus
  data class LocalRunning(val baseUrl: String, val port: Int, val musicDir: String) : BackendStatus
  data class RemoteConnected(val baseUrl: String) : BackendStatus
  data class Error(val message: String) : BackendStatus
}

class QmdlBackendManager(private val context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val lock = Any()

  private val healthClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(1, TimeUnit.SECONDS)
      .readTimeout(1, TimeUnit.SECONDS)
      .build()
  }

  @Volatile
  private var process: Process? = null

  private val _status = MutableStateFlow<BackendStatus>(BackendStatus.Starting)
  val status: StateFlow<BackendStatus> = _status.asStateFlow()

  private val _logs = MutableStateFlow<List<String>>(emptyList())
  val logs: StateFlow<List<String>> = _logs.asStateFlow()

  val musicDir: File by lazy {
    val ext = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
    (ext ?: File(context.filesDir, "music")).apply { mkdirs() }
  }

  private val configDir: File by lazy {
    File(context.filesDir, "config").apply { mkdirs() }
  }

  private val cacheDir: File by lazy {
    File(context.cacheDir, "qmdl-cache").apply { mkdirs() }
  }

  private val tmpDir: File by lazy {
    File(context.cacheDir, "qmdl-tmp").apply { mkdirs() }
  }

  private val cookieFile: File by lazy {
    File(configDir, "qqmusic-dl/cookie.txt").also { it.parentFile?.mkdirs() }
  }

  fun useRemote(url: String) {
    stopLocal()
    val clean = url.trim().trimEnd('/')
    appendLog("[mode] 切换远程后端: $clean")
    _status.value = BackendStatus.RemoteConnected(clean)
  }

  fun startLocal(defaultQuality: String = "320", mirror: String = "auto") {
    scope.launch {
      startLocalInternal(defaultQuality, mirror)
    }
  }

  fun restartLocal(defaultQuality: String = "320", mirror: String = "auto") {
    scope.launch {
      stopLocalSync()
      startLocalInternal(defaultQuality, mirror)
    }
  }

  /** Non-blocking stop safe to call from the Main thread. */
  fun stopLocal() {
    scope.launch {
      stopLocalSync()
    }
  }

  private fun stopLocalSync() {
    val proc = synchronized(lock) {
      val current = process
      process = null
      current
    }
    if (proc != null) {
      appendLog("[cli] 正在停止本地后端…")
      runCatching { proc.destroy() }
      runCatching {
        if (!proc.waitFor(1500, TimeUnit.MILLISECONDS)) {
          proc.destroyForcibly()
        }
      }
    }
  }

  private suspend fun startLocalInternal(defaultQuality: String, mirror: String) {
    synchronized(lock) {
      val cur = process
      if (cur != null && cur.isAlive) {
        return
      }
    }
    _status.value = BackendStatus.Starting

    val bin = File(context.applicationInfo.nativeLibraryDir, "libqmdl.so")
    if (!bin.exists()) {
      val msg = "未找到内置 CLI 后端 (${bin.absolutePath})。请在设置中切换远程模式或重新构建包含当前架构的 APK。"
      appendLog("[error] $msg")
      _status.value = BackendStatus.Error(msg)
      return
    }

    try {
      val port = pickFreePort()
      val baseUrl = "http://127.0.0.1:$port"
      val args = listOf(
        bin.absolutePath,
        "--mirror", mirror,
        "--cookie-file", cookieFile.absolutePath,
        "web",
        "--bind", "127.0.0.1",
        "--port", port.toString(),
        "--out", musicDir.absolutePath,
        "--quality", defaultQuality,
      )
      appendLog("[cli] 启动内置后端: port=$port out=${musicDir.absolutePath}")

      val pb = ProcessBuilder(args)
        .directory(context.filesDir)
        .redirectErrorStream(true)
      pb.environment().apply {
        put("HOME", context.filesDir.absolutePath)
        put("XDG_CONFIG_HOME", configDir.absolutePath)
        put("XDG_CACHE_HOME", cacheDir.absolutePath)
        put("TMPDIR", tmpDir.absolutePath)
      }
      val proc = pb.start()
      synchronized(lock) {
        process = proc
      }
      scope.launch {
        try {
          BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
            while (true) {
              val line = reader.readLine() ?: break
              appendLog(line)
            }
          }
        } catch (_: Exception) {
        }
        val exit = runCatching { proc.waitFor() }.getOrNull()
        appendLog("[cli] 进程已退出 (code=$exit)")
        synchronized(lock) {
          if (process === proc) {
            process = null
            if (_status.value is BackendStatus.LocalRunning) {
              _status.value = BackendStatus.Error("本地 CLI 后端意外退出 (code=$exit)")
            }
          }
        }
      }

      if (waitUntilReady(baseUrl)) {
        _status.value = BackendStatus.LocalRunning(baseUrl, port, musicDir.absolutePath)
        appendLog("[cli] 后端就绪: $baseUrl")
      } else {
        val alive = proc.isAlive
        _status.value = BackendStatus.Error(
          if (alive) "后端启动超时（端口 $port 无响应）" else "后端进程提前退出，请查看日志",
        )
      }
    } catch (e: Exception) {
      val msg = "启动内置 CLI 后端失败: ${e.message}"
      appendLog("[error] $msg")
      _status.value = BackendStatus.Error(msg)
    }
  }

  private suspend fun waitUntilReady(baseUrl: String): Boolean {
    repeat(30) {
      val alive = synchronized(lock) { process?.isAlive == true }
      if (!alive) return false
      try {
        val req = Request.Builder().url("$baseUrl/api/health").get().build()
        healthClient.newCall(req).execute().use { resp ->
          if (resp.isSuccessful) return true
        }
      } catch (_: Exception) {
      }
      delay(150)
    }
    return false
  }

  private fun pickFreePort(): Int {
    try {
      ServerSocket(18088).use { return 18088 }
    } catch (_: Exception) {
    }
    return ServerSocket(0).use { it.localPort }
  }

  private fun appendLog(line: String) {
    val clean = line.replace(Regex("\u001B\\[[;\\d]*m"), "").trimEnd()
    if (clean.isEmpty()) return
    _logs.update { current -> (current + clean).takeLast(200) }
  }
}
