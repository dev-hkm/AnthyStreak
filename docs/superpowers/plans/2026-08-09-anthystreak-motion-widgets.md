# AnthyStreak Motion and Widget System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship AnthyStreak 1.2.0 with physical flame motion, a centered Today hero, EB Garamond, improved goal cards, and a responsive multi-style widget system that follows system theme changes.

**Architecture:** Keep streak persistence in `HabitRepository`/`StreakViewModel`, isolate animation math and widget layout decisions in pure Kotlin models, and let Compose/RemoteViews render those models. Widget configuration remains device-local and widgets always finish in an authoritative fully rendered state.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android RemoteViews/AppWidget, JUnit 4, Android Gradle Plugin, static TTF resources.

## Global Constraints

- Preserve package `com.hkm.streakwidget` and all existing habit/backup compatibility.
- Rename only the product-facing label to `AnthyStreak`.
- App chrome remains monochrome; goal color is restricted to flame art.
- Main flame is 208dp; ignition ritual is 1,800ms and commits the captured goal around 620ms.
- Widget offers six responsive buckets and four stored styles; widget IDs/styles remain excluded from JSON backup.
- Widget continuously correct final state is mandatory; intermediate ignition frames are best-effort because launchers may coalesce RemoteViews updates.
- Use only Lucide-style app icons and no emoji.
- Version is `versionCode 3`, `versionName 1.2.0`.

---

### Task 1: Deterministic physical flame model

**Files:**
- Create: `app/src/main/java/com/hkm/streakwidget/ui/components/FlameMotion.kt`
- Create: `app/src/test/java/com/hkm/streakwidget/ui/components/FlameMotionTest.kt`
- Modify: `app/src/main/java/com/hkm/streakwidget/ui/components/CustomFlame.kt`

**Interfaces:**
- Produces: `FlameMotionFrame`, `IgnitionStage`, `ignitionFrameAt(elapsedMs: Int)`, and `steadyFrameAt(elapsedMs: Long, seed: Int)`.
- Consumed by: `CustomFlame` and Today check-in orchestration.

- [ ] **Step 1: Write failing model tests**

```kotlin
@Test fun ignition_has_expected_stage_boundaries() {
    assertEquals(IgnitionStage.COMPRESS, ignitionStageAt(99))
    assertEquals(IgnitionStage.BLOOM, ignitionStageAt(100))
    assertEquals(IgnitionStage.SETTLED, ignitionStageAt(1800))
}

@Test fun motion_is_bounded_and_deterministic() {
    val frame = ignitionFrameAt(420)
    assertTrue(frame.scaleY in .93f..1.14f)
    assertEquals(steadyFrameAt(1234, 7), steadyFrameAt(1234, 7))
}
```

- [ ] **Step 2: Run RED test**

Run: `./gradlew.bat --no-daemon --max-workers=1 testDebugUnitTest --tests "*FlameMotionTest"`

Expected: compilation failure because the motion API does not exist.

- [ ] **Step 3: Implement pure motion functions and render them**

Use bounded damped interpolation for compress/bloom/recoil/settle phases, two incommensurate steady harmonics, normalized Bézier deformation, anchored base, contained shrinking embers, and one event-resettable ignition clock.

- [ ] **Step 4: Run GREEN test**

Run the Task 1 test command and expect all `FlameMotionTest` cases to pass.

### Task 2: Today hero, explicit goal check-in, and horizontal palette

**Files:**
- Modify: `app/src/main/java/com/hkm/streakwidget/ui/StreakViewModel.kt`
- Modify: `app/src/main/java/com/hkm/streakwidget/ui/screens/MainScreen.kt`
- Create: `app/src/main/java/com/hkm/streakwidget/ui/screens/GoalPalette.kt`
- Create: `app/src/test/java/com/hkm/streakwidget/ui/screens/GoalPaletteTest.kt`
- Create: `app/src/test/java/com/hkm/streakwidget/ExplicitGoalCheckInTest.kt`

**Interfaces:**
- Produces: `GoalColorOption(name: String, color: Color, hex: String)` and `toggleToday(habitId: String)`.
- Consumes: Task 1 flame model/renderer.

- [ ] **Step 1: Write failing palette and target tests**

```kotlin
@Test fun palette_contains_22_unique_colors() {
    assertEquals(22, GoalPalette.options.size)
    assertEquals(22, GoalPalette.options.map { it.hex }.distinct().size)
}
```

