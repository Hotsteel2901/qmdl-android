package com.qmdl.android.data

enum class QualityOption(val slug: String, val label: String, val shortLabel: String) {
  MP3_128("128", "128k MP3", "128k"),
  MP3_320("320", "320k MP3", "320k"),
  FLAC("flac", "FLAC 无损", "FLAC");

  companion object {
    fun fromSlug(slug: String?): QualityOption =
      entries.firstOrNull { it.slug.equals(slug, ignoreCase = true) } ?: MP3_320
  }
}

enum class MirrorOption(val slug: String, val label: String) {
  AUTO("auto", "自动 (Auto)"),
  MIGU("migu", "咪咕音乐"),
  KUWO("kuwo", "酷我音乐"),
  NETEASE("netease", "网易云音乐"),
  OFF("off", "关闭镜像");

  companion object {
    fun fromSlug(slug: String?): MirrorOption =
      entries.firstOrNull { it.slug.equals(slug, ignoreCase = true) } ?: AUTO
  }
}

enum class ThemeMode(val key: String, val label: String) {
  SYSTEM("system", "跟随系统"),
  DARK("dark", "深色"),
  LIGHT("light", "浅色");

  companion object {
    fun fromKey(key: String?): ThemeMode =
      entries.firstOrNull { it.key == key } ?: SYSTEM
  }
}

data class AuthInfo(
  val loggedIn: Boolean = false,
  val uin: String = "",
  val source: String = "匿名",
  val error: String? = null,
)

data class HealthInfo(
  val version: String = "",
  val apiVersion: Int = 1,
  val anon: Boolean = false,
  val loginAvailable: Boolean = true,
  val loginBlockedReason: String? = null,
  val auth: AuthInfo = AuthInfo(),
  val outDir: String = "",
  val bridge: String? = null,
  val defaultQuality: String = "320",
  val mirror: String = "auto",
  val verify: Boolean = true,
  val libraryCount: Int = 0,
)

data class SongItem(
  val mid: String,
  val mediaMid: String,
  val name: String,
  val artist: String,
  val album: String,
  val albumMid: String,
  val coverUrl: String?,
  val remoteCoverUrl: String?,
  val duration: Long,
  val durationText: String,
  val size128: Long,
  val size320: Long,
  val sizeFlac: Long,
  val qualityFlags: String,
) {
  val bestBadge: String
    get() = when {
      sizeFlac > 0 || qualityFlags.contains("FLAC") -> "FLAC"
      size320 > 0 || qualityFlags.contains("320") -> "320k"
      size128 > 0 || qualityFlags.contains("128") -> "128k"
      else -> "-"
    }

  fun resolvedCover(baseUrl: String): String? {
    val cleanBase = baseUrl.trimEnd('/')
    return when {
      !coverUrl.isNullOrBlank() && coverUrl.startsWith("/") -> "$cleanBase$coverUrl"
      !coverUrl.isNullOrBlank() -> coverUrl
      !remoteCoverUrl.isNullOrBlank() -> remoteCoverUrl
      else -> null
    }
  }
}

data class SearchResponse(
  val songs: List<SongItem>,
  val page: Int,
  val limit: Int,
  val hasMore: Boolean,
)

data class JobItem(
  val id: Long,
  val mid: String,
  val name: String,
  val artist: String,
  val albumMid: String,
  val coverUrl: String?,
  val requestedQuality: String,
  val quality: String,
  val qualityLabel: String,
  val preview: Boolean,
  val status: String,
  val stage: String,
  val source: String,
  val downloaded: Long,
  val total: Long,
  val speed: Double,
  val path: String?,
  val fileName: String?,
  val url: String?,
  val error: String?,
  val verified: Double?,
  val messages: List<String>,
  val elapsed: Double,
) {
  val isActive: Boolean
    get() = status in setOf("queued", "resolving", "downloading", "verifying")

  val canCancel: Boolean
    get() = status in setOf("queued", "resolving", "downloading")

  val canRetry: Boolean
    get() = status == "failed" || status == "cancelled"

  val progressRatio: Float?
    get() = when {
      total > 0 -> (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
      status == "done" -> 1f
      else -> null
    }

  fun resolvedCover(baseUrl: String): String? {
    val cleanBase = baseUrl.trimEnd('/')
    return when {
      !coverUrl.isNullOrBlank() && coverUrl.startsWith("/") -> "$cleanBase$coverUrl"
      !coverUrl.isNullOrBlank() -> coverUrl
      else -> null
    }
  }

  fun statusText(): String = when (status) {
    "queued" -> "等待中"
    "resolving" -> "解析中"
    "downloading" -> "下载中"
    "verifying" -> "校验中"
    "done" -> "已完成"
    "failed" -> "失败"
    "cancelled" -> "已取消"
    else -> status
  }
}

data class LibraryFile(
  val name: String,
  val title: String,
  val artist: String,
  val ext: String,
  val size: Long,
  val modifiedMs: Long,
  val isPreview: Boolean,
  val url: String,
  val downloadUrl: String,
) {
  fun fullStreamUrl(baseUrl: String): String =
    if (url.startsWith("/")) "${baseUrl.trimEnd('/')}$url" else url
}

data class LibraryResponse(
  val outDir: String,
  val totalSize: Long,
  val files: List<LibraryFile>,
)

data class ResolveResult(
  val song: SongItem,
  val quality: String,
  val qualityLabel: String,
  val source: String,
  val preview: Boolean,
  val degraded: Boolean,
  val expectedSize: Long,
  val url: String,
  val fileName: String,
  val messages: List<String>,
)

data class LoginStatus(
  val running: Boolean = false,
  val success: Boolean = false,
  val kind: String? = null,
  val canceled: Boolean = false,
  val error: String? = null,
  val lines: List<String> = emptyList(),
  val qrUrl: String? = null,
) {
  fun fullQrUrl(baseUrl: String): String? =
    qrUrl?.let { if (it.startsWith("/")) "${baseUrl.trimEnd('/')}$it" else it }
}

fun formatBytes(bytes: Long): String {
  if (bytes < 1024L) return "$bytes B"
  val units = arrayOf("KB", "MB", "GB", "TB")
  var value = bytes.toDouble()
  var idx = -1
  while (value >= 1024.0 && idx < units.lastIndex) {
    value /= 1024.0
    idx++
  }
  val fmt = when {
    value >= 100 -> "%.0f"
    value >= 10 -> "%.1f"
    else -> "%.2f"
  }
  return "${fmt.format(value)} ${units[idx]}"
}

fun formatSpeed(bytesPerSec: Double): String {
  if (!bytesPerSec.isFinite() || bytesPerSec <= 0.0) return ""
  return "${formatBytes(bytesPerSec.toLong())}/s"
}

fun formatMillis(ms: Long): String {
  val totalSec = (ms.coerceAtLeast(0L) / 1000L)
  val min = totalSec / 60
  val sec = totalSec % 60
  return "%d:%02d".format(min, sec)
}
