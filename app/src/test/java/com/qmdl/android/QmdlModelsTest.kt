package com.qmdl.android

import com.qmdl.android.data.QmdlApiClient
import com.qmdl.android.data.formatBytes
import com.qmdl.android.data.formatMillis
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QmdlModelsTest {

  @Test
  fun parsesSongAndCoverUrl() {
    val json = JSONObject(
      """
      {
        "songmid": "0039MnYb0qxYhV",
        "media_mid": "0039MnYb0qxYhV",
        "name": "晴天",
        "artist": "周杰伦",
        "album": "叶惠美",
        "album_mid": "000MkMni19ClKG",
        "cover_url": "/api/cover/000MkMni19ClKG",
        "duration": 269,
        "duration_text": "04:29",
        "size_128": 4300000,
        "size_320": 10700000,
        "size_flac": 30000000,
        "quality_flags": "320k/128k/FLAC"
      }
      """.trimIndent(),
    )
    val song = QmdlApiClient.parseSong(json)
    assertEquals("晴天", song.name)
    assertEquals("FLAC", song.bestBadge)
    assertEquals(
      "http://127.0.0.1:18088/api/cover/000MkMni19ClKG",
      song.resolvedCover("http://127.0.0.1:18088"),
    )
  }

  @Test
  fun parsesJobProgress() {
    val json = JSONObject(
      """
      {
        "id": 7,
        "mid": "0039MnYb0qxYhV",
        "name": "晴天",
        "artist": "周杰伦",
        "status": "downloading",
        "downloaded": 500,
        "total": 1000,
        "speed": 204800.0
      }
      """.trimIndent(),
    )
    val job = QmdlApiClient.parseJob(json)
    assertTrue(job.isActive)
    assertTrue(job.canCancel)
    assertEquals(0.5f, job.progressRatio ?: 0f, 0.001f)
  }

  @Test
  fun formatsBytesAndTime() {
    assertEquals("512 B", formatBytes(512))
    assertEquals("1.50 MB", formatBytes(1572864))
    assertEquals("4:29", formatMillis(269_000))
  }
}
