package com.qmdl.android.data

import android.content.Context
import org.json.JSONArray

class AppPreferences(context: Context) {
  private val prefs = context.getSharedPreferences("qmdl_prefs", Context.MODE_PRIVATE)

  var useRemoteBackend: Boolean
    get() = prefs.getBoolean("use_remote", false)
    set(v) = prefs.edit().putBoolean("use_remote", v).apply()

  var remoteUrl: String
    get() = prefs.getString("remote_url", "http://192.168.1.100:8088") ?: "http://192.168.1.100:8088"
    set(v) = prefs.edit().putString("remote_url", v.trim().trimEnd('/')).apply()

  var quality: QualityOption
    get() = QualityOption.fromSlug(prefs.getString("quality", "320"))
    set(v) = prefs.edit().putString("quality", v.slug).apply()

  var mirror: MirrorOption
    get() = MirrorOption.fromSlug(prefs.getString("mirror", "auto"))
    set(v) = prefs.edit().putString("mirror", v.slug).apply()

  var previewOnly: Boolean
    get() = prefs.getBoolean("preview_only", false)
    set(v) = prefs.edit().putBoolean("preview_only", v).apply()

  var fallbackPreview: Boolean
    get() = prefs.getBoolean("fallback_preview", true)
    set(v) = prefs.edit().putBoolean("fallback_preview", v).apply()

  var verifyDuration: Boolean
    get() = prefs.getBoolean("verify_duration", true)
    set(v) = prefs.edit().putBoolean("verify_duration", v).apply()

  var themeMode: ThemeMode
    get() = ThemeMode.fromKey(prefs.getString("theme_mode", "system"))
    set(v) = prefs.edit().putString("theme_mode", v.key).apply()

  var dynamicColor: Boolean
    get() = prefs.getBoolean("dynamic_color", true)
    set(v) = prefs.edit().putBoolean("dynamic_color", v).apply()

  var autoExportMediaStore: Boolean
    get() = prefs.getBoolean("auto_export_music", false)
    set(v) = prefs.edit().putBoolean("auto_export_music", v).apply()

  fun searchHistory(): List<String> {
    val raw = prefs.getString("search_history", "[]") ?: "[]"
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val s = arr.optString(i)
          if (s.isNotBlank()) add(s)
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun pushSearchHistory(query: String): List<String> {
    val clean = query.trim()
    if (clean.isEmpty()) return searchHistory()
    val updated = (listOf(clean) + searchHistory().filter { it != clean }).take(12)
    prefs.edit().putString("search_history", JSONArray(updated).toString()).apply()
    return updated
  }

  fun clearSearchHistory() {
    prefs.edit().putString("search_history", "[]").apply()
  }
}
