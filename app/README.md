
# Hyperion Maritime Licensure Reviewer

![Android](https://img.shields.io/badge/Platform-Android-0C0C0C?style=for-the-badge&logo=android&logoColor=3DDC84)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-0C0C0C?style=for-the-badge&logo=kotlin&logoColor=7F52FF)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-0C0C0C?style=for-the-badge&logo=jetpackcompose&logoColor=4285F4)
![Clean MVVM](https://img.shields.io/badge/Architecture-Clean%20%2F%20MVVM-0C0C0C?style=for-the-badge)
![STCW Compliance](https://img.shields.io/badge/STCW-Deck%20%7C%20Engine%20%7C%20Radio-0C0C0C?style=for-the-badge&color=FF5A67)

An enterprise-grade, offline-first Android review and diagnostic terminal engineered for Filipino Merchant Marine Officers and GMDSS Radio Operators preparing for official licensure and STCW Competence Recognition examinations. Built using an industrial high-contrast bridge terminal interface ("Spaghetti UI") designed for low-latency drilling, mistake remediation, and complete offline autonomy at sea.

## Key Licensure Tracks (Operational & Management)

* **Deck Operational (OIC-NW):** Full STCW Regulation II/1 coverage across 12 competencies (6,445 items), timed mock examinations, and rationales.
* **GOC Radio Operator (GMDSS):** STCW Regulation IV/2 radio communication protocols, distress frequencies, and SOLAS operating procedures across 2 competencies (2,260 items).
* **Engine Operational (OIC-EW):** STCW Regulation III/1 marine engineering competencies (13 competencies), electrical systems, and auxiliary machinery diagnostics.
* **Deck Management Level:** STCW Regulation II/2 advanced navigation, ship handling, stability, and maritime law across 18 competencies.
* **Engine Management Level:** STCW Regulation III/2 marine engineering management, automation, propulsion plant, and troubleshooting across 14 competencies.

## Technical Architecture

```text
com.nhyle.hyperionmaritime
├── data
│   ├── billing      # Google Play Billing 8.0.0 integration & sea-voyage persistence cache
│   ├── local        # Room database, SQLite DAOs, pre-seeded CSV question banks
│   ├── model        # Domain models, Track enum, Legal texts, and licensing tiers
│   └── util         # 3-Layer anti-tamper engine & cryptographic hardware token validation
├── ui
│   ├── components   # Monospace terminal HUD elements, status cards, and custom buttons
│   ├── screens      # Compose screens (Search, Store, Streak, History, Practice, Terms)
│   └── theme        # High-contrast night-vision palette (#0C0C0C canvas, #FF5A67 / #00FF66)
└── viewmodel        # Unidirectional StateFlow (MVI/MVVM) screen coordinators
```

### Engineering Highlights

- **100% Maritime Offline Persistence:** Subscriptions and offline pass keys are anchored locally to allow uninterrupted study during deep-sea voyages without requiring active internet connectivity.
- **Multi-Track License Coexistence:** Independent per-track licensing allows candidates to unlock individual modules or bundle passes without entitlement collision.
- **Auto-Healing Anti-Tamper Engine:** 3-layer security system that protects against system clock rollback manipulation while automatically recovering if a runaway system clock is restored.
- **High-Performance Full-Text Search:** Instant debounced search querying question prompts, choices (A/B/C/D), and rationales with composite keys preventing recomposition stutter.

## Build & Environment Specifications

- **Android Studio:** Ladybug / Iguana or later
- **JDK:** OpenJDK 17
- **Minimum SDK:** Android 7.0 (API Level 24)
- **Target / Compile SDK:** Android 16 (API Level 36)
- **Kotlin:** 2.0+ with Jetpack Compose Compiler plugin

### Run and test directly from Android Studio (recommended)

1. Open the repository's root folder (`HYPERION-REVIEWER`) in Android Studio, then allow Gradle sync to finish.
2. Set **Gradle JDK** to JDK 17 and install **Android SDK Platform 36** in SDK Manager.
3. Select the `app` run configuration.
4. Select an Android emulator or connect your Android phone with Developer options and USB debugging enabled.
5. Press **Run ▶**. Android Studio builds, installs, and launches the debug app on the selected target automatically.

For normal development, there is no need to download an APK, copy it to your phone, or install it manually. Use **Git → Pull** to receive source updates after they are merged into `main`, then press Run again. Android Studio will rebuild only what is needed.

The debug build uses Gradle's standard local debug signing key. It does not need the Google Play upload keystore.

The current app does not require a Firebase project file to compile or run its local/debug flow. The Google Services Gradle plugin is configured to warn when `app/google-services.json` is absent. That file is intentionally excluded from Git; add a valid project-specific file locally only when a Firebase-backed feature is enabled. Never commit it.

To enable native Google account sign-in in a local build, add the Web OAuth client ID to your user-level `~/.gradle/gradle.properties` as described in [ANDROID_ACCOUNT_SIGN_IN_SETUP.md](../ANDROID_ACCOUNT_SIGN_IN_SETUP.md). Do not put an OAuth client secret there or in the app.

### Optional command-line checks

```bash
# Run unit and Robolectric tests
./gradlew :app:testDebugUnitTest

# Optional clean build
./gradlew clean
```

The signed release App Bundle is a separate, manual release task. It is not part of the normal Android Studio Run workflow.

## Security & Billing Compliance

1. **Dual Licensing Pipeline:** Full support for Google Play auto-renewing subscriptions (Weekly, Monthly, Yearly) along with hardware-bound offline activation keys (`HYPE-XXXX-XXXX-XXXX`).
2. **Data Privacy & Zero Tracking:** Candidate test results, study streaks, and diagnostic records remain strictly on the local device. No advertising trackers, marketing SDKs, or external telemetry are embedded.
3. **Google Play Policy Compliant:** Clear subscription disclosures, transparent pricing matrices, and direct Google Play subscription management workflows.

## Release signing and Google Play Internal testing

Release signing is fail-closed: `app/build.gradle.kts` will not package a release unless signing credentials are explicitly supplied outside source control. For a local release build, configure these values in your developer-local Gradle properties file (for example `~/.gradle/gradle.properties`) or environment variables:

```properties
KEYSTORE_PATH=/absolute/path/to/your-upload-keystore.jks
STORE_PASSWORD=your-keystore-password
KEY_ALIAS=upload
KEY_PASSWORD=your-key-password
```

Never put actual values in a repository-committed file. The debug build uses Android Gradle Plugin's generated debug key; do not commit a custom debug keystore.

The GitHub Actions workflow **Android internal-testing App Bundle** builds and verifies a signed `.aab` using repository Actions secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `GOOGLE_SERVICES_JSON`, and `HYPERION_GOOGLE_WEB_CLIENT_ID`. It writes the keystore only to the temporary runner directory and uploads the signed bundle as an artifact. To build the account-linking feature, select branch `feature/android-account-signin-and-web-licenses` in the workflow's **Run workflow** dropdown; this does not merge or publish that branch automatically.

Signing files, Firebase configuration, exported certificates, APKs, and AABs are excluded by `.gitignore`. A file removed in a later commit can still exist in older Git history. Do not put a private keystore or password into source control; restrict repository-history access and decide whether history cleanup is needed before relying on a signing key that was previously committed.

Before uploading to Play Console, verify the new upload-key reset is effective and that the bundle's version code is greater than the latest version already accepted for this app.
