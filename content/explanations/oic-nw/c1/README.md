# OIC-NW C1 explanations

All **831 C1 questions** now include a concise explanation in the encrypted bank used by hyperion-pwa. Published 12 November 2026 through the existing verified GitHub bank publisher.

The PWA uses its existing Explanation box after checking an answer; no layout or frontend code changed. Users with an older offline bank need to download the updated track bank. No native Android app release was created.

## Source and publication

- Bank revision: `217258a953e39f4b87b9e3cac56d68aabfb70540`.
- Publication commit: `fa0cb6b53539865926952d891622489d864a6cf6`.
- Authoring records: [explanations.json](explanations.json).
- Verification results: [verification.json](verification.json).
- [REVIEW.md](REVIEW.md), [pilot-001.json](pilot-001.json), and [variant-audit.json](variant-audit.json) preserve the earlier ten-question draft work. Their original draft labels describe that historical stage.

## Preserved content

All 834 CSV rows are retained. Only the explanation column changed: all 831 question IDs, stems, punctuation, choices, keys, retirement flags and content fingerprints are unchanged. The other 68 encrypted bank files are unchanged. CSV data is 454,022 bytes; the published encrypted file is 454,050 bytes.

Explanations follow the bank's keyed answer. Where wording, arithmetic or a legacy key is inconsistent with an underlying rule, the explanation gives a brief qualification instead of inventing a derivation. References support the concepts; they are not certification of every bank key.

## Checks

- Six writing batches covered the remaining 821 questions; three separate reviewers checked those batches. Seven explanation corrections were applied before release.
- Coverage, exact content/ID preservation, CSV field preservation and AES-GCM roundtrip passed.
- Existing publisher validation, transaction/idempotency, release-source, delivery/access-control and security checks passed.
- The live public free-sample response reports the new revision and matches the authored explanation. Anonymous full-bank access remains denied (HTTP 401).
- The current PWA publisher already reconciles C1 explanations on later uploads; no duplicate backend implementation was added.
- This change does not sign or publish an Android APK/AAB.
