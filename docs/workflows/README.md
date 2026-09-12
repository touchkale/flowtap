# FlowTap UI workflows

Real emulator screenshots (Pixel 10 Pro canvas, 1280×2856) assembled as
labeled SVGs. One SVG per independent workflow; each panel is numbered in
flow order with a caption.

Captured on: `VartaTest` AVD, Android API 37, fresh `-wipe-data` boot,
FlowTap `v0.1.0 (6dcc4ee)` debug, Sep 2026, via `adb screencap` (no mockups).
Light theme unless a panel says otherwise.

| SVG | Workflow | Panels |
| --- | -------- | ------ |
| `01-onboarding.svg` | Onboarding (pair tag → permissions → app picker) | 4 |
| `02-home-session.svg` | Home + session lifecycle (idle → active → ticking → ended) | 4 |
| `03-history.svg` | History (week chart + list → detail + delete) | 2 |
| `04-tags-nfc.svg` | Tags + NFC pairing | 2 |
| `05-modes.svg` | Modes list (cards → overflow → create → saved) | 4 |
| `06-mode-editor.svg` | Mode editor — LONG SCROLLABLE SCREEN (name → apps → advanced → save) | 4 |
| `07-settings.svg` | Settings — LONG SCROLLABLE SCREEN (readiness → permissions → theme/about) | 3 |
| `08-blocking-overlay.svg` | Blocking + enforcement (active → notification → blocked app → end) | 5 |
| `09-theming.svg` | Theming + appearance (dark Home/Modes/Settings + light default) | 4 |

Raw captures live in `raw/`. Emulator limits noted inside the SVGs:
no NFC hardware, accessibility overlay doesn't composite on emulator GPU,
compat blocking needs usage access.