The explicit goal test creates two goals, focuses the second, invokes the first ID, and asserts only the first history changes.

- [ ] **Step 2: Run RED tests**

Run: `./gradlew.bat --no-daemon --max-workers=1 testDebugUnitTest --tests "*GoalPaletteTest" --tests "*ExplicitGoalCheckInTest"`

Expected: missing palette API and ID-based toggle behavior.

- [ ] **Step 3: Implement the Today structure and event flow**

Move goal title/note to the top row opposite settings. Use a full-size Box so the 208dp flame/count hero remains centered and lower controls remain bottom-anchored. Capture habit ID on tap, advance the ignition timeline, commit that ID near 620ms, and finish at 1,800ms. Replace `FlowRow` with a semantics-rich snapping `LazyRow` containing all 22 options.

- [ ] **Step 4: Run GREEN tests and existing streak tests**

Run: `./gradlew.bat --no-daemon --max-workers=1 testDebugUnitTest --tests "*GoalPaletteTest" --tests "*ExplicitGoalCheckInTest" --tests "*StreakCalculatorTest"`

### Task 3: EB Garamond and AnthyStreak branding

**Files:**
- Create: `app/src/main/res/font/eb_garamond_regular.ttf`, `eb_garamond_italic.ttf`, `eb_garamond_medium.ttf`, `eb_garamond_medium_italic.ttf`, `eb_garamond_semi_bold.ttf`, `eb_garamond_semi_bold_italic.ttf`, `eb_garamond_bold.ttf`, `eb_garamond_bold_italic.ttf`, `eb_garamond_extra_bold.ttf`, `eb_garamond_extra_bold_italic.ttf`
- Create: `app/src/main/res/font/eb_garamond_family.xml`
- Create: `app/src/main/assets/licenses/eb_garamond_ofl.txt`
- Modify: `app/src/main/java/com/hkm/streakwidget/ui/theme/Type.kt`
- Modify: all widget layout TextViews or shared widget styles under `res/values` and `res/values-v26`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: `EbGaramondFontFamily` and complete Material 3 `Typography`.

- [ ] **Step 1: Add official pinned font resources and license**

Fetch ten static TTFs from `octaviopardo/EBGaramond12` commit `106a4a6d377987459ae5e68673a4570f13b957fb`; retain exact upstream bytes and OFL license.

- [ ] **Step 2: Map every typography role and widget fallback**

Map weights 400/500/600/700/800 and both styles. Apply the family to all Material typography roles, replace weight 900 usages with 800, use custom widget font on API 26+, and serif fallback on API 24-25.

- [ ] **Step 3: Rename and bump version**

Set app label to `AnthyStreak`, `versionCode = 3`, and `versionName = "1.2.0"` without changing application ID.

- [ ] **Step 4: Compile resources**

Run: `./gradlew.bat --no-daemon --max-workers=1 processDebugResources compileDebugKotlin`

Expected: BUILD SUCCESSFUL with all fonts and labels resolved.

### Task 4: My Goals card redesign

**Files:**
- Modify: `app/src/main/java/com/hkm/streakwidget/ui/components/LucideIcons.kt`
- Modify: `app/src/main/java/com/hkm/streakwidget/ui/screens/MainScreen.kt`
- Create: `app/src/main/java/com/hkm/streakwidget/ui/screens/GoalRowPresentation.kt`
- Create: `app/src/test/java/com/hkm/streakwidget/ui/screens/GoalRowPresentationTest.kt`

**Interfaces:**
- Produces: canonical Lucide `Ellipsis` and accessible goal-card behavior.

- [ ] **Step 1: Add failing metadata and action tests**

Add tests for pure `goalRowPresentation(habit, today)` proving checklist progress, completion text, singular/plural streak labels, and `GoalRowPrimaryAction.MANAGE` for archived cards so a row tap cannot restore data.

- [ ] **Step 2: Run RED test**

Run the exact new test with `testDebugUnitTest`; expect missing metadata API.

- [ ] **Step 3: Implement cards**

Use a 42dp lightweight colored flame, 19sp title, left status/checklist progress, right `N day(s)` metadata, 48dp Ellipsis manage target, and explicit restore-only archived action. Add restrained spring press and item placement animation.

- [ ] **Step 4: Run GREEN test**

Run the new test and then `testDebugUnitTest`.

