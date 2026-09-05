# FlowTap — Manual QA Checklist (V1)

> Last updated after an on-emulator test pass (Android 16 / API 37, gfxstream).
> Two emulator limitations to know: TYPE_ACCESSIBILITY_OVERLAY windows are **not
> composited by the emulator's GPU** (the overlay window attaches and its timer
> logic runs — ~1 frame/s invalidation — but nothing appears on screen; it
> renders normally on real hardware), and the emulator has **no NFC hardware**
> (tags must be tested on a physical device).

Build & install:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Onboarding & permissions

- [ ] Fresh install shows "Step 1 of 3 — Pair your first NFC tag"
- [ ] "Do it later" advances to permissions
- [ ] Default "Deep Work" mode exists even if every onboarding step is skipped (seeding)
- [ ] Suggestion chip (Desk) creates/picks "Deep Work" mode and opens pairing
- [ ] Pairing: tapping a blank NTAG sticker writes NDEF + saves the tag ("Tag paired!")
- [ ] After the first pairing, Step 1 shows "Tag paired! Moving on…" and auto-advances
- [ ] Re-tapping an already-paired sticker says "Already paired as …"
- [ ] Read-only tag shows a clear error toast; a failed pairing never locks the scanner (busy flag always resets)
- [ ] Step 2 status rows flip green on return from system screens (resume refresh, no polling)
- [ ] Denied notification permission → row offers app-settings deep link
- [ ] Accessibility toggle works; "Allow restricted settings" (Android 13+) reached
- [ ] DND policy access screen opens; grant it
- [ ] Device Admin screen opens; enabling shows "Uninstall protection enabled"
- [ ] Android 16 device: "NFC tag scanning" item appears and opens the system prompt
- [ ] Step 3 lists launchable apps, search filters, checkboxes select
- [ ] "Start using FlowTap" saves blocked apps to the default mode and shows Home
- [ ] Re-opening the app skips onboarding

## Sessions

- [ ] Home shows goal ring, today total, "Tap NFC tag to start" button
- [ ] With no modes at all, Home shows "No modes yet" + "Pair a tag" instead of a dead button
- [ ] With only strict modes, the Start button is disabled ("All modes are strict — start with a tag")
- [ ] Soft start (tap the button): active theme appears, timer runs, notification with chronometer appears
- [ ] Notification has "End session" action (non-strict) and tapping it ends the session
- [ ] Tap the same tag with the app CLOSED: session starts/ends (screen must be unlocked)
- [ ] Cold-process end: with the app force-stopped mid-session, tapping the tag ENDS the session (does not start a second one)
- [ ] Cold-process notification end: force-stop, reopen via notification "End session" → session ends, not resumed
- [ ] Tap a different tag mid-session: current session logged (switched), new one starts
- [ ] Strict mode: different-tag tap is ignored + "Finish with your X tag" notification
- [ ] Strict/hardcore: no End button anywhere; tag ends, or emergency hold-to-end
- [ ] Home active ring shows TODAY's cumulative total (continues across same-day sessions, never resets to zero); overnight sessions capped at today's slice
- [ ] Home DND chip toggles the mode's DND live mid-session (filter NONE↔ALL) and persists for future sessions; enabling without policy access opens DND settings
- [ ] App kill mid-session: reopen → session still active, elapsed time is correct
- [ ] Reboot mid-session: session closed (endReason reboot), DND restored (receiver kept alive via goAsync)
- [ ] 12h auto-timeout ends the session (endReason timeout) + completion notification fires

## Tags & modes

- [ ] Tapping a tag card opens edit dialog: rename + re-bind to another mode
- [ ] Deleting a mode with tags warns with the tag names and deletes them (no "Unknown tag" zombies)

## Blocking

- [ ] Blocked app opens during session → returns home + fullscreen overlay within ~1s
- [ ] Overlay shows live elapsed time and the tag-to-finish message
- [ ] Overlay dismisses itself when the session ends (tag tap or app)
- [ ] Overlay does not appear when no session is active
- [ ] Opening FlowTap itself is never blocked (overlay is not shown over our own UI)
- [ ] Overlay survives mode switch mid-session (shows the new mode name)
- [ ] Home shows the blocked apps as icon badges (letter avatars) + "+N more" for unresolvable packages
- [ ] Blocking is immediate even when the app was killed (accessibility service is separate)

## Overlay exits (never traps the user)

