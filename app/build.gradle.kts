import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.google.services)
}

val releaseKeystorePath = configuredSigningValue("KEYSTORE_PATH")
val releaseStorePassword = configuredSigningValue("STORE_PASSWORD")
val releaseKeyAlias = configuredSigningValue("KEY_ALIAS") ?: "upload"
val releaseKeyPassword = configuredSigningValue("KEY_PASSWORD")
val releaseSigningConfigured = releaseKeystorePath != null &&
  releaseStorePassword != null &&
  releaseKeyPassword != null

// Signing credentials must come from local user properties or CI secrets, never source control.
fun configuredSigningValue(name: String): String? =
  providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
    ?: System.getenv(name)?.takeIf { it.isNotBlank() }

android {
  namespace = "com.nhyle.hyperionmaritime"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.nhyle.hyperionmaritime"
    minSdk = 24
    targetSdk = 36
    versionCode = 64
    versionName = "1.0.7"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      if (releaseSigningConfigured) {
        storeFile = file(requireNotNull(releaseKeystorePath))
        storePassword = requireNotNull(releaseStorePassword)
        keyAlias = releaseKeyAlias
        keyPassword = requireNotNull(releaseKeyPassword)
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = if (releaseSigningConfigured) signingConfigs.getByName("release") else null
      ndk {
        debugSymbolLevel = "FULL"
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }


  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }
}


// Refuse release packaging/signing when credentials are missing. Debug builds do not need
// production signing material and use the Android Gradle plugin's generated debug key.
tasks.matching {
  it.name in setOf(
    "assembleRelease",
    "bundleRelease",
    "packageRelease",
    "packageReleaseBundle",
    "signReleaseBundle"
  )
}.configureEach {
  doFirst {
    if (!releaseSigningConfigured) {
      throw GradleException(
        "Release signing is not configured. Supply KEYSTORE_PATH, STORE_PASSWORD, and " +
          "KEY_PASSWORD through secure Gradle user properties or environment variables. " +
          "Do not use a keystore or default password stored in source control."
      )
    }
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation("androidx.compose.material:material-icons-extended")
  implementation(libs.androidx.navigation.compose)

  // Newer fragment library (Play recommendation)
  implementation("androidx.fragment:fragment-ktx:1.8.9")

  // Room Database
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  ksp("androidx.room:room-compiler:2.6.1")

  // WorkManager & Security
  implementation("androidx.work:work-runtime-ktx:2.9.1")
  implementation("androidx.security:security-crypto:1.1.0-alpha06")

  // Google Play Billing & In-App Updates & Reviews
  implementation("com.android.billingclient:billing-ktx:8.0.0")
  implementation("com.google.android.play:app-update:2.1.0")
  implementation("com.google.android.play:app-update-ktx:2.1.0")
  implementation("com.google.android.play:review:2.0.2")
  implementation("com.google.android.play:review-ktx:2.0.2")

  // Firebase & Google Play Services
  implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
  implementation("com.google.firebase:firebase-config-ktx")
  implementation("com.google.firebase:firebase-analytics-ktx")
  implementation("com.google.firebase:firebase-common-ktx")
  implementation("com.google.android.gms:play-services-tasks:18.2.0")

  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation("androidx.compose.ui:ui-test-manifest")

  // Testing
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.robolectric:robolectric:4.14.1")
  testImplementation("androidx.test:core:1.6.1")
  testImplementation("androidx.test.ext:junit:1.2.1")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
  testImplementation("androidx.compose.ui:ui-test-junit4")
}