### Task 5: Responsive widget styles, animation, and theme refresh

**Files:**
- Create: `app/src/main/java/com/hkm/streakwidget/widget/WidgetLayoutSpec.kt`
- Create/modify: curated widget XML layouts and transition drawables under `app/src/main/res/layout*`, `drawable`, and `anim`
- Modify: `app/src/main/java/com/hkm/streakwidget/data/HabitRepository.kt`
- Modify: `app/src/main/java/com/hkm/streakwidget/widget/WidgetConfigurationActivity.kt`
- Modify: `app/src/main/java/com/hkm/streakwidget/widget/StreakAppWidgetProvider.kt`
- Modify: `app/src/main/res/xml/streak_widget_info.xml`
- Create: `app/src/test/java/com/hkm/streakwidget/widget/WidgetLayoutSpecTest.kt`
- Modify: `app/src/test/java/com/hkm/streakwidget/widget/WidgetRenderStateTest.kt`

**Interfaces:**
- Produces: `WidgetSize`, `WidgetStyle`, `resolvedStyle`, per-widget style persistence, responsive size map, and best-effort ignition frames.

- [ ] **Step 1: Write failing layout/style tests**

```kotlin
@Test fun size_classifier_handles_tall_and_wide_independently() {
    assertEquals(WidgetSize.SQUARE, WidgetLayoutSpec.classify(112, 168))
    assertEquals(WidgetSize.WIDE, WidgetLayoutSpec.classify(240, 88))
}

@Test fun tiny_sizes_render_ember_without_forgetting_preference() {
    assertEquals(WidgetStyle.EMBER, resolvedStyle(WidgetSize.TILE, WidgetStyle.POSTER, false))
}
```

Also test style default, saved value, invalid-value fallback, and removal on widget deletion.

- [ ] **Step 2: Run RED widget tests**

Run: `./gradlew.bat --no-daemon --max-workers=1 testDebugUnitTest --tests "*WidgetLayoutSpecTest" --tests "*WidgetRenderStateTest"`

Expected: missing size/style APIs or failing old classification.

- [ ] **Step 3: Implement persistence, configuration, and layouts**

Offer Ember, Focus, Checklist, and Poster previews after goal selection. Render six breakpoint hierarchies, use Android 12 size maps with legacy fallback, remove style state on deletion, and preserve selected style across resize.

- [ ] **Step 4: Implement interaction and theme refresh**

Move widget background, text, action, and neutral flame chrome to day/night-qualified resources so an existing `RemoteViews` re-inflates in the current system mode without waking the app; where API 31 runtime setters remain necessary, supply both light and dark values. Do not add an `ACTION_CONFIGURATION_CHANGED` manifest receiver because Android does not deliver that action to manifest-declared components. On valid widget check-in, persist once, then issue 3-4 partial updates during 600-750ms before a final authoritative render. Keep duplicate action idempotent and always terminate the broadcast async result.

- [ ] **Step 5: Run GREEN widget tests**

Run Task 5 tests and the complete unit-test suite.

### Task 6: Integration, visual smoke test, changelog, and release

**Files:**
- Modify: `CHANGELOG.md`
- Create release artifact outside generated intermediates without overwriting 1.1.0.

- [ ] **Step 1: Run full automated verification**

Run: `./gradlew.bat --no-daemon --max-workers=1 clean testDebugUnitTest assembleDebug assembleRelease`

Expected: BUILD SUCCESSFUL and zero failing tests.

- [ ] **Step 2: Inspect generated resources and APK**

Verify app label, package ID, version 1.2.0, embedded font files/license, widget receiver metadata, manifest configuration-change action, and no accidental purple/green theme tokens.

- [ ] **Step 3: Device smoke test when connected**

Install the signed build, open Today and My Goals, resize/style at least representative widget ratios, toggle system dark/light, and check one goal in from app and widget. Capture screenshots/logcat evidence without claiming unsupported launcher behavior.

- [ ] **Step 4: Build and verify production artifact**

Align the unsigned release, sign with the HKM keystore and alias already authorized by the user, and run `apksigner verify --verbose --print-certs`. Do not print passwords or private material.

- [ ] **Step 5: Publish release**

Generate a user-facing 1.2.0 changelog from the actual diff, upload `Widget-1.2.0-production.apk` to Catbox, and post the download link plus changelog to ntfy topic `sendlink`.
