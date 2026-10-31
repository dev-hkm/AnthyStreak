# Changelog

## 1.6.0 — 2026-10-31

### Added
- Full English and Vietnamese localization for the app, widgets, reminders, settings, and configuration screen.
- Full-screen Settings with appearance, language, feel, notification, and data preferences.
- Insights screen with an eight-week heatmap, best days, milestones, and streak summary.
- Milestone celebrations at 3, 7, 14, 30, 60, 100, 180, and 365 days.
- Pause mode: take a planned break without losing the earned streak.
- Habit templates to start a goal with a thoughtful, editable setup.
- Launcher shortcuts for Today and Insights, plus a Quick Settings check-in tile.
- Predictive back with progress-linked visuals across screens.

### Changed
- Replaced every hand-drawn icon with Phosphor Light icons and removed emoji from all copy.
- Rewrote notifications and rescue reminders in a calmer, friendlier bilingual voice.
- Rebuilt widget layouts so every size, including the minimum crop, stays readable with no clipped text.

## 1.5.0 — 2026-09-27

### Added
- Overhauled Streak Debt System:
  - Limited automatic debt to a maximum of 2 days (grace period).
  - Unclaimed missed days beyond grace require active commitment via a manual Claim Debt flow in-app to protect the streak.
  - Interactive Debt Management: users can view and pay off individual debt days directly in the app.
  - Relentless Debt Reminders: daily high-urgency notifications at 10:00 and 20:30 nagging and warning users to pay back their streak debts.

### Changed
- Responsive Widget Architecture:
  - Re-engineered widget size classification and layout matrix inspired by Tonser's adaptive architecture.
  - Fixed text clipping, overflow, and cramped padding across all homescreen grid scales (1x1, 2x1, 2x2, 3x1, 4x1, 4x2).
  - Replaced hardcoded hex colors with unified `@color/widget_...` themes supporting both light and dark system appearances.
  - Added uniform autoSizeText on streak counters so multi-digit numbers fit without cut-offs.

## 1.4.2 — 2026-09-11

### Added

- Added per-goal streak notes with date, add, delete, and local persistence support.
- Included all streak notes in JSON backup and restore.

## 1.4.1 — 2026-09-11

### Added

- Missed past goal days are now recorded as debt instead of resetting the current streak.
- Added a compact debt indicator and weekly debt legend while keeping the core streak ritual unchanged.
- App and homescreen widget data now reconcile missed days consistently.

## 1.4.0 — 2026-09-08

### Added

- Bonsai Tree Visual Theme: Introduced organic growth visualization adapting through 5 stages (Seed, Sprout, Sapling, Bonsai, Blooming) according to streak days.
- Nurture Ritual Animation: Gentle dew drop watering animation with ripple splash and blooming blossoms upon check-in.
- Visual Theme Selector in Goal Editor: Easily switch any daily goal between Flame and Bonsai Tree.

## 1.3.3 — 2026-09-08

### Changed

- Removed the fire sound effect from the check-in ritual; the experience is fully visual again.

## 1.3.1 — 2026-09-04

### Fixed

- Slimmed the flame back down: tall narrow silhouette with a sharp crown tip and slim waist, in-app and widget alike.
- Fixed ghost rings showing behind the flame before check-in — the ignition burst overlay now only draws during the actual 1.8s ritual.
- Livelier idle burn: added a fast flame-lick harmonic so the tip darts like a real fire while the body sway stays calm.

## 1.3.0 — 2026-09-04

### Flame glow-up

- Rebuilt the in-app flame with four depth layers: deep ember body, glowing mid, white-hot core, and hot nucleus, plus rim light, ember bed, and grounded shadow.
- Richer ignition ritual (still 1.8s): white-hot flash bloom, twin shockwave rings, 14 rising embers, and a 12-ray spark burst with a hot tip spark.
- Added a matching check-in burst behind the hero flame: early flash plus expanding accent rings with a spring scale punch.
- Redesigned the homescreen widget flame: new premium silhouette with leaning crown tip, mid glow layer, hot core with bright heart, and a true radial halo.
- Widget ignition now lights all four flame layers in lock-step with the existing 690ms ritual.

## 1.2.0 — 2026-08-10

### AnthyStreak identity and typography

- Renamed the app from Widget to AnthyStreak without changing the package ID or existing local data.
- Bundled EB Garamond offline across the app and widget, including regular, italic, and weights 400–800.
- Kept API 24–25 widget text on a safe serif fallback and used the bundled family on API 26+.

### Flame and Today

