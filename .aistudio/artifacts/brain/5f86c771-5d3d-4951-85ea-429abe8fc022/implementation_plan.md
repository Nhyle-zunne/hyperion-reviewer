# Implementation Plan - MARINA Exam Target Date & Countdown HUD Widget

Implement the MARINA Exam Target Date and Countdown HUD widget on the dashboard.

## Proposed Changes

### 1. Dashboard UI & Composable: `MarinaExamCountdownStrip`
- Add `MarinaExamCountdownStrip` composable placed horizontally across the full width directly between the Summary/Streak Strip and the 2x2 Licensure Grid.
- **Styling**:
  - Strict 0dp corner radius on container and badge.
  - 1dp border (`#212E3C`), dark slate background (`#121922`), height ~44dp-48dp, 16dp horizontal screen padding.
- **State A (Target Date SET)**:
  - Left side: Calendar icon (`Icons.Outlined.CalendarMonth` in `#00FF66`) + Monospace text: `"TARGET EXAM: [MMM dd, yyyy]"`.
  - Right side: High-contrast rectangular badge (0dp corners, 1dp green border) with text: `"T-MINUS [X] DAYS"` (or `"TODAY!"` if 0 days, or `"[X] DAYS AGO"` if passed).
- **State B (Target Date NOT SET)**:
  - Left side: Calendar icon + `"SET MARINA EXAM TARGET DATE"` (muted grey text `#8B949E`).
  - Right side: Green badge with text: `"SET DATE →"`.

### 2. SharedPreferences Persistence & DatePicker Interaction
- **Key**: `"marina_exam_target_millis"`, default `0L`.
- **Interaction**:
  - Tapping the strip opens Android `DatePickerDialog` initialized to current date or saved target date.
  - Minimum selectable date: today's date (`Calendar.getInstance().timeInMillis`).
  - Upon selecting a date: save epoch timestamp in SharedPreferences and update state immediately.
  - Long-pressing the strip resets/clears target date back to unset (`0L`).

## Verification Plan
- Compile with `compile_applet`.
- Verify state rendering (Unset state vs Set state with days remaining calculation).
- Verify date picker opens, saves correctly, and long-press clears target date.
