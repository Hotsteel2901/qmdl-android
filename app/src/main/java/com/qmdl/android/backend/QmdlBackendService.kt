package com.qmdl.android.backend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.qmdl.android.MainActivity
import com.qmdl.android.QmdlApp
import com.qmdl.android.R

class QmdlBackendService : Service() {

  override fun onCreate() {
    super.onCreate()
    ensureChannel()
    val notification = buildNotification("内置下载引擎运行中")
    val started = runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    }.isSuccess
    if (!started) {
      // Android 12-16 may block background FGS starts; stop gracefully instead of crashing
      stopSelf()
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      (application as? QmdlApp)?.backendManager?.stopLocal()
      runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
      stopSelf()
      return START_NOT_STICKY
    }
    val app = application as? QmdlApp
    if (app != null && !app.prefs.useRemoteBackend) {
      app.backendManager.startLocal(app.prefs.quality.slug, app.prefs.mirror.slug)
    }
    // START_NOT_STICKY prevents Android 12+ ForegroundServiceStartNotAllowedException on OS restart
    return START_NOT_STICKY
  }

  /**
   * Required on Android 15/16 (targetSdk 35+) for foregroundServiceType="dataSync":
   * when the 6-hour OS quota expires, gracefully stop the service instead of throwing RemoteServiceException.
   */
  override fun onTimeout(startId: Int, fgsType: Int) {
    (application as? QmdlApp)?.backendManager?.stopLocal()
    runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
    stopSelf(startId)
  }

  override fun onDestroy() {
    runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun ensureChannel() {
    val mgr = getSystemService(NotificationManager::class.java) ?: return
    val channel = NotificationChannel(
      CHANNEL_ID,
      "QMDL 后端服务",
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = "保持本地 qqmusic-dl 下载引擎在后台运行"
      setShowBadge(false)
    }
    mgr.createNotificationChannel(channel)
  }

  private fun buildNotification(text: String): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pending = PendingIntent.getActivity(
      this,
      0,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_qmdl)
      .setContentTitle("QMDL 音乐下载器")
      .setContentText(text)
      .setOngoing(true)
      .setContentIntent(pending)
      .build()
  }

  companion object {
    private const val CHANNEL_ID = "qmdl_backend"
    private const val NOTIFICATION_ID = 1001
    const val ACTION_STOP = "com.qmdl.android.STOP_BACKEND"

    fun start(context: Context) {
      val intent = Intent(context, QmdlBackendService::class.java)
      runCatching {
        ContextCompat.startForegroundService(context, intent)
      }
    }
  }
}
