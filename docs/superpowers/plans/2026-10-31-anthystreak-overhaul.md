# AnthyStreak Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace harsh notifications, ship a full bilingual (EN/VI) polished app with native Settings, bulletproof widgets, predictive back, and six finished features.

**Architecture:** Keep the single-activity Compose app. Add a lightweight in-app route state (`HOME`, `SETTINGS`, `INSIGHTS`) driven by predictive back instead of a nav library. Localize through Android resources plus a stored `AppLanguage` that produces a localized `Context` for Compose, notifications, widgets, and receivers. Pure calculators (`StreakCalculator`, `InsightsCalculator`, `MilestoneRules`) stay Android-free and unit-tested.

**Tech Stack:** Kotlin 2.2.10, Compose Material 3, activity-compose 1.13, Phosphor icons 1.0.0, SharedPreferences JSON, AppWidget RemoteViews, TileService.

**Spec:** Approved scope from the 2026-10-31 conversation: 4 technical upgrades + 6 features.

## Global Constraints

- EN + VI for every user-facing string; no emoji anywhere; Phosphor Light icons only.
- No backend, no analytics, no network calls; data stays on device.
- Keep all approved motion; never shorten or skip the flame ignition ritual.
- 48dp minimum touch targets; light/dark parity; edge-to-edge with insets.
- Predictive back with progress everywhere a sub-screen/dialog can be dismissed.
- Widget must render cleanly at 56x48dp and every larger crop bucket.
- Never expose the Telegram/R2 credentials from the ARW skill; never commit them.

## Review Focus

- A missed day while paused must not create a debt or inflate the streak.
- Switching language must update every visible string without stale English.
- A 1x1 widget at 48dp height must not clip the streak count or actions.
- Notification tap on a paused/completed goal must not produce a nag.
- Back from Settings/Insights must never exit the app or skip the home tab.

---

## File Structure

- Create `data/AppLanguage.kt` — language enum + localized context.
- Create `ui/theme/AppIcons.kt` — Phosphor aliases.
- Create `ui/components/AppPredictiveBack.kt` — handler + visual modifier.
- Create `domain/InsightsCalculator.kt` — pure insight stats.
- Create `domain/MilestoneRules.kt` — thresholds and next target.
- Create `domain/HabitTemplates.kt` — template catalog.
- Create `ui/settings/SettingsScreen.kt` — full-screen preferences.
- Create `ui/insights/InsightsScreen.kt` — heatmap and stats.
- Create `ui/components/MilestoneOverlay.kt` — celebration.
- Create `ui/components/TemplatePickerDialog.kt` — template chooser.
- Create `receiver/StreakTileService.kt` — QS one-tap check-in.
- Create `res/xml/shortcuts.xml`, `res/values-vi/strings.xml`, notification/tile/shortcut vectors.
- Modify `MainActivity`, `MainScreen`, `StreakViewModel`, `HabitRepository`, `Habit`, `StreakCalculator`, notification helper/receiver, widget provider/render state/all layouts, widget configuration, `GoalRowPresentation`, manifest, Gradle.
- Tests: extend `StreakCalculatorTest`; add `InsightsCalculatorTest`, `MilestoneRulesTest`, `PauseModeTest`, update widget/render/presentation tests.

---

### Task 1: Foundation (language, icons, routes, prefs)

**Files:** `data/AppLanguage.kt`, `ui/theme/AppIcons.kt`, `ui/components/AppPredictiveBack.kt`, `HabitRepository`, `StreakViewModel`, `app/build.gradle.kts`, manifest.

- [ ] Add Phosphor dependency and `AppIcons` aliases; delete `LucideIcons` after all references move.
- [ ] Add `AppLanguage` (`SYSTEM`, `ENGLISH`, `VIETNAMESE`) with `localizedContext`.
- [ ] Persist `settings_app_language`, `settings_reduce_motion`, `settings_reminders_enabled`, `settings_rescue_enabled`; expose state in the ViewModel.
- [ ] Add `AppPredictiveBackHandler` + `Modifier.appPredictiveBackVisual(progress)`.
- [ ] Set `android:enableOnBackInvokedCallback="true"`.
- [ ] Run `./gradlew testDebugUnitTest`; commit.

### Task 2: Bilingual notification rewrite

**Files:** `helper/NotificationHelper.kt`, `receiver/HabitReminderReceiver.kt`, strings, `res/drawable/ic_notification_streak.xml`.

- [ ] Add friendly channel/reminder/rescue strings in EN + VI.
- [ ] Notify with the localized context, Phosphor-style monochrome flame, goal accent color.
- [ ] Skip notifications for completed or paused goals; respect both toggles.
- [ ] Replace two aggressive debt nags with one calm 19:00 rescue reminder.
- [ ] Add unit-testable `NotificationCopy` selection; verify with `testDebugUnitTest`.

### Task 3: Widget hardening

**Files:** all `widget_layout_*.xml`, `WidgetRenderState`, `WidgetLayoutSpec`, provider, widget strings.

- [ ] Redesign TILE for 56x48: 26dp flame + auto-sized count + label, no detail rows.
- [ ] Add auto-size text, maxLines/ellipsize, weights, and min-height-safe spacing to every layout.
- [ ] Localize eyebrow/status/action/labels and add paused state (`ON BREAK`).
- [ ] Keep responsive variants and ignition sequence intact.
- [ ] Update `WidgetRenderStateTest`, `WidgetLayoutSpecTest`; run tests.

### Task 4: Settings + Insights + predictive back

**Files:** new screens, `MainScreen`, `StreakViewModel`, manifest.

- [ ] Add in-app route state and a header entry for Insights + Settings.
- [ ] Full-screen Settings: theme, language, haptics, reduce motion, notification toggles + permission, data export/import/reset, about.
- [ ] Insights: 8-week heatmap, current/best streak, total check-ins, consistency, weekday pattern, paused/debt counts.
- [ ] Predictive back with progress for routes and GOALS -> TODAY; root back exits.
- [ ] Respect reduce motion by replacing sliding transitions with fades and skipping the ignition delay.
- [ ] Unit-test `InsightsCalculator`; run tests.

### Task 5: Pause, milestones, templates, shortcuts, tile

**Files:** `Habit`, `StreakCalculator`, ViewModel, MainScreen, `MilestoneRules`, `HabitTemplates`, shortcuts XML, `StreakTileService`, manifest.

- [ ] Add `pausedDates` with JSON compatibility; make reconcile/unclaimed/calculate/longestRun pause-aware.
- [ ] Pause/resume today in the Today screen; show paused banner; suppress reminders/debt; widget shows break state.
- [ ] Milestones 3/7/14/30/60/100/180/365 with a no-emoji celebration overlay and haptic.
- [ ] Template picker fills name/note/tasks/theme/color on new goals.
- [ ] Static launcher shortcuts (Today, Insights) and a Quick Settings check-in tile.
- [ ] Add tests for pause math, milestone thresholds, template catalog; run tests.

### Task 6: Full UI localization sweep

- [ ] Move every remaining hardcoded string (MainScreen dialogs, widget picker, accessibility labels, snacks) into resources.
- [ ] Complete `values-vi/strings.xml`; verify no `Text("...")` literals remain via grep.
- [ ] Localize widget configuration screen and switch it to Phosphor icons.
- [ ] Run tests + `assembleRelease`; fix compile errors.

### Task 7: Release (ARW)

- [ ] `git add` only task files; checkpoint commit.
- [ ] Build signed release; check size; deliver per ARW (Telegram direct <=50MB, R2/storage.to otherwise) with a Vietnamese changelog.
- [ ] Report what could not be runtime-verified without a device/emulator.
