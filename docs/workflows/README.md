# FlowTap UI design studies

Open **[the preview gallery](index.html)** to review all nine workflows and switch between compact, Pixel 10 Pro and larger phone layouts.

| SVG | Design coverage |
| --- | --- |
| [01-onboarding.svg](01-onboarding.svg) | Pairing, permissions, app selection |
| [02-home-session.svg](02-home-session.svg) | Idle, active and completed sessions |
| [03-history.svg](03-history.svg) | Week, session detail, delete confirmation |
| [04-tags-nfc.svg](04-tags-nfc.svg) | Empty, pair, success, retry |
| [05-modes.svg](05-modes.svg) | Library, actions, saved state |
| [06-mode-editor.svg](06-mode-editor.svg) | Scrolling form and app picker |
| [07-settings.svg](07-settings.svg) | Readiness, permissions, behavior, appearance |
| [08-blocking-overlay.svg](08-blocking-overlay.svg) | Active session, notification, block, emergency end |
| [09-theming.svg](09-theming.svg) | Matching light and dark layouts |
| [10-responsive-check.svg](10-responsive-check.svg) | Three sizes side by side |

These are **editable vector references**, with sample data. The Android application implements their warm-paper system, responsive phone rules, and interaction states; the SVGs remain the visual review source. The original screenshot-based SVGs are preserved in [reference/](reference/); their original captures remain in `raw/`.

- `screens/`: individual SVGs in three phone sizes, plus complete scrolling editor and Settings sheets.
- [DESIGN.md](DESIGN.md): geometry, colors, responsive layout rules and interaction specifications.
- `generate_mockups.py`: reproducible standard-library Python source for all SVGs and the gallery.
- `manifest.json`: workflow-to-screen mapping.

Rebuild: `python3 docs/workflows/generate_mockups.py`.
