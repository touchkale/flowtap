# Flowtap repair and simplification plan

This is an implementation plan, not a claim that repairs have been made. Findings and evidence are in `AUDIT.md`. Raj explicitly prioritizes Settings and Mode Edit and wants a simple, easy-to-use app.

## Proposed product contracts

Use these defaults for implementation unless Raj supplies different preferences. They are recommendations, not previously approved changes to product behavior.

1. A **mode** describes a reusable focus configuration; a **session** is one run of it. The editor says “Changes apply to your next session” during a run. Current-run changes must be explicit.
2. Settings is for app-wide preferences, permissions and troubleshooting. Mode Edit owns app selection, ending rules, sound, duration and goal. Avoid parallel controls with unclear precedence.
3. Preserve existing global values during migration. Initially show the DND master as an advanced app-wide override with explicit wording; don't silently enable DND for users who disabled it. Keep-awake override labels must say “Use app default.”
4. Keep strict/tag ending and the emergency hold. Centralize rules before considering consolidation of Strict and Hardcore. Never remove an emergency route as a visual simplification.
5. A configured permission is not proof of active enforcement. Home and Settings use the same readiness model. Starting with unavailable blocking offers “Fix setup” and an explicitly identified timer-only option if appropriate.
6. A paired tag is an ending credential for the configured strict behavior. Unknown/deleted UIDs require pairing rather than silently trusting a local numeric mode ID. Existing tag migration needs a compatibility path.
7. Preserve recorded history. Recommended behavior is showing all completed sessions; decide whether accidental sessions are excluded from goals through one visible rule. Do not delete short records.

## Settings screen specification

Main page should be a short list of meaningful destinations, with compact current-value summaries:

| Order | Row | Behavior |
|---|---|---|
| 1 | Setup status: “Ready” or “Action needed” | One card with the specific missing requirement and a Fix action. No universal “On” badge. |
| 2 | App blocking — selected method and readiness | Opens method choices, relevant permissions and troubleshooting. |
| 3 | Session defaults — concise summary | Opens screen-awake default, global restrictions and clearly described DND override. Link to Modes for per-mode behavior. |
| 4 | Appearance — System / Light / Dark | Opens a compact choice sheet; changes immediately and persists. |
| 5 | Permissions & device access | Always accessible; every permission has Granted/Not granted status and a Manage action. |
| 6 | About & app management | Version/build identifier and uninstall flow. Destructive action belongs here. |

Blocking detail: two ordinary radio rows with plain descriptions. Explain compatibility tradeoffs once. Show only the selected method's setup requirements, plus a specific note if accessibility must be disabled for banking compatibility. Do not promise every banking app works. Missing permission must be actionable after both denial and grant. A locked session disables enforcement changes at the domain layer as well as visually; appearance and help remain available.

Use grouped rows instead of a bordered card per setting, one title size, readable secondary text, a single accent for actions/selection, consistent chevrons for navigation and switches only for binary settings. Selected radio state must be exposed to accessibility, not merely indicated through color. Keep touch targets at least 48 dp and let text wrap.

## Mode Edit screen specification

Use one stable top bar with Back and Save. The basic page consists of:

| Order | Control | Summary/example |
|---|---|---|
| 1 | Name field | Deep Work |
| 2 | Apps | “Block 6 selected apps” → full-screen app picker |
| 3 | End session | “With the starting tag” or “From the app or tag” → explicit choices and emergency explanation |
| 4 | Sound | “Keep unchanged,” “Silent,” or “Do Not Disturb”; show unavailable/overridden state beneath |
| 5 | Session duration | “No automatic end” or “25 minutes” → preset/custom sheet |
| 6 | Daily goal | “2 hours per day” → preset/custom sheet; visually distinct from session duration |
| 7 | Tags | Actual paired names; “Pair new tag”; separate “Move existing tag” with source mode and confirmation |
| 8 | More options | Screen-awake override and launch-on-start |

The app picker gets one scrolling list, sticky search, selected count, “Selected” filter and a Done action. Recommendations become a small optional action, not a large explanatory card. Load apps off-main, deduplicate by package, include previously selected unavailable apps with a clear status, and preserve selections across search and recreation. “Allow only selected apps” belongs here with an explanation of essential exemptions.

Save validates the whole draft, highlights errors, disables during saving, and returns only after a committed transaction. Failed saves preserve the draft and show Retry. Repeated taps cannot create duplicates. Pairing a new tag while editing must not silently persist an unrelated partial draft: either require saving first or hold a draft-bound pairing result and commit it explicitly.

## Implementation sequence and acceptance gates

### Phase 0 — Establish a reproducible baseline

- Inspect current Git status before editing; baseline for this audit is `98eb4c8`. Respect work from other coding apps.
- Add a build identifier for future APK/source comparisons and record Android version, app version and tested configuration in QA notes.
- Add the necessary test infrastructure: Compose interaction tests, coroutine/fake repositories, Room migration tests. Tests should exercise user outcomes and failure behavior, not duplicate implementation.
- Create failing regression cases for editor recreation/double-save, permission labels, effective lock policy, timer tick consumption and midnight statistics.
- Gate: current tests/build still pass and new targeted regressions fail for the expected reasons.

### Phase 1 — Repair Settings behavior and rebuild its layout

