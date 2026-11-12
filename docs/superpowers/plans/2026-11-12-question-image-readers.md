# Question image reader fixes

Scope: improve existing native attachment readers without changing the shared CSV contract, media API, bank assets, entitlement rules, default smart-bank flag, signing or releases.

1. Add isolated Robolectric/Compose regressions using generated 2x2 PNG bytes in the real owner/track/hash cache. Exercise owner switch, revocation, revised references, track switch, Retry, and one hash at both placements. Run the unchanged gallery against them in unsigned Android CI to establish failures.
2. Key gallery state and zoom to question ID, track, placement, complete references, owner and access. Observe existing entitlement revisions and local expiry while visible. Decode bounded bitmaps on the Default dispatcher; cancel superseded work and recheck access before publishing. Add an explicit Retry and accessible zoom controls.
3. Preserve loader cancellation, check cancellation during bounded reads and before cache publication/return, and recheck owner/access after cache writes. Keep downloads on demand and bounded to 512 KiB.
4. Audit all question screen call sites; add only actual missing gallery placements. Update attachment guidance with implemented behavior and CI evidence.
5. Run full unit, lint and debug assemble checks using GitHub Android CI because this workspace lacks Android SDK/Gradle. Review diff, push focused branch and open PR; no merge or release.

Verification evidence will be recorded below after execution.
