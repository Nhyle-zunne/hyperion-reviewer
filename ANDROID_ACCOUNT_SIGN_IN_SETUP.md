# Android account sign-in setup

The Android app uses optional native Google sign-in with the existing Hyperion Supabase Auth project. It reads active rows from public.entitlements with the signed-in user's JWT, so the table's existing user_id row-level security policy remains in force.

## One-time Google OAuth setup

1. In Supabase Dashboard, open Authentication → Sign In / Providers → Google and note the Web OAuth client ID configured there.
2. In Google Cloud / Google Auth Platform, ensure an Android OAuth client is registered for package com.nhyle.hyperionmaritime and the SHA-1 fingerprints for the app's signing certificates. For Play releases, use the Play App Signing certificate SHA-1, not the upload-key certificate SHA-1.
3. Set HYPERION_GOOGLE_WEB_CLIENT_ID to the same Web OAuth client ID configured in Supabase. The ID is public; the OAuth client secret must never be included in the Android app.
4. Rebuild the Android app and test on a physical Android device.

For a local Gradle build, place this property in the developer's user-level ~/.gradle/gradle.properties file:

    HYPERION_GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com

For CI, set HYPERION_GOOGLE_WEB_CLIENT_ID as a GitHub Actions secret/environment value and expose it to Gradle as ORG_GRADLE_PROJECT_HYPERION_GOOGLE_WEB_CLIENT_ID.

## Google AI Studio (browser-based Android build)

Google AI Studio can build this native Kotlin/Jetpack Compose project in its browser. Android Studio is not required.

- The AI Studio browser emulator does not include Google Play services. Google Sign-In cannot be fully tested inside that emulator; test it on a physical Android phone instead.
- If the app is installed from a Google Play testing track, obtain the SHA-1 under Play Console → Test and release → App integrity → App signing key certificate. Register that SHA-1 in a Google Cloud Android OAuth client for package com.nhyle.hyperionmaritime. Do not substitute the upload-key certificate.
- If the app is installed directly using AI Studio's Install on Device flow, its signing certificate may differ from the Play-distributed app. Have the AI Studio agent run the Gradle signing report for the exact build being installed, or export that APK and inspect its certificate. Register that certificate SHA-1 as well if direct-install builds need Google Sign-In.
- Create a separate Web application OAuth client if one does not already exist. Its client ID must be the same ID entered in Supabase Authentication → Providers → Google. The Android client uses the app package name and signing-certificate SHA-1; the Web client ID is the server/audience ID used by Credential Manager.
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
