# OIC-NW C1 explanations implementation plan

**Goal:** Complete all 831 C1 explanations using the accepted concise style and show them in the existing hyperion-pwa Explanation area.
**Spec:** User approved the actual PWA layout and requested the rest of C1, preserving bank answers and punctuation; official references and exact-result/closest-option wording are already agreed.
**Architecture:** Author question-specific records outside app assets. Bind each record to exact stem/options/key fingerprint, inject only the explanation CSV column, encrypt with the existing bank codec, and publish through the existing verified manifest/release-pointer transaction. No new layout or native Android release. Retain exact-match explanations during later C1 CSV updates; clear an old copied explanation on changed content.

- [x] Author questions 11–831 in six independent ranges; retain the accepted first ten. Use official references for concept checking, keep bank keys, and never invent a derivation that conflicts with arithmetic.
- [x] Reuse and test the C1 explanation reconciliation already added on current main. The obsolete duplicate implementation from the interrupted session was removed before publication.
- [x] Assemble all records, verify complete unique coverage, exact source fingerprints, nonempty useful text, and unchanged first seven CSV fields/IDs/order/keys. Test actual PWA mobile rendering and explanation-only changes.
- [x] Review the combined content and publisher diff, run relevant security/build/publisher tests, and publish the encrypted C1 file using the verified GitHub release flow. Recheck source/head before publication and verify bank-delivery access controls without exposing paid content publicly.

**Constraints:** No question removals, key edits, source punctuation changes, generic answer-restatement filler, secret exposure, real payment tests, native app signing, or backup-workflow reactivation. Similarity is discovery only, never explanation identity. This task targets PWA as clarified by the user; no native Android binary release is included.

## Completion, 12 November 2026

All 831 explanations were published in bank revision 217258a953e39f4b87b9e3cac56d68aabfb70540. Exact source fields, IDs, keys and all other 68 files were preserved. Live free-sample content and anonymous denial were verified. No PWA code or layout change and no Android store or signing release. Actual PWA mobile and desktop rendering passed in dark and light themes, with explanations hidden until Check Answer and no overflow or page errors. The fixture build passed. Native Android account-linking build passed, but Android CI unit tests failed on an entitlement assertion and the retry failed on a different entitlement test. Root cause is not established; no native release is claimed.
