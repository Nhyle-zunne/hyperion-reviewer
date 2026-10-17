
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

### Compilation Commands

Bash

```
# Clean build workspace
./gradlew clean

# Compile Debug APK
./gradlew :app:assembleDebug

# Compile Production Release Bundle (AAB)
./gradlew :app:bundleRelease --no-daemon --stacktrace
```

The compiled release bundle will be generated at: `app/build/outputs/bundle/release/app-release.aab`

## Security & Billing Compliance

1. **Dual Licensing Pipeline:** Full support for Google Play auto-renewing subscriptions (Weekly, Monthly, Yearly) along with hardware-bound offline activation keys (`HYPE-XXXX-XXXX-XXXX`).
2. **Data Privacy & Zero Tracking:** Candidate test results, study streaks, and diagnostic records remain strictly on the local device. No advertising trackers, marketing SDKs, or external telemetry are embedded.
3. **Google Play Policy Compliant:** Clear subscription disclosures, transparent pricing matrices, and direct Google Play subscription management workflows.

The modified file list confirms the updates are in order[cite: 10]:
* `app/build.gradle.kts` contains the `versionName = "1.0.0"` update[cite: 10].
* `app/proguard-rules.pro` contains the closing bracket fixes[cite: 10].
* `metadata.json` reflects the 5-track scope[cite: 10].
* `my-upload-key.jks` is ready for release signing[cite: 10].
