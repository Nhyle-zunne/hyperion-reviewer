# Image attachments in Hyperion Android question banks

The Android reviewer supports optional hash-addressed figure references in encrypted CSV question banks. This matches the companion PWA/admin format.

Each row can contain a quoted CSV cell in an optional `HYPERION_MEDIA` header column. The JSON value is an array of at most four objects, each with **exactly** these keys:
- `sha256`: lowercase SHA-256 hex digest of the actual image bytes
- `mime`: `image/webp`, `image/png`, or `image/jpeg`
- `placement`: `question` or `explanation`
- `alt`: short descriptive text (1-160 characters)

Example one-image value:
```json
[{"sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","mime":"image/webp","placement":"question","alt":"Example radar chart"}]
```

The actual images are NEVER packaged into CSV files, URLs are NEVER placed inside bank records, and premium image downloads are authenticated through the Supabase `question-media` Edge Function. Native images are stored in app-private `filesDir`, keyed by owner/track/SHA and verified on every read; local access also requires the current device entitlement.

Quick Scan and Practice show diagrams and can enlarge them with pinch-to-zoom. Explanation diagrams are shown only after answering in practice review, while Quick Scan already displays the answer by design.

**Sync caveat:** bank metadata is delivered through the existing encrypted question-bank format, but Android's `SMART_BANK_UPDATES_ENABLED` defaults **false**. A web admin edit does NOT instantly update an installed Android app. Issue a signed Android update with the refreshed encrypted CSV data, or separately validate and explicitly enable the authenticated smart-bank update route. Current users without an updated APK or remote bank update will not see new references. Uploaded images remain stored privately in Supabase, not in GitHub.

The feature adds no repeated license polling and no user progress sync writes. The first image download uses protected Supabase Storage bandwidth, then the local SHA-verified copy is reused while the licensed owner remains entitled. Clearing app data removes offline copies.
