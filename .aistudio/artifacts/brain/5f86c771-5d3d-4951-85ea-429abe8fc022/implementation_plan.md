# Implementation Plan - Restore Intensive Mode Layout & Mastery Card

Restore `QuizScreen.kt` Intensive Mode layout and UI components to precisely match the user's reference screenshots, including the top app bar, queue telemetry strip, feedback card placement, full-width bottom action button, and the ITEM MASTERY & ATTEMPTS card.

## Proposed Changes

### `QuizScreen.kt`
- **Intensive Mode Top Bar**: Ensure title `"Intensive Drill"` is accompanied by the green `"[RECYCLING]"` tag, subtitle `"F[#] • C[#]"`, back arrow on left, and single bookmark icon on right (removing grid icon).
- **Queue Telemetry Strip**: Display `"Cleared: X / Y"` on left and `"In Queue: Z"` with status dot on right.
- **Feedback Card**: Positioned below answer options and above the Mastery card after answer evaluation (Green for Correct, Red for Incorrect).
- **ITEM MASTERY & ATTEMPTS Card**: Fully restore the mastery card showing accuracy percentage, correct/incorrect counts, progress bar, and chronological attempts history.
- **Bottom Action Button**: Full width (`fillMaxWidth`) displaying `"Select an answer to proceed"` when unanswered and solid vibrant green `"Next Question →"` when evaluated.

## Verification Plan

### Automated Tests
- Run `gradle :app:testDebugUnitTest` to ensure all tests pass successfully.

### Manual Verification
- Compile applet via `compile_applet` and verify screen preview.
