# Android account sign-in setup

The Android app uses optional native Google sign-in with the existing Hyperion Supabase Auth project. It reads active rows from public.entitlements with the signed-in user's JWT, so the table's existing user_id row-level security policy remains in force.

## One-time Google OAuth setup

1. In Supabase Dashboard, open Authentication → Sign In / Providers → Google and note the existing Web OAuth client ID. Do not replace the existing website OAuth client.
2. In the same Google Cloud project, create Android OAuth clients for every signing certificate that must be supported. Use the exact package name com.nhyle.hyperionmaritime and the SHA-1 values listed below.
3. Configure the Supabase Google provider Client IDs field with the Web client ID first, followed by the Android client IDs, comma-separated. Keep the existing Google client secret unchanged. Supabase documents this multi-client format and requires the Web client ID first.
4. Set HYPERION_GOOGLE_WEB_CLIENT_ID in the Android Gradle build to the Web OAuth client ID (the first client ID above), not an Android client ID and not the client secret.
5. Rebuild the Android app and test Google sign-in on a physical Android device.

The Web client ID is a public identifier intended to be included in the native application. The OAuth client secret must never be embedded in the Android app, committed to source control, or pasted into AI Studio prompts.

For a local Gradle build, place this property in the developer's user-level ~/.gradle/gradle.properties file:

    HYPERION_GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com

For CI, set HYPERION_GOOGLE_WEB_CLIENT_ID as an Actions secret/environment value and expose it to Gradle as ORG_GRADLE_PROJECT_HYPERION_GOOGLE_WEB_CLIENT_ID.

## Verified Hyperion signing fingerprints

These fingerprints were supplied from the user's actual signing reports on October 30, 2026. Both Android OAuth clients use package com.nhyle.hyperionmaritime.

| Build / distribution | Signing certificate SHA-1 | Google Cloud Android OAuth client |
|---|---|---|
| Google AI Studio Install on Device (debug APK, /root/.android/debug.keystore, AndroidDebugKey) | 24:55:65:8E:67:8D:09:91:43:D6:58:C2:7B:57:B0:B5:DB:53:DA:0B | Create one Android client for this SHA-1 |
| Google Play distribution (Play App Signing key from Play Console) | 75:D4:82:65:86:F8:D9:86:EB:F1:32:40:2D:E1:3D:86:9C:AF:42:3D | Create a separate Android client for this SHA-1 |

Do not use the Play upload-key fingerprint in place of the Play App Signing fingerprint. The upload key and app-signing key serve different purposes.

## Google AI Studio (browser-based Android build)

Google AI Studio can build this native Kotlin/Jetpack Compose project in its browser. Android Studio is not required.

- The AI Studio browser emulator does not include Google Play services. Google Sign-In cannot be fully tested inside that emulator; test it on a physical Android phone instead.
- AI Studio's Install on Device flow currently produced a debug APK signed with /root/.android/debug.keystore. Its verified SHA-1 is listed above. If AI Studio regenerates or changes that signing key, rerun signingReport and update the Android OAuth client.
- AI Studio can also publish builds to the Play internal-testing track using its managed signing process. Play-distributed builds use the Play App Signing certificate displayed in Play Console; register that fingerprint separately.
- Create a separate Web application OAuth client if one does not already exist. Its client ID must be the same ID entered in Supabase Authentication → Providers → Google and in HYPERION_GOOGLE_WEB_CLIENT_ID. The Android clients are registered against their package name and certificate SHA-1.
- In the AI Studio Android project, configure the public Web Client ID as HYPERION_GOOGLE_WEB_CLIENT_ID for the app's Gradle BuildConfig before rebuilding. Do not paste the Google OAuth client secret, Supabase service-role key, or Play service-account JSON into Android source or AI Studio prompts.

## Behavior and security

- Signing in is optional. Existing Google Play Billing access, device-bound license activation, and normal offline reviewer behavior remain available without an account.
- Account purchases are checked with the authenticated session; the app does not send a client-supplied user ID to decide which entitlements are owned.
- Website entitlements are stored in Android encrypted preferences and are accepted offline for no more than 72 hours since the last successful sync, and never beyond the server-provided expiry time.
- Signing out clears only the account session and account-entitlement cache. It does not cancel or clear Google Play purchases or the existing device-key system.
- Google Play purchase linking, the authenticated link-google-play-subscription Edge Function, and periodic subscription reconciliation are deployed in the existing Supabase project. Android sign-in and purchase linking still require the Google OAuth client configuration described above. Real Google Pub/Sub push delivery remains an outstanding acceptance check; periodic reconciliation is the recovery mechanism if an RTDN notification is missed.

## Release signing must be configured securely

The Gradle release build no longer falls back to a repository keystore or default passwords. Before requesting a release APK/AAB, supply these values through environment variables or the developer's user-level ~/.gradle/gradle.properties file (never a repository-committed Gradle properties file):

    KEYSTORE_PATH=/absolute/path/to/your-upload-keystore.jks
    STORE_PASSWORD=your-keystore-password
    KEY_ALIAS=upload
    KEY_PASSWORD=your-key-password

The alias defaults to upload if KEY_ALIAS is omitted. Keep the keystore and passwords outside source control. Debug builds and the CI debug build do not require release-signing credentials.

**Important keystore review (October 30, 2026):** Keystore files and an exported upload certificate were accidentally present in the current GitHub main tree, and the old keystore was tracked on this feature branch. They have been removed from the current working trees, and the repository .gitignore now excludes keystores, exported certificates, Firebase config, and generated APK/AAB artifacts. This does not remove copies from earlier Git commits. Anyone with repository-history access may still be able to retrieve them, so treat any keystore that was committed as potentially exposed. Before depending on the replacement upload key for a Play release, restrict repository access and decide whether to purge the affected Git history or generate/register another replacement upload key. Never commit the new .jks, any Base64 copy, or signing passwords; provide them only through GitHub Actions secrets or developer-local secure configuration.

## Before releasing to Google Play

Review the Play Console Data safety answers and Privacy Policy to disclose account sign-in and processing/storage of account email, authentication session, and account-linked license entitlement data. Never ship a Supabase service-role key or a Google OAuth client secret in the Android app.

## Google Play subscription linking

The signed-in Android app re-queries owned Play subscriptions and submits each purchase token to the authenticated link-google-play-subscription Edge Function. The function verifies tokens with Google's purchases.subscriptionsv2.get endpoint before creating a separate Google Play entitlement; it never overwrites a website checkout or admin-issued entitlement.

The backend uses the GOOGLE_PLAY_SERVICE_ACCOUNT_JSON Supabase Edge Function secret containing a service account JSON authorized for the Android Publisher API in Play Console. Do not put that JSON, private key or OAuth client secret in the Android app.

Linking is an explicit authenticated account association: existing users open the Android app on the Google Play account that owns the subscription, sign into their Hyperion account, then tap Sync. A Play purchase already linked to another Hyperion account is rejected rather than silently moved.
