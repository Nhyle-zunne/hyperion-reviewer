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
- [ ] Add storage and Compose tests for tokens, text identity, shared editing, inert cards, saving and failure.
- [ ] Run tests in CI to observe missing behavior, then implement shared state and atomic save.
- [ ] Wire standard drill, intensive drill and Quick Scan to the same state per question.
- [ ] Verify unit tests, lint and debug assembly using unsigned Android CI; inspect diff and open focused PR.

## Execution ledger
User specification authorizes implementation without another design approval. Local SDK and Gradle are unavailable, so red/green evidence comes from CI.
