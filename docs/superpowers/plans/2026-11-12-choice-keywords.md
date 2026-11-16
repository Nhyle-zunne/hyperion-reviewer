# Choice Keyword Highlighting Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Highlight words and symbols in stems and choices while safely suspending answering.
**Architecture:** Shared question-local editor state, legacy-compatible token IDs, atomic private preference writes.
**Tech Stack:** Kotlin, Jetpack Compose, SharedPreferences, Robolectric.
**Spec:** ../specs/2026-11-12-choice-keywords-design.md

## Global Constraints
Preserve existing stem highlights, banks, licensing and private storage. Do not install Android SDK locally; verify with GitHub CI.

## Review Focus
Legacy word IDs; punctuation and symbols; shuffled choices; pending answer submission; failed save and navigation discarding drafts.

### Task 1: Storage and UI behavior
- [x] Add storage and Compose tests for tokens, text identity, shared editing, inert cards, saving and failure.
- [x] Observe missing APIs in red CI 43200299249, then implement shared state and atomic save.
- [x] Wire standard drill, intensive drill and Quick Scan to the same state per question.
- [x] Run full-suite and focused unsigned Android CI, inspect the diff, and open the focused PR; report full-suite failures separately from focused validation.

## Execution ledger
User specification authorizes implementation without another design approval. Local SDK and Gradle are unavailable, so red/green evidence comes from CI.

Review found that Android commits update memory even when disk persistence fails. The storage now restores previous values on failed commit; a wrapped real-preferences regression covers later loads. Legacy compound punctuation overlaps word ranges, so rendering appends text once and adds styles to ranges; hit lookup prioritizes punctuation IDs. Independent review of 62c4be0 found no remaining important code bugs. Shared Compose tests cover punctuation taps, shuffled choices, focus/keyboard/accessibility blocking, disabled settings and navigation. Standard and intensive screen tests cover pending confirmation; Quick Scan uses the tested shared editor but has no separate full-screen regression.

Follow-up review requires independent symbol paint inside compounds. Legacy loads expand word selections to internal symbols; each text key receives an independent-symbol marker only upon successful save. Word toggles include internal symbols, while symbol toggles leave word letters alone. Version markers participate in failed-commit rollback. Historical choice preference entries are preserved because saves update current keys only.

## Verification evidence

- Application and tests at `ccee217`: all 14 keyword tests, `:app:lintDebug`, `:app:assembleDebug`, encrypted-APK safety and repository skills passed in [focused CI 43269170843](https://github.com/Nhyle-zunne/hyperion-reviewer/actions/runs/43269170843).
- The full 254-test feature run [43269185239](https://github.com/Nhyle-zunne/hyperion-reviewer/actions/runs/43269185239) passed all keyword tests but failed `NavigationAndTelemetryTableTest.testResumeSessionRestoresQuestionIndexAndAnswers` (line 299) and `ReviewerViewModelTest.testReactiveEntitlementPropagationWithoutRecreation` (line 625). The separate [account-linking run 43269185282](https://github.com/Nhyle-zunne/hyperion-reviewer/actions/runs/43269185282) failed only `NewTrackIsolationTest.testDeckMgtPassIsolation` (line 80). These tests were not changed or weakened.
- The full unit-test step passed on unchanged main `fa0cb6b` in [comparison 43269986629](https://github.com/Nhyle-zunne/hyperion-reviewer/actions/runs/43269986629). That comparison does not reproduce the feature-run failures; their cause remains unresolved. Earlier main entitlement failures were observed separately, so no claim is made that every failure is caused by this feature or is pre-existing.
- The final documentation/CI update leaves `app/` identical to tested `ccee217`. Manual filtered diagnostics use a distinct check name and cannot masquerade as the default full-suite check.
- Screen fixtures restore touched preferences and license flows and clear their ViewModels. The keyboard regression permits legitimate Save activation after focus moves away from a disabled answer.
- Independent review found no important remaining implementation bugs. No local Android SDK, device/emulator acceptance, production signing, store release or merge was performed. Quick Scan uses the covered shared editor and storage but has no separate full-screen regression.
