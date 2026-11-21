plugins {
  alias(libs.plugins.maia.android.application)
  alias(libs.plugins.maia.android.compose)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"

  defaultConfig {
    applicationId = "app.heymaia"
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
    val releaseKeyFile = file(keystorePath)
    if (releaseKeyFile.exists()) {
      create("release") {
        storeFile = releaseKeyFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    }
    debug { signingConfig = signingConfigs.getByName("debug") }
  }

  buildFeatures {
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
  implementation(project(":core:model"))
  implementation(project(":core:contract"))
  implementation(project(":core:database"))
  implementation(project(":core:runtime"))
  implementation(project(":core:memory"))
  implementation(project(":core:character"))
  implementation(project(":core:design"))
  implementation(project(":feature:chat"))
  implementation(project(":feature:memoryview"))
  implementation(project(":feature:settings"))

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.androidx.work.runtime)
  implementation(libs.androidx.biometric)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
