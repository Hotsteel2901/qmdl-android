package com.qmdl.android.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

object MediaStoreExporter {

  private fun sanitizeFileName(raw: String): String {
    val cleaned = raw
      .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
      .trim()
      .trim('.')
    return cleaned.ifEmpty { "qmdl_track_${System.currentTimeMillis()}.mp3" }
  }

  private fun mimeTypeFor(fileName: String): String =
    when (fileName.substringAfterLast('.', "").lowercase()) {
      "flac" -> "audio/flac"
      "m4a", "mp4" -> "audio/mp4"
      "wav" -> "audio/wav"
      "ogg" -> "audio/ogg"
      else -> "audio/mpeg"
    }

  /**
   * Streaming audio export to system Music/QMDL directory using a 16KB buffer.
   * Avoids loading 50-100MB lossless FLAC files into a single heap ByteArray (prevents OOM).
   */
  suspend fun exportAudioStream(
    context: Context,
    fileName: String,
    input: InputStream,
  ): String = withContext(Dispatchers.IO) {
    val safeName = sanitizeFileName(fileName)
    val mime = mimeTypeFor(safeName)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, safeName)
        put(MediaStore.Audio.Media.MIME_TYPE, mime)
        put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/QMDL")
        put(MediaStore.Audio.Media.IS_MUSIC, 1)
        put(MediaStore.Audio.Media.IS_PENDING, 1)
      }
      val resolver = context.contentResolver
      val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IOException("无法写入系统音乐库")
      try {
        resolver.openOutputStream(uri)?.use { out ->
          input.copyTo(out, bufferSize = 16 * 1024)
          out.flush()
        } ?: throw IOException("无法打开输出流")
        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
      } catch (e: Exception) {
        runCatching { resolver.delete(uri, null, null) }
        throw e
      }
      "Music/QMDL/$safeName"
    } else {
      @Suppress("DEPRECATION")
      val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
      val dir = File(base, "QMDL").apply { mkdirs() }
      val target = File(dir, safeName)
      target.outputStream().use { out ->
        input.copyTo(out, bufferSize = 16 * 1024)
        out.flush()
      }
      MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mime), null)
      target.absolutePath
    }
  }

  suspend fun exportAudioBytes(
    context: Context,
    fileName: String,
    bytes: ByteArray,
  ): String = exportAudioStream(context, fileName, bytes.inputStream())

  suspend fun saveQrToGallery(
    context: Context,
    bytes: ByteArray,
    label: String = "qmdl_login_qr",
  ): String = withContext(Dispatchers.IO) {
    val fileName = sanitizeFileName("${label}_${System.currentTimeMillis()}.png")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QMDL")
        put(MediaStore.Images.Media.IS_PENDING, 1)
      }
      val resolver = context.contentResolver
      val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IOException("无法保存到相册")
      try {
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
          ?: throw IOException("无法写入图片")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
      } catch (e: Exception) {
        runCatching { resolver.delete(uri, null, null) }
        throw e
      }
      "Pictures/QMDL/$fileName"
    } else {
      @Suppress("DEPRECATION")
      val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
      val dir = File(base, "QMDL").apply { mkdirs() }
      val target = File(dir, fileName)
      target.writeBytes(bytes)
      MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("image/png"), null)
      target.absolutePath
    }
  }
}
