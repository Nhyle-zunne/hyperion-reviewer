# Question image reader fixes

Scope: improve existing native attachment readers without changing the shared CSV contract, media API, bank assets, entitlement rules, default smart-bank flag, signing or releases.

1. Add isolated Robolectric/Compose regressions using generated 2x2 PNG bytes in the real owner/track/hash cache. Exercise owner switch, revocation, revised references, track switch, Retry, and one hash at both placements. Run the unchanged gallery against them in unsigned Android CI to establish failures.
2. Key gallery state and zoom to question ID, track, placement, complete references, owner and access. Observe existing entitlement revisions and local expiry while visible. Decode bounded bitmaps on the Default dispatcher; cancel superseded work and recheck access before publishing. Add an explicit Retry and accessible zoom controls.
3. Preserve loader cancellation, check cancellation during bounded reads and before cache publication/return, and recheck owner/access after cache writes. Keep downloads on demand and bounded to 512 KiB.
4. Audit all question screen call sites; add only actual missing gallery placements. Update attachment guidance with implemented behavior and CI evidence.
5. Run full unit, lint and debug assemble checks using GitHub Android CI because this workspace lacks Android SDK/Gradle. Review diff, push focused branch and open PR; no merge or release.

Verification evidence will be recorded below after execution.

## Implemented scope and evidence

- `QuestionImageGallery.kt`: keys in-memory images, failures and zoom to ID/track/placement/full references/owner; entitlement event resets plus one-second local expiry checks; Default-dispatcher decoding; missing-image Retry; labeled and bounded zoom controls.
- `VerifiedQuestionMediaStore.kt`: unavailable session storage fails closed, cancellation propagates and is checked during bounded reads, owner/access checked before final downloaded-byte return. Blocking HTTP cancellation remains bounded by the existing timeouts.
- Audited readers and filled actual missing placements in Mock Competence, Free Practice, expanded Mistakes cards, and Results. Explanation-only figures in Results render even with blank rationale text. Existing readers already use the shared gallery.
- `QuestionImageGalleryTest.kt`: generated 2x2 PNG fixtures, isolated account preferences and cache cleanup; real verified local reads, account/access/expiry reset, reference/track changes, Retry, hash tampering, both placements, and zoom reset.
- Regression-only unsigned CI [43297070691](https://github.com/Nhyle-zunne/hyperion-reviewer/actions/runs/43297070691) ran 253 tests and reproduced owner/revocation/reference stale-zoom assertions plus an uncaught session-storage error on missing cache. All other tests passed.
- Local `node scripts/tests/agent-skills-test.mjs`, `python3 scripts/tests/question-bank-asset-safety.py`, and `git diff --check` passed. Android verification runs in the PR's unsigned Android CI; local SDK/Gradle are unavailable and were not installed.

Limitations: no physical-device gesture/TalkBack or live licensed service acceptance; superseded in-flight network responses are guarded in production but are not simulated in these cache-based tests. No signed build, store release, real image upload, bank/key edit, or default smart-bank enablement.

## Image-only compatibility follow-up

- All numbered native CSV readers and normalized GMDSS rows accept blank stems
  only with validated question-placement media; explanation-only media is rejected.
- Explicit normalized headers set the first data row even in one- and two-question
  GMDSS files. Existing encrypted synthetic fixtures exposed the previous short-file
  skip in CI runs 43334401847 and 43334401886; the parser row-start rule is corrected.
- Session snapshots, mistakes and cloud bookmarks retain image references; Room
  7-to-8 adds the column without deleting legacy questions or bookmarks.
- Regression coverage includes malformed/missing prompt media, answer/ID retention,
  saved-question round trips, corrupt-session rejection and a real SQLite migration.
- Final unsigned verification is tracked on PR #22; physical-device and live
  licensed delivery acceptance remain outside the automated checks above.
