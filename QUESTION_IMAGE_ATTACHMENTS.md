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

Images are NEVER embedded in CSV files, and URLs are NEVER stored in bank records. The encrypted media files are stored alongside the encrypted questions under `app/src/main/assets/question-media/<TRACK>/<SHA>.<ext>.enc` and bundled directly into the APK. Android decrypts and verifies locally, only with an active entitlement for the image's track (or existing verified administrator access), including when the diagram belongs to a Free Reviewer question. Previously downloaded images in app-private `filesDir` remain a verified backward-compatible fallback. **Android does not make network requests to load question images.**

Quick Scan and Practice show diagrams and can enlarge them with pinch-to-zoom. Explanation diagrams are shown only after answering in practice review, while Quick Scan already displays the answer by design.

**Sync caveat:** bank metadata remains in the encrypted question-bank CSV, and `SMART_BANK_UPDATES_ENABLED` defaults **false**. Admin image uploads are encrypted and staged in the `hyperion-reviewer` GitHub repository; publication promotes images and banks together to `main`. An installed APK does **not** automatically receive new files. Issue a signed app update containing the new bank and media assets, or explicitly validate an update mechanism that delivers and verifies both before enabling it. No Supabase Storage is used for new image uploads, publishing or Android image loading.

The feature adds no repeated license polling, no user progress sync writes and **zero image-delivery network traffic** on Android. APK-bundled encrypted media remains available offline only while the matching track entitlement is active. Free Reviewer questions remain accessible, but their images are locked without a license. Clearing app data only removes the optional legacy private-file cache, not the encrypted images packaged in the app.

## Internal test of Android bank synchronization

The optional signed internal-testing workflow now offers an explicit
`enable_smart_bank_updates` checkbox (default **off**). When selected, only
that internal AAB has the verified incremental bank updater enabled.

Test with a signed-in **licensed** account: purchase or grant a track,
tap **Sync License**, confirm the Wi-Fi-only bank-refresh worker completes,
**close and reopen the app** to reload the in-memory question bank, then open
a bank with newly attached figures and test airplane mode and
offline fallback. Repeat using a free account (no bank download), expired
license, logout/account switch and a bank whose checksum changes mid-download.

A successful GitHub CI compile is not proof of a working Play Store update,
and the workflow does not upload to Google Play automatically. **Do not
enable the flag for standard production builds** without this device-level
license, protected CDN and rollback verification. Internal test builds may
use the workflow's signed AAB artifact with existing Play upload credentials.

## Reader recovery and access changes

The shared native gallery is used by Quick Scan, Practice, Mock Competence,
Free Practice, Flashcards, Search, Bookmarks, Mistakes, Results and Session Review.
Question figures follow the question; explanation figures follow the screen's
existing answer/review visibility rules. The same image hash may appear at both
placements with separate alternative text.

An image attached to a Free Reviewer question is still a premium image. A license for another track is insufficient. Gallery images and open zoom dialogs reset when the question's references,
track, signed-in owner or access change. Existing entitlement events update the
view immediately; a local check every second also catches expiry while a
figure is open. These checks do not contact the license server or download
additional images. Bitmap decoding runs off the UI thread. Zoom supports both
pinch gestures and labeled buttons, with the image clipped inside the dialog.

A failed session lookup, cache read or download leaves the text question usable.
Tap **Retry** after reconnecting or repairing access to retry missing images
without leaving the question. Superseded work cannot publish a decoded bitmap;
the loader preserves coroutine cancellation and checks access again before
returning downloaded bytes. Blocking HTTP operations retain their existing
15-second connection and 20-second read timeouts.

Automated reader regressions use generated synthetic PNGs in the real private
cache. They cover owner switch, access revocation, expiry without a license
update event, revised references, track changes, Retry, hash tampering, zoom
reset and one image at both placements. Physical-device pinch/TalkBack behavior
and live licensed network delivery still require device acceptance testing.

## Image-only prompts and saved progress

Native numbered CSV readers and normalized GMDSS readers accept a blank question
stem only when `HYPERION_MEDIA` contains a validated `question` image reference.
Missing, malformed or explanation-only media cannot supply the prompt. Explicitly
headed normalized GMDSS files also support one- and two-question banks. Existing
question IDs, option order and correct-answer indices keep their existing rules.

Active-session snapshots, mistake entries and cloud bookmark snapshots preserve
image references. The Room version-7-to-8 migration adds the image-reference
column with an empty-array default and preserves existing questions and bookmarks.
Image-only saved questions require a valid prompt image when restored.

## Current media pipeline

Admin uploads are authenticated through the existing PWA question-bank administration gate. Images are encrypted and staged in GitHub; validated publishing commits encrypted media files to this Android repository alongside encrypted bank updates. The Android app loads those bundled encrypted bytes directly from the APK and SHA-verifies each image. Every image requires a valid local entitlement for that track; Free Reviewer references no longer unlock images. Unlicensed readers display **UNLOCK FULL REVIEWER TO VIEW IMAGES** instead of the diagram. The existing Supabase Auth/licensing system is separate from media storage and image delivery.