- [ ] BACK with overlay up → overlay gone, launcher front, session still running
- [ ] RECENTS gesture with overlay up → overlay gone
- [ ] Screen off with overlay up → overlay gone (never covers lock screen); wake → no overlay
- [ ] "Show my timer" → overlay gone; app opens, or a toast points to the home screen if the OS blocks the launch
- [ ] Strict overlay: no End button; "HOLD TO END" + 3s hold ends session as EMERGENCY (history shows "Emergency")
- [ ] Fallback activity (force locally: make `BlockOverlayController.show()` return false) → hold/end/timer UI renders, BACK finishes to home, session end auto-finishes
- [ ] Reopening a blocked app after any exit re-shows the overlay

## DND

- [ ] Session start with DND-enabled mode → DND ON
- [ ] Session end → DND returns to its PRE-EXISTING state (not blindly off)
- [ ] Manual DND enabled before session survives the session
- [ ] Universal master (Settings → Behavior → Use Do Not Disturb): OFF at
  start → DND never engages; toggled ON mid-session → engages live;
  toggled OFF mid-session → restores immediately; end always restores
- [ ] Home DND chip toggles the mode flag (+ live mid-session); enabling
  without policy access opens system DND settings; Settings Protection row
  shows grant status

## Stats & history

- [ ] Today total, weekly ring, streak, longest, average all correct after a few sessions
- [ ] Weekly chart bars animate and match the numbers; dashed goal line correct
- [ ] Week range label shows Monday of the current week
- [ ] Session rows show correct times; detail dialog + delete works
- [ ] Deleting a session updates all stats immediately

## Settings

- [ ] Daily/weekly goal chips persist and update Home/History rings
- [ ] Hardcore mode locks End button and Settings during a session
- [ ] Keep screen awake prevents sleep only during a session
- [ ] Uninstall protection: with admin active, Play/Settings uninstall is blocked; deactivation shows the warning
- [ ] Protection rows reflect live status (resume refresh, no polling)
- [ ] Denied notifications → "Session notifications" row appears, opens app settings
- [ ] "Uninstall FlowTap" (About section): confirmation warns when admin protection or a session is active; confirming deactivates Device Admin and opens the system app-info screen where Uninstall works
- [ ] Uninstall is genuinely blocked while Device Admin is active, and unblocks after the in-app flow deactivates it

## Misc

- [ ] No chooser dialog appears on any tag tap (NDEF MIME exclusive)
- [ ] No crash on: NFC disabled, no tags paired, empty history, deleting last mode
- [ ] All strings render without truncation at small font scale
- [ ] Launcher long-press shows "Start focus" / "End session" shortcuts (localized); start soft-starts the first non-strict mode (no-op if a session runs or setup is strict-only); end closes the session; both work with the app already open (singleTop delivery)

---

## Verified on emulator (2026-09-02, Android 16 / API 37)

Automated pass via adb + uiautomator + DB/notification inspection:

- Onboarding: 3 steps render; default Deep Work seeded; accessibility/DND/notifications/
  battery/admin permissions all register green; blocked-app selection persisted
- Session: soft-start → teal UI + live timer + foreground service (specialUse) +
  notification with chronometer + DND filter NONE; process-death restore keeps
  correct elapsed; End (in-app + notification action) → DND restored to ALL,
  service stopped, DB row closed with correct duration, completion notification
- Blocking: Calendar blocked → sent home + overlay window attached and kept;
  overlay timer logic ran (1 fps invalidation); session end auto-removed overlay
- Uninstall protection: `adb uninstall` → DELETE_FAILED_DEVICE_POLICY_MANAGER
- History: totals/streak/longest/average math correct; weekly chart + range label
- Tags: mode editor (rename/DND/Strict/blocked-apps) works; strict mode disables
  Home's Start button with "All modes are strict — start with a tag"
- Settings: live protection + blocking status reflect device state

**Bugs found & fixed during this pass:**
1. `Permissions.isAccessibilityEnabled` compared `flattenToString()` against the
   system's `package/package.Class` form → always false. Fixed to accept both forms.
2. The block overlay auto-showed over the app's own UI whenever a session was
   active, and a window-id race could dismiss it. Fixed: overlay is now shown
   ONLY on blocked-app events and is removed on session end.
3. The overlay trapped users: BACK felt dead, "Show my timer" silently failed
   to open the app on newer Android, strict sessions had no exit without the
   tag. Fixed: overlay is focusable with a BACK handler; BACK/home/recents/
   lock all dismiss it (session continues); "Show my timer" falls back to a
   guiding toast; strict overlays (and the Home lock box) offer a 3s
   hold-to-end emergency exit recorded as EMERGENCY.
4. Permission/hardware rows polled every 1.5–2s forever. Fixed: resume-based
   refresh (`rememberResumeTick`) + permanently-denied notification guidance.

**Still requires a physical device:**
- NFC tag pairing + tap-to-start/stop with the app closed
- Onboarding auto-advance after first pairing
- Real rendering of the accessibility overlay window