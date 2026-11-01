import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.google.services)
}

googleServices {
  // This project can be run locally without Firebase features/configuration.
  // Add a valid app/google-services.json only when a Firebase-backed feature is enabled.
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}

// This is the public OAuth Web client ID used by Credential Manager to request
// Google ID tokens for Supabase Auth. Keep it configurable for environments/projects,
// but include the production default so AI Studio direct builds cannot silently omit it.
val googleWebClientId = providers.gradleProperty("HYPERION_GOOGLE_WEB_CLIENT_ID").orNull
  ?.takeIf { it.isNotBlank() }
  ?: System.getenv("HYPERION_GOOGLE_WEB_CLIENT_ID")?.takeIf { it.isNotBlank() }
  ?: "831625838811-nakuna5j6odd0qf4pc6odquvad3n4j3j.apps.googleusercontent.com"
val escapedGoogleWebClientId = googleWebClientId.replace("\\", "\\\\").replace("\"", "\\\"")

// Release artifacts must be signed only with credentials supplied outside version control.
// Deliberately do not fall back to the repository's tracked keystore or the default
// "android" password values when those credentials are missing.
fun configuredSigningValue(name: String): String? =
  providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
    ?: System.getenv(name)?.takeIf { it.isNotBlank() }

val releaseKeystorePath = configuredSigningValue("KEYSTORE_PATH")
val releaseStorePassword = configuredSigningValue("STORE_PASSWORD")
val releaseKeyAlias = configuredSigningValue("KEY_ALIAS") ?: "upload"
val releaseKeyPassword = configuredSigningValue("KEY_PASSWORD")
val releaseSigningConfigured = releaseKeystorePath != null &&
  releaseStorePassword != null &&
  releaseKeyPassword != null

android {
  namespace = "com.nhyle.hyperionmaritime"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.nhyle.hyperionmaritime"
    buildConfigField("String", "SUPABASE_URL", "\"https://evnjoezffghtrzqacwcj.supabase.co\"")
    buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"sb_publishable_3QAooJa46k2g9PaUNbTOLA_1UXxAIeR\"")
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$escapedGoogleWebClientId\"")
    minSdk = 24
    targetSdk = 36
    versionCode = 65
    versionName = "2.0.0"

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
      // Use Gradle's standard debug keystore so CI and developer machines do not
      // depend on a generated keystore being tracked in the repository.
      signingConfig = signingConfigs.getByName("debug")
    }
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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

// Allow cloud publisher and CI to build release bundles when signing credentials are
// managed externally (e.g. Google Play App Signing or cloud publisher environment).
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
      logger.warn(
        "Release signing is not configured. An unsigned artifact will be generated unless " +
          "KEYSTORE_PATH, STORE_PASSWORD, and KEY_PASSWORD are provided."
      )
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17")
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
  implementation("androidx.room:room-runtime:2.8.5")
  implementation("androidx.room:room-ktx:2.8.5")
  ksp("androidx.room:room-compiler:2.8.5")

  // Backport java.time APIs for the app's minSdk 24 (Android 7.x) devices.
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

  // WorkManager & Security
  implementation("androidx.work:work-runtime-ktx:2.9.1")
  implementation("androidx.security:security-crypto:1.1.0-alpha06")

  // Native Google sign-in via Android Credential Manager
  implementation("androidx.credentials:credentials:1.6.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

  // Google Play Billing & In-App Updates & Reviews
  implementation("com.android.billingclient:billing-ktx:8.0.0")
  implementation("com.google.android.play:app-update:2.1.0")
  implementation("com.google.android.play:app-update-ktx:2.1.0")
  implementation("com.google.android.play:review:2.0.2")
  implementation("com.google.android.play:review-ktx:2.0.2")

  // Firebase & Google Play Services
  implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
  implementation("com.google.firebase:firebase-config-ktx")
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
