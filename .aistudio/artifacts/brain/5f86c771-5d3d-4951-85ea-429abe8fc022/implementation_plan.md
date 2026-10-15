# Implementation Plan — Persistent RECENTLY DELETED & 30-Day Retention Policy

Implement persistent access to Recently Deleted sessions on the Session Log & Diagnostics page (`HistoryScreen.kt`) and enforce a 30-day retention policy with expiration badges and permanent deletion confirmation.

## Proposed Changes

### 1. Fix 1 — Persistent RECENTLY DELETED Action on HistoryScreen
**Files:** `app/src/main/java/com/nhyle/hyperionmaritime/ui/screens/HistoryScreen.kt`

- **Action Bar Placement**:
  - Add a compact, full-width rectangular outlined button (`RECENTLY DELETED`) directly below `MaritimeTopBar` and above the study streak / cumulative performance sections.
  - Features 0dp corner radius (`RectangleShape`), theme-aware border (`colors.border`), trash icon (`Icons.Default.Delete`), and 48dp minimum touch target.
  - Visible in all History states: when 0 active logs exist, 1 active log exists, or multiple logs exist.
- **Dialog Connection**:
  - Tapping `RECENTLY DELETED` opens the Recently Deleted recovery dialog.
  - In the empty-state (`NO EXAM SESSIONS RECORDED`), retain the centered empty text and description, with the action bar visible above the streak/content area.

---

### 2. Fix 2 — 30-Day Retention Policy & Permanent Deletion
**Files:**
- `app/src/main/java/com/nhyle/hyperionmaritime/data/local/DeletedSessionManager.kt`
- `app/src/main/java/com/nhyle/hyperionmaritime/viewmodel/ReviewerViewModel.kt`
- `app/src/main/java/com/nhyle/hyperionmaritime/ui/screens/HistoryScreen.kt`
- `app/src/main/java/com/nhyle/hyperionmaritime/ui/screens/PracticeConfigScreen.kt`

- **30-Day Retention & Automatic Purging (`DeletedSessionManager.kt`)**:
  - Calculate remaining days: `val daysLeft = ((30L * 24 * 60 * 60 * 1000 - (now - deletedTimestamp)) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)`.
  - In `loadDeletedSessions()`, automatically filter out and purge sessions older than 30 days (`now - deletedTimestamp > 30 days`), writing the cleaned list back to disk.
  - Expose helper property/method for formatted expiration string: e.g. `"EXPIRES IN 29 DAYS"`, `"EXPIRES IN 1 DAY"`, or `"EXPIRES TODAY"`.
- **Permanent Deletion Method (`ReviewerViewModel.kt` / `DeletedSessionManager.kt`)**:
  - Add `permanentlyDeleteSession(deletedSession)` method in `ReviewerViewModel`:
    - Removes `deletedSession` from `deleted_sessions.json`.
    - Deletes active snapshot file from internal storage.
    - Re-emits updated `deletedSessions` state flow.
- **Dialog UI Enhancements (`HistoryScreen.kt` & `PracticeConfigScreen.kt`)**:
  - In the Recently Deleted dialog, show the compact expiration status label for each item (e.g. `"EXPIRES IN 29 DAYS"` in warning/muted color).
  - Add a red/error `PERMANENTLY DELETE` button with trash icon below the expiration label.
  - Tapping `PERMANENTLY DELETE` opens an explicit confirmation dialog: `"Are you sure you want to permanently delete this session log? This action cannot be undone."`.
  - Confirming permanently deletes the item and refreshes Recently Deleted.
  - Canceling closes the dialog without modifying the session.

---

## Verification Plan

### Automated Build Verification
- Execute `compile_applet` to confirm zero compilation or lint errors.

### Acceptance Tests
1. **Persistent Button (`HistoryScreen.kt`)**:
   - Verify `RECENTLY DELETED` action bar appears below top app bar with 0, 1, and many active logs.
2. **30-Day Expiration**:
   - Verify remaining days calculation (`EXPIRES IN X DAYS` / `EXPIRES TODAY`).
   - Verify sessions older than 30 days are automatically purged on load and cannot be restored.
3. **Permanent Deletion**:
   - Tap `PERMANENTLY DELETE`, confirm in warning dialog, and verify session is permanently removed from storage.