Files: `ui/settings/*`, `ui/onboarding/Permissions.kt`, `data/prefs/SettingsDataStore.kt`, `session/*`, `blocking/FocusAccessibilityService.kt`, shared UI components and strings.

- Introduce a shared readiness model and typed settings actions/results. Model permission, selected method and running engine separately.
- Coordinate blocking methods and centralize lock policy. Fix active-row no-ops and banking-nudge bypass.
- Introduce requested/effective sound state and restoration error tracking. Reconcile permission changes; implement Android-version-aware DND ownership.
- Build the short Settings page and its detail screens above using the existing visual foundation.
- Gate: every visible row has a predictable action; permission return refreshes correctly; selected method is actually used; denied permission cannot look active; Hardcore cannot be bypassed through the nudge or notification; saved settings survive relaunch.

### Phase 2 — Repair editor persistence and simplify the workflow

Files: `ui/modes/*`, `data/repo/FocusRepository.kt`, `data/db/*`, `ui/navigation/MainNav.kt`, `nfc/PairTagActivity.kt` as needed.

- Move draft into an editor ViewModel/SavedStateHandle; add loading, not-found, dirty, saving and failure states.
- Save mode/tag changes transactionally with single-flight protection. Validate tag binding and launch-app conflicts.
- Extract one shared app catalog/picker with deterministic package identity, async loading and selected state.
- Implement compact editor summary rows, dedicated picker, sheets, named tag list and direct pairing flow.
- State clearly when edits apply to the next session. Preserve the active session's ending tag until it is safe to move/delete.
- Gate: rename, choose apps, change goal, save, reopen all match; rotate/background/restore retains draft; cancel changes nothing; double-save creates one record; write failure changes nothing and retains input; selected apps survive filtering; empty/missing mode states never render blank.

### Phase 3 — Make sessions, NFC and enforcement consistent

Files: `SessionManager`, `SessionService`, `FlowtapApp`, `BootReceiver`, both overlays, `Notifications`, NFC activities/writer and Room/DataStore recovery paths.

- Enforce all start/end commands centrally with explicit source, expected session ID and effective restrictions. Match strict ending to starting tag.
- One deadline owner and a serialized startup/recovery sequence. Design recovery for failures between Room, preferences and Android side effects.
- Share exemptions and foreground tracking across engines. Surface actual enforcement failure instead of swallowing it.
- Pairing: check existing UID before physical write; await local persistence before success; recovery for partial pairing; unknown/deleted-tag policy; UID uniqueness migration.
- Gate: same/different tag behavior, emergency exit, app/shortcut/notification ending, timeout, rapid switching, process restore, reboot, permission revocation, incoming calls, launcher and lock screen all follow one policy. Test on a disposable profile/emulator first, then physical NFC hardware.

### Phase 4 — Repair Home/history and remaining usability

Files: `ui/home/*`, `data/repo/Stats*`, `ui/history/*`, `ui/tags/*`, onboarding, theme and shared components.

- Render an observed session clock; distinguish elapsed session from daily total; replace misleading start/sound/blocked-count labels.
- Calculate calendar-day overlap consistently and refresh on date/timezone/resume changes. Apply one counting policy to history, longest, streak and totals.
- Preserve history identity after mode deletion; transactional delete and one-time seeding; confirm/undo tag deletion.
- Honor zero apps and the actual target mode during onboarding; reuse permission readiness and app picker.
- Match overlay theme with app preference; verify standard control semantics and keyboard behavior.
- Gate: 90-second visible ticking, midnight crossing, short session completion, timezone change, mode deletion and zero-selection setup show correct results without manual refresh.

### Phase 5 — Device acceptance and handoff

- Run fresh unit/integration/UI tests, lint and assembly. Test light/dark/system, 320/360/412 dp widths, font scales 1.0/1.15/1.5/2.0, keyboard open, recreation, empty data and populated history.
- Use the connected Android 16/API 36 phone for final layout comparison against screenshots; cover older supported APIs in emulators. Record actual tested versions rather than universal compatibility claims.
- Verify both blocking methods independently with and without required access; test real NFC and sound restoration with an agreed device test scenario.
- Update TESTING.md to current product contracts and attach before/after images of Settings and Mode Edit.
- Commit each coherent app/feature change promptly under author and committer **Raj Sharma**, using the repository's existing identity information. Do not bundle other tools' uncommitted work or rewrite history.
- Completion means the user can change app preferences and edit a mode reliably, with honest feedback and recovery—not simply that compilation succeeds.

## Suggested first implementation session scope

Complete Phases 0–2, pulling in the readiness, sound and lock-policy pieces from Phase 3 needed to make Settings truthful. If the work spans more than one session, finish one tested vertical slice and record exact remaining findings. Avoid a broad navigation rewrite, library upgrade campaign or new visual theme in this first session.

Keep current five tabs initially to contain risk. A later simplification to Focus, Modes, History and Settings with tag management under Modes is optional, not a prerequisite. Do not remove tag access until the new route is proven easy to find.

Optional product decisions worth confirming when relevant: whether short sessions count toward goals, whether mode edits should ever apply live, and whether global Hardcore should remain distinct from per-mode tag ending. These do not block repairing no-op rows, editor saving, duplicate app keys or readiness reporting.
