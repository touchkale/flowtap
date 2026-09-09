# Flowtap UI and application-logic audit

Date: 9 September 2026. Source baseline: `98eb4c8`.

## User direction and conclusion

Raj asked for a whole-app UI/backend audit and a detailed fix plan for a new session. His priority workflows are **Settings and Mode Edit**. His design requirement is **simple and easy to use**.

The problem is more than visual styling. Configuration, permission, and effective behavior are represented as if they were the same thing. Large cards and chips amplify this ambiguity. Several actual logic defects undermine the interface's promises. Reorganize Settings and Mode Edit around everyday tasks while fixing the state and persistence contracts underneath them.

This audit changes documentation only. No app code, settings, modes, tags, sessions, permissions, or installed APK were intentionally changed. The existing mode editor was opened, scrolled, and closed without saving. The phone was returned to Home.

## Evidence and limits

- Reviewed the main navigation, all main screen implementations and their view models; onboarding and pairing; session lifecycle, both blocking paths, notifications and sound controllers; Room entities/DAOs/migrations, DataStore, statistics, theme/layout components, existing tests and TESTING.md.
- Visually inspected installed Home, Settings, Modes, and Mode Edit on the connected the test device phone: Android 16/API 36, 1272×2800, density 560, font scale 1.15. Its effective width is approximately 363 dp. This is a relevant everyday layout, not an extreme accessibility configuration.
- Installed app reports version 0.1.0/code 1, matching source metadata. This does **not** prove its APK exactly matches the source commit; no reinstall was performed.
- Screenshots are saved in `screenshots/`. One initial capture caught a navigation crossfade; it was replaced with a settled screenshot. No claim of persistent overlapping screens is based on that transient frame.
- UIAutomator twice returned a null root, so screenshot inspection and source tracing were used. This alone is not evidence of a Flowtap accessibility defect.
- Debug assembly succeeded. Fresh execution of all **32 tests across seven suites** passed. Lint completed with **0 errors, 51 warnings**. Initial Gradle output reused old tests; a second, test-only invocation with `--rerun` executed them on the audit date.
- NFC writes, actual blocking, permission revocation, reboot/process death, saving failures, rotation and dark mode were not exercised on Raj's phone. Findings below distinguish code evidence from observed UI and device-dependent risks.
- There is no separate server backend in this repository: this is Kotlin/Compose, Room v4, Preferences DataStore, Android services and NFC activities. No remote API/authentication layer was found.

## UI diagnosis

| Area | Evidence | What makes it difficult | Direction |
|---|---|---|---|
| Settings top | `screenshots/settings-top.png` | Blocking-method chooser and repeated banking warning dominate the screen. “Recommended” is immediately followed by “Breaks banking apps.” The primary readiness problem is farther down. | One concise readiness card; method configuration and troubleshooting on a detail screen. |
| Settings status | `screenshots/settings-bottom.png` | DND says “On” while the DND master says “Never touch DND.” Rows do not distinguish permission from actual behavior. | Use “Permission granted,” “Allowed for modes,” and effective session state as separate concepts. |
| Mode editor top | `screenshots/mode-editor.png` | Name, five goal choices, enforcement selector, explanation, search, large recommendation card, and nested app list compete. Selected apps are not summarized first. | Compact summary rows; app selection gets its own screen. |
| Mode editor lower section | `screenshots/mode-editor-bottom.png`, `mode-advanced.png` | Important ending and sound behavior are hidden under Advanced. Tags shows a count instead of the paired tag's name; pairing instructions refer to moving existing tags. Two Save controls. | Expose ending and sound summaries; list paired tags by name; separate “Pair new tag” from “Move existing tag.” One stable Save action. |
| Modes | `screenshots/modes.png` | Edit, duplicate, delete and many status chips compete in each card. The card itself is not the edit target. | Tappable row/card with name and one summary; overflow for duplicate/delete. |
| Home | `screenshots/home.png` | Oversized goal ring; disabled NFC-styled button; passive chips and an interactive sound chip look alike. Home says six apps blocked while Settings says blocking is off. | Show readiness, selected mode and clear start/tag instruction first. Separate current session timer from today's total. |
| Tags/history/onboarding | Source review | Immediate tag deletion, technical UID in everyday UI, repeated setup concepts, implicit statistics rules. | Clear summaries and recovery actions; disclose counting rules; request permissions contextually. |

The current palette can remain during the first repair. Smaller, clearer hierarchy and truthful state labels will help more than another color-only redesign. Keep font scaling; don't “fix” the density by shrinking all text.

