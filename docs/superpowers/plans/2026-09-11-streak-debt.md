# Streak Debt Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically record missed past goal days as debt while preserving streak continuity and showing the state in the existing AnthyStreak UI.

**Architecture:** Keep debt reconciliation pure in `StreakCalculator`, normalize habits in `StreakViewModel`, and reuse the existing `Habit.debts` path consumed by app history and widgets. Add only a compact debt indicator and explanatory copy to the current Today/history surfaces.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android `SharedPreferences` JSON persistence, JUnit 4.

## Global Constraints

- Do not create debt for today, before `Habit.startDate`, or before the first completed check-in.
- Keep completed dates and debt dates disjoint.
- Preserve streaks through debt dates using the existing protected-date calculation.
- Do not add a repayment flow, new tab, or unrelated refactor.
- Release delivery is Release-only, signed with the authorized HKM keystore, R2-first, storage.to fallback only on R2 failure, then exactly one `filesend` ntfy message.

### Task 1: Define debt reconciliation behavior

**Files:**
- Modify: `app/src/test/java/com/hkm/streakwidget/StreakCalculatorTest.kt`
- Modify: `app/src/main/java/com/hkm/streakwidget/domain/StreakCalculator.kt`

**Interfaces:**
- Produce `StreakCalculator.reconcileDebts(startDate: String, completedDates: List<String>, existingDebtDates: List<String>, today: String = todayString()): List<String>`.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test
fun `reconciles missed past dates without debting today`() {
    val result = StreakCalculator.reconcileDebts(
        startDate = "2026-08-07",
        completedDates = listOf("2026-08-07"),
        existingDebtDates = emptyList(),
        today = "2026-08-10"
    )

    assertEquals(listOf("2026-08-08", "2026-08-09"), result)
}

@Test
fun `preserves existing debts and never duplicates completed dates`() {
    val result = StreakCalculator.reconcileDebts(
        startDate = "2026-08-07",
        completedDates = listOf("2026-08-07", "2026-08-09"),
        existingDebtDates = listOf("2026-08-08", "2026-08-08"),
        today = "2026-08-10"
    )

    assertEquals(listOf("2026-08-08"), result)
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `.\gradlew.bat --no-daemon --max-workers=1 testDebugUnitTest --tests com.hkm.streakwidget.StreakCalculatorTest`

Expected: compilation/test failure because `reconcileDebts` does not exist yet.

- [ ] **Step 3: Implement the minimal date-range helper**

Implement a calendar walk from the first valid completed date (falling back to existing valid debts when history is empty) up to but excluding parsed `today`; add valid dates absent from both completed and existing debt sets, return sorted unique ISO dates, and preserve only valid debt dates that are still before today.

- [ ] **Step 4: Run the focused test and verify it passes**

Run the same focused Gradle test command. Expected: PASS, including all existing streak tests.

- [ ] **Step 5: Commit the domain checkpoint**

```powershell
git add app/src/test/java/com/hkm/streakwidget/StreakCalculatorTest.kt app/src/main/java/com/hkm/streakwidget/domain/StreakCalculator.kt
git commit -m "feat: add streak debt reconciliation"
```

### Task 2: Reconcile debts in the app flow

**Files:**
- Modify: `app/src/main/java/com/hkm/streakwidget/ui/StreakViewModel.kt`
- Modify: `app/src/main/java/com/hkm/streakwidget/widget/StreakAppWidgetProvider.kt`

**Interfaces:**
- `StreakViewModel.refreshStreak` returns a habit with normalized `debts` and recalculated `streakCount`.
- Widget check-in retains all older debts while removing only the checked date.

- [ ] **Step 1: Add a regression test for streak continuity through a debt date**

Add to `StreakCalculatorTest.kt`:

```kotlin
@Test
fun `keeps streak continuous across a reconciled missed day`() {
    val debts = StreakCalculator.reconcileDebts(
        startDate = "2026-08-07",
        completedDates = listOf("2026-08-07", "2026-08-09"),
        existingDebtDates = emptyList(),
        today = "2026-08-10"
    )

    assertEquals(3, StreakCalculator.calculate(
        completedDates = listOf("2026-08-07", "2026-08-09"),
        protectedDates = debts,
        today = "2026-08-10"
    ))
}
```

- [ ] **Step 2: Run the test and verify the intended failure/coverage**

Run the focused test command and confirm the new behavior is represented before changing the ViewModel.

- [ ] **Step 3: Normalize habits in `refreshStreak`**

Call `reconcileDebts` with the habit's `startDate`, `completionHistory`, `debts`, and the current date; copy the normalized debt list and calculate the streak from it. This path is already used by initial load, create/update, restore, and ordinary check-in persistence.

- [ ] **Step 4: Preserve debts in widget check-in**

Use the normalized target before calculating `updatedTarget`, remove only `today` from its debt list, and leave older debt dates untouched.

- [ ] **Step 5: Run all unit tests**

Run: `.\gradlew.bat --no-daemon --max-workers=1 testDebugUnitTest`

Expected: all tests pass.

### Task 3: Add restrained debt presentation

**Files:**
- Modify: `app/src/main/java/com/hkm/streakwidget/ui/screens/MainScreen.kt`
- Modify: `app/src/test/java/com/hkm/streakwidget/ui/screens/GoalRowPresentationTest.kt` only if the existing presentation contract needs a debt field.

- [ ] **Step 1: Add the compact debt badge to `FocusCard`**

Render only when `habit.debts.distinct().isNotEmpty()`, using the existing tertiary container/on-tertiary-container colors, rounded shape, small typography, and copy such as `2 days owed · streak protected`.

- [ ] **Step 2: Add a legend to the weekly history card**

Keep the current tertiary marker color and add a small `Debt` label beside the `This week` heading when the week contains a debt date.

- [ ] **Step 3: Correct the history explanation**

Replace the reset-oriented sentence with copy explaining that missed days are recorded as debt and the current streak remains protected.

- [ ] **Step 4: Run all unit tests**

Run: `.\gradlew.bat --no-daemon --max-workers=1 testDebugUnitTest`

Expected: PASS.

### Task 4: Document and package the completed Android change

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `README.md` only if the public behavior description needs the debt rule.

- [ ] **Step 1: Add a concise changelog entry**

Document automatic missed-day debt, streak preservation, and the new compact UI indicator.

- [ ] **Step 2: Create the named Git checkpoint**

Use a short changelog-derived commit such as:

```powershell
git add .
git commit -m "feat: preserve streaks with missed-day debt"
```

- [ ] **Step 3: Build only the Release APK**

Use `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` and `.\gradlew.bat :app:assembleRelease --no-daemon --max-workers=1`.

- [ ] **Step 4: Verify alignment and signature**

Run `zipalign` and `apksigner verify --verbose` against the final APK using the transient HKM keystore credentials; do not print credentials.

- [ ] **Step 5: Deliver through the standing workflow**

Upload the verified APK to Personal R2 `filesend`, verify the public URL and byte length, use current storage.to Init → presigned PUT → Confirm only if R2 fails, then send exactly one ntfy message to `filesend` with the APK name, final link, and concise changelog.