- Rebuilt the flame motion as a bounded physical sequence: compression, bloom, recoil, settle, and completed linger.
- Kept the ignition ritual at 1.8 seconds and tied the commit to the exact goal that was tapped.
- Increased the main flame slightly to 208dp while keeping the streak hero centered independently of actions and checklist shortcuts.
- Moved the focused goal title to the top-left header and added ten curated flame colors for a total of 22.
- Replaced the color grid with a snapping horizontal selector and accessibility semantics.

### My Goals

- Replaced the gray streak-number circle with a lightweight flame in the goal's selected color.
- Moved streak counts into compact metadata and added checklist progress/status presentation.
- Replaced the misleading navigation chevron with a Lucide Ellipsis manage action.
- Made archived cards safe: only the explicit Restore action can restore a goal.

### Widget system

- Added six responsive size buckets: Tile, Strip, Compact, Square, Wide, and Hero.
- Added four widget styles: Ember, Focus, Checklist, and Poster, with sensible fallbacks for small ratios.
- Added per-widget goal/style persistence, Android 12+ responsive size maps, and pre-Android 12 size classification.
- Added a best-effort 690ms widget ignition sequence after check-in with a guaranteed final authoritative render.
- Moved widget chrome to day/night-qualified resources so existing widgets follow system light/dark changes without reopening the app.
- Kept widget placement/style local to the device and excluded from portable JSON backups.

### Verification

- Passed the full JVM unit-test suite: 36 tests, 0 failures/errors.
- Built the release artifact with versionCode 3/versionName 1.2.0 and verified the HKM v2/v3 signature.

## 1.1.0 — 2026-08-09

### Fixed

- Removed the oversized gray empty-state card on Today; the first-goal prompt now sits centered directly on the app surface.
- Replaced the legacy 110dp, single-column widget that collapsed text and wasted space when resized.
- Prevented stale widget check-in actions from completing archived goals, re-checking an already secured day, or colliding across widget instances.

### Widget overhaul

- Added responsive compact, standard, and expanded widget layouts with a layered flame, large streak count, clear goal hierarchy, checklist progress, and a safe one-tap check-in state.
- Kept widget chrome strictly black/white in light and dark mode; only the goal flame receives its selected accent color.
- Added a focused widget setup screen so each homescreen widget can follow its own daily goal, including Android 12+ preview and reconfiguration support.
- Added per-widget intent identity and private action tokens, plus immediate in-app refresh after a widget check-in.
- Added reliable refresh handling for resize, date/time-zone changes, boot, package replacement, goal edits, archive/delete, import, and theme changes.

### Quality checks

- Added five unit tests for empty, focused, ready, completed, archived/fallback, checklist, and invalid-color widget render states.
- Built, signed, and installed the HKM release APK on the connected Pixel 6 Pro while preserving app data.

## 1.0 — 2026-08-09

### Fixed

- Fixed the crash when returning from **My goals** to **Today**. The spring tab indicator can no longer feed a negative value into Compose padding.
- Hardened the bottom-tab indicator against both left and right spring overshoot, including unexpectedly narrow layouts.
- Corrected contrast and system-bar handling across light and dark mode.
- Removed remaining wallpaper-driven purple and pale-green interface colors; app chrome now stays deliberately monochrome.

### Experience and interface

- Reworked the app around a clean Today focus: the current flame and streak are the visual priority.
- Added polished tab slide transitions and a floating, spring-driven segmented bottom navigator.
- Added a deliberate check-in ignition sequence with animation and haptic feedback.
- Refined button sizing, spacing, status-bar integration, dialogs, settings controls, and color selection clarity.
- Added consistent Lucide-style icons throughout the app with no emoji-based UI icons.
- Updated the adaptive launcher icon with a smaller visual crop and Android monochrome/dynamic-color support for the app icon only.

### Goals and daily flow

- Added goal creation, editing, archiving, restoring, and focused-goal management.
- Added two goal modes: a simple daily goal or a daily checklist.
- Added compact task shortcuts on Today and a focused daily task dialog.
- Expanded flame-only color choices to twelve clearly selectable colors.
- Added reminder, history, and progress interactions with haptic feedback.

### Data and widget

- Added JSON file import and export from Settings.
- Extended JSON backups to preserve goal mode, checklist items, and daily task completion state, while keeping older backups compatible.
- Preserved the existing widget behavior and data synchronization path.

### Quality checks

- Added regression coverage for tab-indicator bounds and checklist JSON persistence.
- Verified all debug unit tests and Android lint before release packaging.