## Findings: Settings and Mode Edit first

Severity: **P1** means incorrect core behavior or a trust/data issue; **P2** means significant usability or conditional reliability defect. “Code” means the relevant path is present in source, not that it was reproduced on this phone.

### S1 — Enabled-looking Settings rows do nothing (P1, code + visible affordance)

`ui/settings/SettingsScreen.kt:223–266`: admin, app-blocking and DND rows are always clickable, but their callbacks only act when permission is absent. Repro: after DND permission is granted, tap its row. It has no navigation/action. Fix: rows always open the corresponding management/detail screen, or render as clearly noninteractive status with a separate action. Permission status must not read simply “On.”

### S2 — Blocking-method selection does not select the enforcement engine (P1, code)

`SettingsViewModel.setBlockingMethod()` only writes a preference. `FocusAccessibilityService.onAccessibilityEvent()` never checks that preference; `SessionService` independently enables polling for compatibility. If accessibility stays enabled, both paths can operate after switching. The banking button cannot disable Android's accessibility service or guarantee a banking app accepts the resulting device state. Fix: coordinate exactly one selected engine, show remaining setup requirements, and guide the user to disable the service when needed. Replace unconditional banking guarantees with conditional, actionable copy.

### S3 — Readiness is not represented (P1, code + observed)

Settings' `blockingActive` is solely `isAccessibilityEnabled`, even for compatibility. Home does not surface missing blocking permission before starting and counts configured packages as “apps blocked.” On this phone, Full power is selected and App blocking is Off. Fix: a shared readiness state that considers selected engine, its required permissions, actual service connection, and current session. Distinguish “6 apps selected” from “Blocking active.” Allow an explicitly described timer-only fallback if that is the chosen product behavior.

### S4 — Hardcore restriction is inconsistent (P1, code)

Home locks ending for `strictMode || hardcore`, but `Notifications.sessionNotification`, `BlockOverlayController.render`, and `BlockOverlayActivity` inspect strict mode alone. A flexible session under Hardcore still gets ordinary End paths. The Settings banking nudge remains enabled while `locked`, bypassing the disabled method card. `SessionManager.endSession()` enforces neither restriction. Fix: one domain policy used by all entry points and UI surfaces. Preserve emergency exit. Scope appearance/help separately from enforcement changes.

### S5 — DND/silent state can claim success after failure (P1, code + platform risk)

`SessionManager.applyNotification()` may return without applying DND, yet the snapshot stores DND. Controllers swallow failures; restoration clears pending state even when the restore call fails. Granting permission after a failed live application does not itself reapply the requested state. A mode can appear configured correctly while the phone behaves differently.

Fix: report requested, effective, unavailable and restore-pending states; preserve restoration evidence until confirmed successful; reconcile on permission return and startup. Silent transitions can also require policy access when they toggle DND, contrary to the controller's “needs no system permission” comment. See [Android AudioManager](https://developer.android.com/reference/android/media/AudioManager#setRingerMode(int)).

