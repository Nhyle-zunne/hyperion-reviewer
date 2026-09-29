# Hyperion Maritime CRP

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-0C0C0C?style=for-the-badge&logo=android&logoColor=3DDC84" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0+-0C0C0C?style=for-the-badge&logo=kotlin&logoColor=7F52FF" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-0C0C0C?style=for-the-badge&logo=jetpackcompose&logoColor=4285F4" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Architecture-Clean%20%2F%20MVVM-0C0C0C?style=for-the-badge" alt="Clean MVVM" />
  <img src="https://img.shields.io/badge/Licensure-MARINA%20%7C%20OIC--NW%20%7C%20GMDSS-0C0C0C?style=for-the-badge&color=FF5A67" alt="Licensure" />
</p>

An offline-first Android review and diagnostic terminal engineered for maritime deck officers and radio operators preparing for MARINA Competence Recognition Process (CRP), Officer-in-Charge of a Navigational Watch (OIC-NW), and GMDSS licensure examinations.

Built using an industrial, high-contrast bridge terminal interface ("Spaghetti UI") focused on low-latency drilling, intensive retention loops, and compliance readiness.

---

## Key Modules & Licensure Tracks

* **MARINA CRP Competence Engine:** Systematic breakdown of operational and management-level assessment matrices.
* **OIC-NW Question Banks:** Comprehensive deck officer bank coverage across Terrestrial/Celestial Navigation, Cargo Handling, Ship Handling, and Watchkeeping.
* **GMDSS Operations:** Radio communication procedures, distress frequencies, SOLAS protocols, and equipment fault identification.

---

## Technical Architecture

```text
com.nhyle.hyperionmaritime
├── data
│   ├── local        # Room Database, SQLite DAOs, pre-seeded JSON engines
│   └── repository   # Offline-first repositories with reactive Kotlin Flow
├── domain
│   ├── model        # Competence, Question, DrillConfiguration, LicenseTier
│   └── usecase      # Session generators, queue recycling, verification logic
├── ui
│   ├── components   # Spaghetti design terminal cards, monospace inputs, badges
│   ├── config       # Multi-step drill configuration & clearance overlays
│   ├── exam         # Active drill runner, queue dispatcher, diagnostics
│   └── theme        # Industrial palette (#0C0C0C, #141414, #FF5A67)
└── viewmodel        # Unidirectional StateFlow (MVI/MVVM) screen coordinators
```

### Engineering Highlights
* **High-Performance Room Search:** Instant debounced full-text search matching both questions, answer options (A/B/C/D), and rationale keys.
* **Intensive Mode Recycling Queue:** Dynamic queue manager that cycles incorrect items back into active drill sets until true candidate mastery is attained.
* **Granular Session Customization:** Full control over question order (Sequential/Randomized), option shuffling, and subset limits.
* **Clearance Gating:** Tier-based token validation system managing institutional seats without using non-compliant commercial in-app purchase triggers.

---

## Visual Design Specification

The UI strictly adheres to a high-contrast maritime bridge workstation design language:
* **Background Canvas:** `#0C0C0C` (Absolute Night)
* **Surface Containers:** `#141414` with `#2A2A2A` 1dp structural borders
* **Primary Accent:** `#FF5A67` (Night-vision tactical red)
* **Status Badges:** Sharp 2dp rounded container bounds, uppercase monospace typography (`[LOCKED]`, `[SEQUENTIAL]`, `[VERIFIED]`).

---

## Build & Setup

### Environment Requirements
* **Android Studio:** Ladybug / Iguana or later
* **JDK:** OpenJDK 17 or higher
* **Minimum Android Version:** Android 8.0 (API Level 26)
* **Target Android Version:** Android 14+ (API Level 34)

### Clone & Compile

```bash
# Clone the repository
git clone https://github.com/your-username/hyperion-maritime.git
cd hyperion-maritime

# Clean build directory
./gradlew clean

# Compile Debug APK
./gradlew :app:assembleDebug

# Compile Production Release Bundle (AAB)
./gradlew :app:bundleRelease --no-daemon --stacktrace
```

The compiled release bundle will be available at:
`app/build/outputs/bundle/release/app-release.aab`

## Release Signing & Deployment

Production builds are configured via Gradle Kotlin DSL (`app/build.gradle.kts`). To sign releases, configure your `release.keystore` in the root workspace or provide environment secrets for CI/CD runners:

```bash
keytool -genkey -v -keystore release.keystore -alias hyperion_release \
  -keyalg RSA -keysize 2048 -validity 10000
```

## Security & Privacy Compliance

1. **100% Offline Capability:** Candidate test histories, study logs, and search queries remain on the local device.
2. **Google Play Policy Compliant:** Free of commercial in-app purchase SDKs; candidate access is handled through institutional validation.
3. **Zero Analytics/Ad Trackers:** No third-party marketing SDKs or tracking pixels.

## License

Institutional / Candidate License. All exam matrices, item formats, and operational reference data are designed to comply with STCW 1978, as amended, and national maritime evaluation standards.
