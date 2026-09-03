# AnthyStreak Motion and Widget System Design

## Product direction

AnthyStreak remains a deliberately basic daily-streak app: opening Today should reveal one centered flame and the current number of days immediately. All secondary actions stay quiet, monochrome, and outside the hero. The only persistent goal-specific color is the flame itself.

The release renames the launcher/app label from `Widget` to `AnthyStreak`, keeps package identity and stored data compatible, and advances the app to version 1.2.0.

## Today screen

- The focused goal name moves to the top-left opposite the circular settings button. It uses EB Garamond at 26sp with at most two lines. Its optional motivation note sits directly below in one restrained line.
- The flame, streak number, and streak label form a hero group centered independently from the header and lower actions.
- The main flame canvas grows from 198dp to 208dp. Empty-state and compact flames do not grow.
- Checklist access remains near the hero only when tasks exist. Progress and check-in controls are anchored below the hero so they cannot displace its center.
- Screen and content transitions use short spring/fade motion. Reduced system animation scale must not leave the UI in an intermediate state.

## Flame motion

The existing independent repeating tweens are replaced by a deterministic motion model with two modes:

1. A quiet steady flame based on bounded, non-repeating-looking harmonic motion. It deforms the tip and control points while keeping the base anchored.
2. A single 1,800ms check-in ritual: compression, release/bloom, recoil, damped settle, and completed-state linger.

The check-in event captures the tapped goal ID. At approximately 620ms it commits that exact goal and fires confirmation haptic feedback, eliminating focus-change races. The button remains idempotent and disabled during the ritual. Embers remain within the canvas, shrink and fade as they rise, and appear only during the energetic phase.

The physical timeline and steady motion are pure functions so boundary continuity, value limits, deterministic output, and event targeting can be unit-tested without Compose.

## Flame palette

The 12 existing colors retain their exact hex values for compatibility. Ten curated options are appended: Sun Gold `#FFD45A`, Lime `#83D34B`, Emerald `#20C46A`, Aqua `#32CDB1`, Ice Cyan `#5DD7E5`, Cobalt `#3047C9`, Midnight `#29337A`, Fuchsia `#D84CC6`, Plum `#773A8F`, and Wine `#842F4F`.

The editor uses a horizontally scrolling, snap-assisted selector with named accessibility semantics, transparent cells, visible neighboring options, a monochrome selected outline, spring scale feedback, and haptics. Selecting a color changes only the flame.

## Typography

Official EB Garamond static files, pinned to upstream commit `106a4a6d377987459ae5e68673a4570f13b957fb`, are bundled offline for weights 400, 500, 600, 700, and 800 in regular and italic styles. The SIL OFL license is bundled.

Every Material 3 typography role uses EB Garamond. Existing weight 900 requests become explicit ExtraBold 800. Widget TextViews use an Android font-family resource on API 26+ and a safe serif fallback on API 24-25. System-owned surfaces such as notifications, status bars, launcher UI, and keyboards retain their system font.

## My Goals cards

- The leading gray streak-number circle becomes a lightweight flame using the goal color.
- The goal name is the primary line. Status or checklist progress sits below it, while the streak moves to compact right-aligned metadata such as `12 days`.
- Active management uses a Lucide Ellipsis button with a 48dp target because it opens Edit, Archive, and Delete actions. Archived rows retain the Lucide restore icon.
- Tapping an archived card never restores it accidentally; restoration is confined to the explicit restore button.
- Completed rows expose a check icon and text, not color alone. Card press, insertion, restore, and archive use restrained spring/fade motion.

## Responsive widget system

Each widget stores its selected goal, style, and action token locally. Widget placement/style remains device-local and is excluded from portable JSON backup.

Six responsive size buckets replace the current three-way `OR` classifier:

| Bucket | Minimum size | Typical grid shape |
| --- | ---: | --- |
| Tile | 56 x 56dp | 1x1 |
| Strip | 112 x 48dp | shallow 2x1 |
| Compact | 112 x 88dp | 2x1 |
| Square | 112 x 168dp | 2x2 |
| Wide | 240 x 88dp | 4x1 |
| Hero | 240 x 168dp | 4x2 and larger |

Four styles are offered during configuration:

- Ember: flame plus streak, available at every ratio.
- Focus: flame, goal title, state, and check-in; this is the default.
- Checklist: daily task progress is prominent and gracefully falls back to Focus for simple goals.
- Poster: centered large flame, oversized streak, and a quiet bottom action for Square and Hero sizes.

Very small sizes automatically render Ember hierarchy while preserving the selected style for later resizing. Android 12+ receives a `RemoteViews` size map; older versions use deterministic width-and-height classification.

## Widget interaction and system theme

Widget check-in validates and saves once, then attempts a 600-750ms sequence of partial RemoteViews updates: anticipation, ignition, overshoot, and settled state. The flame, count, and action label advance together. Launchers may coalesce intermediate updates, so the guaranteed contract is the correct settled state; no continuous background animation is used.

Widget chrome uses day/night-qualified resources directly inside `RemoteViews`; API 31+ additionally carries paired light/dark values where a runtime setter is required. The launcher can therefore re-inflate the already-delivered widget with the current system appearance even when the app process is not alive. The implementation must not rely on `ACTION_CONFIGURATION_CHANGED`, because Android does not deliver that action to manifest-declared receivers. Existing date, timezone, boot, package-replaced, resize, and app-data refresh paths remain supported.

## Verification and release

- Unit tests cover ignition stages and bounds, steady motion determinism, goal targeting, palette uniqueness, widget size classification, style fallback, repository style cleanup, render state, and theme-refresh routing where testable.
- All unit tests and the release build must pass with Java 21 from Android Studio JBR.
- The release is signed with the existing HKM production keystore, verified with Android build tools, installed/smoke-tested on the connected device when available, uploaded to Catbox, and sent to ntfy topic `sendlink` with a complete user-facing changelog.
