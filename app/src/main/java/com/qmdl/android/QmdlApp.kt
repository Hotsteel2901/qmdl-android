package com.qmdl.android

import android.app.Application
import com.qmdl.android.backend.QmdlBackendManager
import com.qmdl.android.data.AppPreferences
import com.qmdl.android.data.QmdlApiClient
import com.qmdl.android.player.AudioPlayerController

class QmdlApp : Application() {
  lateinit var prefs: AppPreferences
    private set
  lateinit var api: QmdlApiClient
    private set
  lateinit var backendManager: QmdlBackendManager
    private set
  lateinit var playerController: AudioPlayerController
    private set

  override fun onCreate() {
    super.onCreate()
    prefs = AppPreferences(this)
    api = QmdlApiClient()
    backendManager = QmdlBackendManager(this)
    playerController = AudioPlayerController(this)

    if (prefs.useRemoteBackend) {
      backendManager.useRemote(prefs.remoteUrl)
    } else {
      backendManager.startLocal(prefs.quality.slug, prefs.mirror.slug)
    }
  }
}
