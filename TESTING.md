# FlowTap — QA Checklist (current contracts)

> Updated 2026-09-09 for the Settings/Mode-Edit repair (commit `0a9a530` + follow-up).
> Product contracts: a mode is config, a session is one run — **edits apply to the
> next session**. Permission granted ≠ blocking active. All completed sessions
> count; midnight sessions split across days. Emergency hold always exists.
> Emulator limits: accessibility overlay windows don't composite on emulator GPU
> (logic runs, nothing visible — verify on hardware); no NFC hardware on emulator.

Build & install:

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Unit tests: `testDebugUnitTest` covers `SessionPolicy` (strict-tag ending, lock),
`BlockingReadiness` (permission vs enforcement, conflict, timer-only), `AppCatalog`
(dedupe/sort/filter), `StatsCalculator` overlap (midnight split, short retained,
average window) plus legacy stats/time/notification/poller suites.

## Settings (priority)

- [ ] Top card shows Ready or Action needed with the specific missing item + Fix setup; no universal On badge
- [ ] Blocking method radios work; only the selected engine enforces (compat select + accessibility on = conflict note, accessibility silent)
- [ ] Banking note is conditional; switching to compat never claims all banks work
- [ ] Every row (admin, app-blocking, DND access, notifications, usage, overlay) opens Manage even after grant; DND reads Granted/Not granted, never bare On
- [ ] Denied permission never looks active; returning from system settings refreshes and re-applies live DND when needed
- [ ] Hardcore locks method/DND/hardcore/awake + banking switch at the domain layer (not just visually); appearance/help stay available; emergency exit preserved
- [ ] Version row shows `v0.1.0 (sha)` build id for APK/source comparison
- [ ] Failed system intents show "Couldn't open system settings" instead of silence

## Mode Edit (priority)

- [ ] Name → Apps → End → Sound → Duration → Goal → Tags → More options; one Save top + bottom, disabled while Saving…, error + Retry retains draft
- [ ] Running-session banner: changes apply to next session; active ending tag can't be moved mid-run
- [ ] Rotate/background/restore retains draft and tag picks; cancel changes nothing
- [ ] Double-tap save creates one mode; write failure changes nothing
- [ ] App list loads off-main, no duplicate-key crash (two launcher activities, one package), search + Selected filter preserve picks
- [ ] Paired tags listed by name; Pair new vs Move existing separated with confirmation
- [ ] Labels read Block selected apps / Allow only selected apps / Keep unchanged / Use app default
- [ ] Modes list: tappable card (name + one summary line), overflow holds edit/duplicate/delete; active mode can't be deleted

## Sessions / enforcement / NFC

- [ ] Strict ends only with the starting tag; other tags show "Finish with your X tag"; flexible ends on same mode; switch works when flexible
- [ ] No End action anywhere when strict or Hardcore (Home, notification, both overlays, shortcut intent); emergency hold + timeout + reboot always work
- [ ] Timeout fires once for the owning session (rapid switch can't kill the new one); 12h cap holds
- [ ] Reboot closes the run as reboot, restores sound, clears state without stale service
- [ ] Compat never blocks launcher/dialer/SystemUI; foreground retained ~30s across poll gaps
- [ ] Pairing checks UID before writing; success means DB-persisted; already-paired keeps payload; unknown/deleted UID shows unknown-tag and never starts via payload alone
- [ ] Sound failures don't claim success; restore evidence kept until confirmed; permission grant re-applies live DND

## Home / history / onboarding

- [ ] Active timer ticks every second with no input; session elapsed distinct from today's total; overnight capped at today's slice
- [ ] Sound chip reads DND on / Silent / Phone unchanged (three states, not a switch); Start focus button + separate NFC hint; strict-only hint names the mode need, not "all modes"
- [ ] Short sessions appear in history/recents/totals; week/today/average split 23:50–00:10 across days; crossing midnight/week refreshes without manual reload
- [ ] Mode delete is transactional with tag names + active lock; history keeps identity (no Deep Work misnaming); seed runs once
- [ ] Onboarding honors empty app selection (clears defaults) and writes the explicit target mode; uses shared readiness + catalog
- [ ] Tag delete confirms first (history kept); session delete is two-step inside details; lists have loading/empty states, never blank
- [ ] Overlay follows the app theme override (not system); keyboard/TalkBack/large fonts/narrow layouts sane

## Device acceptance

- [ ] Emulator (older supported APIs): both engines ± access, rotation, reboot, revocation, midnight rollover, empty/populated
- [ ] Physical the test device (Android 16/API 36, ~363dp, font 1.15): layout vs `docs/audit-2026-09-09/screenshots/`, light/dark/system, real NFC taps, real sound restore, incoming call + lock screen
- [ ] Record Android version, app version/sha, and config in QA notes; attach before/after Settings + Mode Edit shots

## Verified 2026-09-09 (isolated emulator, no phone changes)

- Env: `VartaTest` AVD, Android 17/API 37 (fresh `-wipe-data` boot); app `0.1.0 (6aba4e8…0a9a530+)` debug; `testDebugUnitTest --rerun` 48 pass, `lintDebug` + `assembleDebug` green.
- Onboarding → Home with zero app selection: Deep Work keeps empty blocklist ("0 apps blocked"); seeded once, never duplicated.
- Session ticking: `00:00:07` → `00:01:34` with no input; day total rolls live; session elapsed and today's total labeled separately.
- Process death: fixed `ForegroundServiceDidNotStartInTimeException` (service now foregrounds synchronously with a placeholder, then replaces it); no crash after fix across repeated force-stop/relaunch cycles.
- Real reboot with live session: closed as `reboot`, no ghost session/service, short 25s run retained in recents/totals; mode edit (`YouTube` blocked) and all history survived.
- Editor round-trip on device: card tap → toggle app → Save → Modes shows `1 blocked`; single navigation, no duplicate.
- Settings/Modes/Home render verified by screenshot against audit direction (readiness card, Granted labels, conditional banking copy, compact cards, honest chips).

Still requires physical hardware: NFC tap/pair flows, accessibility-engine blocking with real apps, DND/silent restore on device audio stack, both engines ± access, older supported APIs, TalkBack/keyboard/large-font passes, the test device layout comparison.
