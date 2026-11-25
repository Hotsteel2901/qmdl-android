plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.qmdl.android"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.qmdl.android"
    minSdk = 26
    targetSdk = 36
    versionCode = 2
    versionName = "2.0.0"

    vectorDrawables.useSupportLibrary = true
    ndk {
      abiFilters += listOf("arm64-v8a", "x86_64")
    }
  }

  packaging {
    jniLibs {
      // Required so Android extracts libqmdl.so (our embedded Rust CLI backend)
      // to nativeLibraryDir with execute permissions on Android 8..16.
      useLegacyPackaging = true
    }
  }

  signingConfigs {
    val ksPath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/release.jks"
    if (file(ksPath).exists()) {
      create("release") {
        storeFile = file(ksPath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    }
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions {
    unitTests.all {
      it.maxHeapSize = "1g"
    }
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.kyant.backdrop)
  implementation(libs.kyant.shapes)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.coil.compose)
  implementation(libs.okhttp)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.junit)
  testImplementation(libs.org.json)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
