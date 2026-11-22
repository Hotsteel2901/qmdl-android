package com.qmdl.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class QmdlApiClient(
  val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
    .build(),
) {
  private val jsonType = "application/json; charset=utf-8".toMediaType()

  suspend fun health(baseUrl: String): HealthInfo = withContext(Dispatchers.IO) {
    val root = getJson("$baseUrl/api/health")
    val authObj = root.optJSONObject("auth")
    val statsObj = root.optJSONObject("stats")
    HealthInfo(
      version = root.optString("version", ""),
      apiVersion = root.optInt("api_version", 1),
      anon = root.optBoolean("anon", false),
      loginAvailable = root.optBoolean("login_available", true),
      loginBlockedReason = root.optNullableString("login_blocked_reason"),
      auth = AuthInfo(
        loggedIn = authObj?.optBoolean("logged_in", false) ?: false,
        uin = authObj?.optString("uin", "") ?: "",
        source = authObj?.optString("source", "匿名") ?: "匿名",
        error = authObj?.optNullableString("error"),
      ),
      outDir = root.optString("out_dir", ""),
      bridge = root.optNullableString("bridge"),
      defaultQuality = root.optString("default_quality", "320"),
      mirror = root.optString("mirror", "auto"),
      verify = root.optBoolean("verify", true),
      libraryCount = statsObj?.optInt("library", 0) ?: 0,
    )
  }

  suspend fun search(baseUrl: String, query: String, limit: Int = 25, page: Int = 1): SearchResponse =
    withContext(Dispatchers.IO) {
      val q = URLEncoder.encode(query, "UTF-8")
      val root = getJson("$baseUrl/api/search?q=$q&limit=$limit&page=$page")
      val arr = root.optJSONArray("songs") ?: JSONArray()
      val songs = buildList {
        for (i in 0 until arr.length()) {
          add(parseSong(arr.getJSONObject(i)))
        }
      }
      SearchResponse(
        songs = songs,
        page = root.optInt("page", page),
        limit = root.optInt("limit", limit),
        hasMore = root.optBoolean("has_more", songs.size >= limit),
      )
    }

  suspend fun resolveStream(
    baseUrl: String,
    mid: String,
    quality: String,
    preview: Boolean,
    noPreview: Boolean,
    mirror: String,
  ): ResolveResult = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("mid", mid)
      put("quality", quality)
      put("preview", preview)
      put("no_preview", noPreview)
      put("mirror", mirror)
    }
    val root = postJson("$baseUrl/api/resolve", body)
    ResolveResult(
      song = parseSong(root.getJSONObject("song")),
      quality = root.optString("quality", quality),
      qualityLabel = root.optString("quality_label", quality),
      source = root.optString("source", ""),
      preview = root.optBoolean("preview", false),
      degraded = root.optBoolean("degraded", false),
      expectedSize = root.optLong("expected_size", 0L),
      url = root.optString("url", ""),
      fileName = root.optString("file_name", ""),
      messages = root.optJSONArray("messages").toStringList(),
    )
  }

  suspend fun jobs(baseUrl: String): List<JobItem> = withContext(Dispatchers.IO) {
    val root = getJson("$baseUrl/api/jobs")
    val arr = root.optJSONArray("jobs") ?: JSONArray()
    buildList {
      for (i in 0 until arr.length()) {
        add(parseJob(arr.getJSONObject(i)))
      }
    }.sortedByDescending { it.id }
  }

  suspend fun enqueue(
    baseUrl: String,
    songs: List<SongItem>,
    quality: String,
    preview: Boolean,
    noPreview: Boolean,
    mirror: String,
    verify: Boolean,
  ): List<Long> = withContext(Dispatchers.IO) {
    val songsArr = JSONArray()
    songs.forEach { s ->
      songsArr.put(
        JSONObject().apply {
          put("mid", s.mid)
          put("name", s.name)
          put("artist", s.artist)
          put("album_mid", s.albumMid)
        },
      )
    }
    val body = JSONObject().apply {
      put("songs", songsArr)
      put("quality", quality)
      put("preview", preview)
      put("no_preview", noPreview)
      put("mirror", mirror)
      put("verify", verify)
    }
    val root = postJson("$baseUrl/api/jobs", body)
    val ids = root.optJSONArray("ids") ?: JSONArray()
    buildList {
      for (i in 0 until ids.length()) add(ids.getLong(i))
    }
  }

  suspend fun enqueueRaw(
    baseUrl: String,
    midOrUrl: String,
    quality: String,
    preview: Boolean,
    noPreview: Boolean,
    mirror: String,
    verify: Boolean,
  ): List<Long> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("songs", JSONArray().put(midOrUrl))
      put("quality", quality)
      put("preview", preview)
      put("no_preview", noPreview)
      put("mirror", mirror)
      put("verify", verify)
    }
    val root = postJson("$baseUrl/api/jobs", body)
    val ids = root.optJSONArray("ids") ?: JSONArray()
    buildList {
      for (i in 0 until ids.length()) add(ids.getLong(i))
    }
  }

  suspend fun cancelJob(baseUrl: String, id: Long) = withContext(Dispatchers.IO) {
    postJson("$baseUrl/api/jobs/$id/cancel", JSONObject())
  }

  suspend fun retryJob(baseUrl: String, id: Long): Long = withContext(Dispatchers.IO) {
    postJson("$baseUrl/api/jobs/$id/retry", JSONObject()).optLong("id", 0L)
  }

  suspend fun deleteJob(baseUrl: String, id: Long) = withContext(Dispatchers.IO) {
    postJson("$baseUrl/api/jobs/$id/delete", JSONObject())
  }

  suspend fun clearFinishedJobs(baseUrl: String): Int = withContext(Dispatchers.IO) {
    postJson("$baseUrl/api/jobs/clear", JSONObject()).optInt("removed", 0)
  }

  suspend fun library(baseUrl: String): LibraryResponse = withContext(Dispatchers.IO) {
    val root = getJson("$baseUrl/api/library")
    val arr = root.optJSONArray("files") ?: JSONArray()
    val files = buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
          LibraryFile(
            name = o.optString("name", ""),
            title = o.optString("title", ""),
            artist = o.optString("artist", ""),
            ext = o.optString("ext", ""),
            size = o.optLong("size", 0L),
            modifiedMs = o.optLong("modified_ms", 0L),
            isPreview = o.optBoolean("is_preview", false),
            url = o.optString("url", ""),
            downloadUrl = o.optString("download_url", ""),
          ),
        )
      }
    }
    LibraryResponse(
      outDir = root.optString("out_dir", ""),
      totalSize = root.optLong("total_size", 0L),
      files = files,
    )
  }

  suspend fun deleteLibraryFile(baseUrl: String, name: String) = withContext(Dispatchers.IO) {
    postJson("$baseUrl/api/library/delete", JSONObject().put("name", name))
  }

  suspend fun updateConfig(
    baseUrl: String,
    outDir: String? = null,
    defaultQuality: String? = null,
    mirror: String? = null,
    verify: Boolean? = null,
    bridge: String? = null,
    updateBridge: Boolean = false,
  ) = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      if (outDir != null) put("out_dir", outDir)
      if (defaultQuality != null) put("default_quality", defaultQuality)
      if (mirror != null) put("mirror", mirror)
      if (verify != null) put("verify", verify)
      if (updateBridge) {
        if (bridge.isNullOrBlank()) put("bridge", JSONObject.NULL)
        else put("bridge", bridge.trim())
      }
    }
    postJson("$baseUrl/api/config", body)
  }

  suspend fun saveCookie(baseUrl: String, cookie: String): AuthInfo = withContext(Dispatchers.IO) {
    val root = postJson("$baseUrl/api/auth/cookie", JSONObject().put("cookie", cookie))
    val a = root.optJSONObject("auth")
    AuthInfo(
      loggedIn = a?.optBoolean("logged_in", false) ?: false,
      uin = a?.optString("uin", "") ?: "",
      source = a?.optString("source", "") ?: "",
    )
  }

  suspend fun logout(baseUrl: String) = withContext(Dispatchers.IO) {
    postJson("$baseUrl/api/auth/logout", JSONObject())
  }

  suspend fun startLogin(baseUrl: String, kind: String) = withContext(Dispatchers.IO) {
    postJson("$baseUrl/api/login/start", JSONObject().put("kind", kind))
  }

  suspend fun cancelLogin(baseUrl: String) = withContext(Dispatchers.IO) {
    postJson("$baseUrl/api/login/cancel", JSONObject())
  }

  suspend fun loginStatus(baseUrl: String): LoginStatus = withContext(Dispatchers.IO) {
    val root = getJson("$baseUrl/api/login/status")
    val qr = root.optJSONObject("qr")
    LoginStatus(
      running = root.optBoolean("running", false),
      success = root.optBoolean("success", false),
      kind = root.optNullableString("kind"),
      canceled = root.optBoolean("canceled", false),
      error = root.optNullableString("error"),
      lines = root.optJSONArray("lines").toStringList(),
      qrUrl = qr?.optNullableString("url"),
    )
  }

  suspend fun downloadBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
    val req = Request.Builder().url(url).get().build()
    http.newCall(req).execute().use { resp ->
      if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
      resp.body?.bytes() ?: throw IOException("空响应")
    }
  }

  private fun getJson(url: String): JSONObject {
    val req = Request.Builder().url(url).get().build()
    return executeJson(req)
  }

  private fun postJson(url: String, body: JSONObject): JSONObject {
    val req = Request.Builder()
      .url(url)
      .post(body.toString().toRequestBody(jsonType))
      .build()
    return executeJson(req)
  }

  private fun executeJson(req: Request): JSONObject {
    http.newCall(req).execute().use { resp ->
      val text = resp.body?.string().orEmpty()
      val obj = try {
        JSONObject(text)
      } catch (_: Exception) {
        if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
        throw IOException("服务端返回非 JSON 数据")
      }
      if (!resp.isSuccessful || !obj.optBoolean("ok", true)) {
        val msg = obj.optString("error").ifBlank { "HTTP ${resp.code}" }
        throw IOException(msg)
      }
      return obj
    }
  }

  companion object {
    internal fun parseSong(o: JSONObject): SongItem = SongItem(
      mid = o.optString("songmid", o.optString("mid", "")),
      mediaMid = o.optString("media_mid", ""),
      name = o.optString("name", ""),
      artist = o.optString("artist", ""),
      album = o.optString("album", ""),
      albumMid = o.optString("album_mid", ""),
      coverUrl = o.optNullableString("cover_url"),
      remoteCoverUrl = o.optNullableString("remote_cover_url"),
      duration = o.optLong("duration", 0L),
      durationText = o.optString("duration_text", "00:00"),
      size128 = o.optLong("size_128", 0L),
      size320 = o.optLong("size_320", 0L),
      sizeFlac = o.optLong("size_flac", 0L),
      qualityFlags = o.optString("quality_flags", "-"),
    )

    internal fun parseJob(o: JSONObject): JobItem = JobItem(
      id = o.optLong("id", 0L),
      mid = o.optString("mid", ""),
      name = o.optString("name", ""),
      artist = o.optString("artist", ""),
      albumMid = o.optString("album_mid", ""),
      coverUrl = o.optNullableString("cover_url"),
      requestedQuality = o.optString("requested_quality", "320"),
      quality = o.optString("quality", "320"),
      qualityLabel = o.optString("quality_label", "320k MP3"),
      preview = o.optBoolean("preview", false),
      status = o.optString("status", "queued"),
      stage = o.optString("stage", ""),
      source = o.optString("source", ""),
      downloaded = o.optLong("downloaded", 0L),
      total = o.optLong("total", 0L),
      speed = o.optDouble("speed", 0.0),
      path = o.optNullableString("path"),
      fileName = o.optNullableString("file_name"),
      url = o.optNullableString("url"),
      error = o.optNullableString("error"),
      verified = if (o.isNull("verified")) null else o.optDouble("verified"),
      messages = o.optJSONArray("messages").toStringList(),
      elapsed = o.optDouble("elapsed", 0.0),
    )

    private fun JSONObject.optNullableString(key: String): String? =
      if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    private fun JSONArray?.toStringList(): List<String> {
      if (this == null) return emptyList()
      return buildList(length()) {
        for (i in 0 until length()) add(optString(i, ""))
      }
    }
  }
}