The app targets API 35. On supported newer Android versions, `setInterruptionFilter` controls an app-associated automatic rule instead of globally restoring the old filter. Audit ownership and cleanup rather than assuming the legacy global-filter model. This is a platform compatibility finding, not a device-reproduced restoration failure. See [Android NotificationManager](https://developer.android.com/reference/android/app/NotificationManager#setInterruptionFilter(int)).

### M1 — Unsaved editor state is lost on recreation (P1, code)

`ModeComponents.kt:277–278` holds the draft in `remember(mode.id)`; attachment IDs are also ordinary remembered state (`ModeEditorScreen.kt:100`). No SavedStateHandle/saveable draft. Rotation or activity recreation rebuilds from the stored mode. Fix: an editor-scoped ViewModel with saved draft and explicit loading/ready/saving/error states. Back/discard must compare normalized values while retaining invalid user input for correction.

### M2 — Saving is neither guarded nor atomic (P1, code)

`ModesViewModel.kt:44`: each tap launches a new coroutine. For a new mode, double tapping may insert two modes. Mode and tag updates are separate writes; no transaction, saving flag or error state. Failures can leave partially moved tags, no feedback, or an uncaught coroutine exception. Fix: single-flight save, repository transaction, result handling, disabled Save while saving, one navigation event after committed success, retry with draft retained.

### M3 — App picker can have duplicate keys and blocks the main thread while loading (P1 conditional crash / P2 responsiveness, code)

`ModeComponents.kt:70` maps launcher activities without deduplicating package names; lists at lines 393 and 537 use package as key. Apps with multiple launcher activities can produce duplicate Compose keys. Onboarding repeats the loader. `LaunchedEffect(Unit)` runs synchronous PackageManager/label work without dispatching off the main thread. Fix: shared app catalog loaded off-main, deduplicated by package, deterministic sorting, loading/error/empty states, selected-app filter. Test a fixture with two launcher activities in one package.

### M4 — Editing a running mode silently changes only some behavior (P1, code)

Saving a mode updates Room but the running session retains its snapshot of blocked apps, strictness, timer and keep-awake. Home reads the live mode goal, and its separate sound toggle applies live. No editor notice explains this inconsistency. Fix: define one contract; recommended first pass is “Changes apply to the next session,” with existing session configuration preserved. Explicit session-only sound actions can remain separate. Protect rebinding/deleting the active session's ending tag.

### M5 — Editor dead ends and unclear controls (P2, code + observed)

Missing edit target plus an empty mode list returns a blank screen indefinitely (`ModeEditorScreen.kt:90`). Tag section cannot directly pair a new tag and hides names of current tags. “Enforcement / Blocklist / Allowlist,” “None,” “System” and “Strict” require interpretation. Fix: loading/not-found states; paired tag names and direct pairing; labels “Block selected apps,” “Allow only selected apps,” “Keep phone sound unchanged,” “Use app default,” and an ending-rule summary. Validate that launch-on-start isn't simultaneously blocked.

## Findings elsewhere in the app

### H1 — Home timer ticks are not consumed (P1, code)

`HomeScreen.kt:495–498` writes `elapsedMs` every second but never reads it in composition. The visible total instead uses `System.currentTimeMillis()` during composition. State writes alone don't invalidate a composition that never reads that state. Fix: observable clock/current elapsed used by the displayed timer and progress. Acceptance: a foreground session advances for 90 seconds with no input; resume is correct. Also give session elapsed and today's cumulative total separate labels.

### H2 — Sound and start labels misdescribe actions (P2, code + observed)

`home_notif_off` says “Notifications off” for `NotificationMode.NONE`, which leaves phone behavior unchanged. The sound control is declared a boolean switch but cycles three states. “Tap NFC tag to start” is a button that manually starts a flexible mode. The strict hint says all modes are strict whenever the selected mode is strict, even if another mode is flexible. Fix copy, use a true three-option selector, and distinguish NFC instructions from a manual Start button.

### B1 — Compatibility exemptions differ from accessibility (P1, code)

`SessionService.startCompatPolling` excludes Flowtap/SystemUI/android only. Accessibility additionally exempts launcher and telephony packages. Allow-only mode can therefore target the launcher or calling surfaces in compatibility mode. Fix: shared exemption policy with device-resolved launcher/dialer/essential system roles; test both engines against identical fixtures and real OEM call UI.

### B2 — Compatibility loses foreground knowledge after five seconds (P1, conditional code risk)

The poller only accepts recent foreground events; it does not retain current-app state or reconcile background/stop transitions. If enforcement fails during the first five seconds, an app that stays open can become invisible to the decision loop. Activity launches are swallowed and do not prove an overlay was shown. Android background launch rules apply; an overlay permission can provide an exemption, so do not claim this path always fails. Fix: explicit foreground tracking, permissions/readiness checks, visible failure diagnostics, screen-off handling and validated enforcement. See [Android activity launch restrictions](https://developer.android.com/guide/components/activities/secure-bal).

### N1 — Strict sessions match the mode instead of the starting tag (P1, code)

`SessionManager.kt:135` ends on `cur.modeId == mode.id`, before strict-mode handling. Any other tag bound to the same mode can end it, contrary to “Only the tag that started the session can end it.” The public end action and launcher shortcut also bypass strict checks. Fix: validate command source, starting tag identity and the effective session policy centrally. Keep emergency and timeout paths explicit.

### N2 — Pairing and removing tags can leave conflicting identity (P1, code)

`PairTagActivity.kt:175` writes the physical tag before checking whether UID is already paired at line 194. Existing tags can get a new payload while keeping their old DB binding. New tags show success/finish while a nested coroutine still saves the DB row. `NfcRouterActivity` accepts a payload mode ID even for an unknown UID, so deleting a tag binding does not necessarily stop that physical tag starting a surviving mode.

Fix: check UID and chosen mode before writing; await persistence before success; explicitly handle the unavoidable physical-write/DB-write recovery gap; define unknown/deleted tag policy and rebind behavior. Add unique UID indexing with deduplication migration. Do not claim UID matching provides cryptographic tag authentication.

### D1 — Recovery spans uncoordinated stores and boot paths (P1, code risk)

Starting a session writes a Room row, modifies sound, writes DataStore, then publishes active state and starts a service. These operations have failure windows. The unused `getInProgressSession()` is not consulted by normal recovery. `FlowtapApp.onCreate()` starts asynchronous restore before `BootReceiver` reconciles separately without SessionManager's mutex; reconciliation clears persisted state but not `_active`. Reboot can race restoration and leave stale in-memory/service state. Fix: one startup/recovery coordinator, idempotent transitions and a durable recovery protocol; fault-inject each boundary. Don't attempt a fictitious transaction spanning Android APIs, Room and DataStore.

### D2 — Timeout ownership and elapsed time need stronger guarantees (P2, code risk)

`SessionService.scheduleTimeout()` creates untracked jobs on every start command; both manager and service schedule deadlines. Jobs end whichever session is current, without a session-ID guard. Whether an old service survives a rapid switch long enough to fire needs integration testing. Wall-clock duration also includes powered-off time on boot and changes with manual clock adjustments. Fix: one deadline owner, cancellation and expected-session-ID checks, monotonic time within a boot plus explicit cross-boot policy.

### D3 — Mode deletion/history and seeding are inconsistent (P2, code)

Mode deletion and tag deletion are separate writes; history has only mode IDs and falls back to “Deep Work” after deletion, potentially misnaming old sessions. `ensureSeeded()` runs every app process start and recreates Deep Work whenever there are no modes, including after intentional deletion. Fix: transactional deletion, intentional history snapshot/archive policy, one-time seed marker, migration tests and exported Room schemas.

### T1 — Short sessions disappear and statistics disagree (P1 usability, code)

Sessions under five minutes are hidden from history, recents and totals. A one-minute session can still affect streak, and longest includes any completed session. During an active short session Home includes elapsed time, then removes it from totals after ending. This can look like data loss. Fix: one documented counting policy; recommended default is retaining every completed session visibly and distinguishing accidental sessions explicitly if needed.

### T2 — Midnight totals are assigned incorrectly and become stale (P1, code)

`StatsCalculator` assigns the full duration to the starting date. A 23:50–00:10 session is absent from today's total after completion and contributes twenty minutes to yesterday's weekly bar. Active Home uses today's overlap, so ending can reduce today's total. `StatsRepository` refreshes only when session data emits; crossing midnight/week boundaries need not recompute. Fix: interval overlap per calendar day, injected clock/time zone, day-boundary/resume invalidation, identical counting rules across displays.

### O1 — Onboarding can save a result different from the selection (P1, code)

Selected apps begin empty, while seeding adds a default blocklist. `OnboardingViewModel.finish()` only writes a nonempty selection; choosing zero preserves invisible defaults. It writes the first mode rather than explicitly tracking the mode paired during onboarding. Permission rows are accessibility-oriented even though a blocking-method preference exists. Fix: explicit draft mode ID, honor empty selection, use shared readiness for method-specific setup and preserve setup progress.

### U1 — Cross-cutting UI consistency and recovery gaps (P2, code/inspection)

Tag trash deletes immediately without undo; mode cards emphasize destructive icons; many screens equate initial empty collections with loaded emptiness. The accessibility overlay follows system theme rather than the app theme override. Several system-settings intents swallow failure without fallback. Replace these with clear loading/error states, consistent tap targets and selected semantics, undo/confirmation for deletion, shared theme resolution, and visible recovery actions. Test keyboard, TalkBack, large fonts and narrow layouts rather than assuming lint covers usability.

## Existing QA does not establish current feature correctness

TESTING.md contains valuable historical checks, but references an earlier UI, global goal controls and old DND behavior. Its emulator/API wording and assertions about overlays should be verified rather than treated as authoritative platform facts. Seven test classes cover statistics/time, notification enum mapping, polling decisions, app recommendation lists and hold math. There are no editor persistence, Room migration, service lifecycle or Compose interaction tests in the checked-in test tree.

Lint warnings need triage rather than blind fixes: the SAM warning on `stopService(Intent(...))` appears unrelated to Android's component-based stop API, and static-context warnings refer to an application-scoped container. Dependency notices are not evidence that updating dependencies will fix the user workflows. Prioritize the defects above.

## Next step

Use `FIX_PLAN.md` for the concrete screen specification and implementation sequence, and `NEXT_SESSION.md` as the new-session handoff prompt. All source paths above are relative to `app/src/main/java/com/flowtap/app/` unless otherwise indicated. Line numbers refer to baseline `98eb4c8`.
