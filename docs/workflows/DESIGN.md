# FlowTap — vector UI proposal

This redesign is an editable SVG design study, not a change to the Android application. All session values, installed apps, tag names and permission states are illustrative. Original screenshot boards are preserved in `reference/`; raw captures remain in `raw/`.

## Direction

Warm paper, forest green and soft outlined cards. Large numerals emphasize focus time; secondary information uses a quieter hierarchy. One primary action sits within thumb reach. The existing five destinations remain Home, History, Tags, Modes and Settings. This is a proposed palette change from the app's current amber/navy system, scoped to these mockups.

Every visible shape, icon and text label is vector geometry or editable text. No embedded screenshots, remote fonts or external SVG assets are needed. Arial with Helvetica/sans-serif fallback keeps previews portable; an Android implementation should use its platform typography and remeasure text.

## Phone geometry and adaptation

[Google's Pixel 10 Pro specifications](https://store.google.com/product/pixel_10_pro_specs?hl=en-US) list a 1280 × 2856 display. The reference design uses a **400 × 892.5 logical canvas**, exactly the same pixel aspect ratio, at an illustrative scale of 3.2. This logical width is a design choice, not a claim about the device's configured Android density or user display-size setting.

Each individual screen is also generated at **360 × 800** and **440 × 956**. The generator recomputes card widths, text wrapping, navigation spacing, bottom actions and visible scroll area. It keeps type and control heights constant; these are separately laid-out variants, not scaled screenshots. `10-responsive-check.svg` compares the same Home screen at all three sizes. The gallery selector changes every phone to the requested layout.

Static SVGs cannot implement Android responsive layout, live keyboard avoidance or scrolling. Their `viewBox` makes them scalable for viewing; the variants and these rules specify how the actual UI should adapt:

- Use the current window width and real status/navigation/IME insets. Do not hardcode a particular handset's pixel density or camera position.
- Maintain 20 dp side gutters at these widths. Cards stretch to fill; on wider windows center content with a 600 dp maximum, consistent with the existing app helper.
- Keep 48 dp minimum interactive targets, 54 dp primary buttons, and 76 dp or taller setting rows. Small drawn icons and switches sit inside their row's touch target.
- Body content scrolls independently beneath stable headers and above the primary action and bottom navigation. The scroll indicator deliberately signals continuation where a row is partially visible.
- Reserve bottom space for both the primary action and navigation; never place a button over a fully accessible form field or row. The bottom bar follows system gesture insets.
- At larger font scales, wrap headings and row labels, allow rows to grow, reduce the timer illustration, and stack segmented choices when they cannot fit. Never shrink text to make it fit. Font scaling and landscape need implementation-level verification.
- When the keyboard opens, keep the edited field and Save reachable through IME padding and scroll-to-focus. Respect native back handling and unsaved-change confirmation.

## Workflow and interaction decisions

1. **Onboarding:** retain tag pairing, permissions and app selection. Pairing can be skipped. Required blocking setup is distinct from optional notifications/DND. Permission rows launch Android settings and refresh on return. NFC requirements depend on hardware and Android version. Continue must explain missing required setup; an optional denial must not trap the user.
2. **Home:** selected mode, daily progress and Start are the main hierarchy. Active duration is elapsed time, not a countdown. The ring represents total progress toward the mode's daily goal, including the running session. Strict state directs the user to the starting tag; normal sessions retain an End action. Session completion updates totals and adds a recent record.
3. **History:** a weekly total and seven-day chart lead to individual records. Opening a row shows start/end provenance. Deletion requires a separate destructive confirmation and recalculates totals. Empty history should replace the chart/list with an explanation and a route to Home. The chart's exact values must be accessible as text in implementation.
4. **Tags:** naming and choosing a mode precede scanning. Success is explicit, with the newly paired tag visible. Failure retains name and mode, offers retry, and explains where to enable NFC. Actual tag writing and unsupported/read-only tag errors require hardware testing.
5. **Modes:** comparable cards show blocked-app count, sound and strictness. Overflow opens Edit, Duplicate and Delete. Create opens the same editor with a blank name and defaults. Deleting a mode used by a running session stays locked; active edits take effect next session. A duplicate must get a distinct editable name.
6. **Mode editor:** one continuous form; the app picker is a separate subview. Daily goal, block/allow policy, app selection, strictness, sound, auto-end, screen awake, launch app and linked tags remain available. The pinned Save action persists while scrolling. Full continuous SVGs expose fields between viewport excerpts. Save validates required name and supported numeric ranges; Back prompts to keep editing or discard unsaved changes.
7. **Settings:** lead with blocking readiness, followed by method, permissions, behavior, appearance and About. Full power and Compatibility retain their existing tradeoffs. A missing permission replaces Ready with an actionable Fix setup state. Selecting Compatibility while accessibility remains enabled must explain the conflict. Full continuous SVGs expose all rows; start/middle/end panels are scroll positions on one page.
8. **Blocking:** a calm interruption names the blocked app and current mode. Back to focus returns to the session. Strict mode uses the starting tag. Emergency hold is **3 seconds**, matching `HoldToEnd.HOLD_MS`; releasing early cancels. Hardcore and strict mode remain distinct and must preserve emergency access. The notification is a conceptual Android notification layout; exact system rendering varies.
9. **Appearance:** System / Light / Dark applies throughout the UI, including the blocking overlay. Dark variants use the same geometry and stronger foreground contrast. Selection must be indicated by text/shape as well as color.

## Tokens

| Role | Light | Dark |
| --- | --- | --- |
| Background | `#F5F4EE` | `#101B18` |
| Surface | `#FFFFFF` | `#1B2A24` |
| Main text | `#192D28` | `#F0F4ED` |
| Secondary text | `#596A63` | `#B0C2B6` |
| Accent | `#245C48` | `#ACD6B0` |
| On accent | `#FFFFFF` | `#142B1F` |
| Selected surface | `#E4EDE3` | `#2B4234` |
| Border | `#DCE3DC` | `#34473C` |
| Destructive text | `#A43D35` | `#FFB4AA` |

Spacing uses 4/8/12/16/20/24/32. Cards use 18–24 dp radii; buttons use 17 dp. Main headings are 24–28, body 16, supporting labels 13–14, timer 44. Decorative separators are intentionally subtle; selected controls use a stronger accent border or fill.

## Rebuild and review

Run `python3 docs/workflows/generate_mockups.py` from the repository root. The Python standard library is enough to generate all assets. Open `index.html` for natural-size previews, or open any numbered SVG directly. The main nine boards use Pixel geometry; `screens/` contains the three phone variants plus expanded editor/Settings content sheets.

Review performed: SVG XML parsing, absence of embedded raster data, light/dark text contrast checks, text width checks using the available Arial font, and rasterized visual inspection of boards and compact/large samples. These checks validate design artifacts, not Android behavior, screen-reader semantics or hardware enforcement.
