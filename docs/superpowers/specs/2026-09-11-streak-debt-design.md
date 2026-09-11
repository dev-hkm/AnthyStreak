# Streak Debt Design

## Goal

Automatically record missed past goal days as debt while preserving the user's streak, with a restrained visual treatment that matches AnthyStreak's existing editorial Material 3 UI.

## Behavior

- A goal never receives debt for today or for a date before its `startDate`.
- When data is loaded or a goal is checked in, every past date from `startDate` through yesterday is reconciled.
- A date is added to `debts` only when it is neither completed nor already protected/debted.
- A check-in records only the selected date as completed and removes that date from `debts` if present.
- `debts` remain protected dates for streak calculation, so a missed day does not reset the streak.
- Existing backup JSON remains compatible because `Habit.debts` already exists.
- Widgets use the same `Habit` and `StreakCalculator` state, so their streak remains consistent automatically.

## UI

- Keep the hero flame/bonsai, count, check-in CTA, and secondary navigation unchanged.
- When debt exists, show a compact tertiary-colored badge under the streak label: `N ngày đang nợ · streak vẫn được giữ`.
- Keep the existing weekly history markers, but label debt markers with a short legend so the tertiary color is understandable.
- Update the history copy so it explains that missed days become debt instead of implying the chain simply resets.
- Do not add a repayment workflow, new tab, new settings, or additional dashboard surface in this change.

## Architecture

- Add a pure `StreakCalculator.reconcileDebts(...)` helper for deterministic date-range reconciliation.
- Make `StreakViewModel` normalize each habit through that helper before publishing or persisting it.
- Reuse the normalized `debts` list in widget rendering without adding widget-specific debt logic.

## Acceptance criteria

1. Missed dates are persisted exactly once and today is never auto-debted.
2. A streak calculated across completed and debted dates remains continuous.
3. Check-in and widget check-in keep existing behavior and do not erase older debts.
4. The main screen communicates the debt state without making the basic streak ritual noisy.
5. Existing unit tests plus new debt tests pass.

