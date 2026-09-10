# AnthyStreak

> A quiet, intentional habit tracker built around one daily promise at a time. Designed with custom vector physics, warm typography, and responsive homescreen widgets for Android.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-green?style=flat-square&logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM-blue?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Release](https://img.shields.io/github/v/release/dev-hkm/AnthyStreak?style=flat-square)](https://github.com/dev-hkm/AnthyStreak/releases)

---

## The Idea

Most habit trackers out there feel like spreadsheets in disguise. Endless setup forms, noisy gamification badges, and overwhelming charts that distract you from the only thing that actually matters: **showing up today**.

AnthyStreak was built with a much simpler mindset:
- **One hero focus at a time:** You don't need 20 habits competing for attention. Focus on the core promise that shapes your day.
- **Physical, satisfying rituals:** Checking in shouldn't be a cold checkbox tap. The app rewards you with tactile weight, bespoke canvas motion, and visual feedback that feels alive.
- **No cloud lock-in or accounts:** Your data stays on your device. Portable JSON backups when you need them, zero background tracking when you don't.

---

## Visual Themes

### 1. The Living Flame
The signature AnthyStreak visual. Drawn with 4 distinct depth layers (deep ember body, mid glow, white-hot core, and hot nucleus).
- **Idle Motion:** Calm breathing physics with a high-frequency harmonic tip flutter.
- **Ignition Ritual (1.8s):** A white-hot bloom, expanding shockwave rings, rising ember sparks, and subtle haptic feedback that grounds every completed day.

### 2. The Bonsai Tree (Growth Progression)
For people who prefer organic, steady growth over fire. Your bonsai evolves in lockstep with your streak count:
- **Seed (Day 0):** A glazed ceramic pot with fertile soil, cradling a dormant seed glowing with potential.
- **Sprout (Days 1–3):** A fresh curved green shoot unfurling its first two cotyledon leaves.
- **Sapling (Days 4–7):** The stem thickens into young wood, branching out with vibrant leaves.
- **Bonsai (Days 8–20):** Classic *Moyogi* S-curved weathered trunk adorned with dense *Tamamono* foliage clouds.
- **Blooming Masterpiece (Days 21+):** A grand mature bonsai bursting with glowing blossoms that mirror your chosen goal accent.
- **Nurture Ritual:** Check-in drops a crystalline dew droplet onto the soil, producing a gentle splash ripple and spring bounce.

---

## Homescreen Widgets

A habit tracker you have to open every time quickly becomes a habit you forget. AnthyStreak features a complete widget suite for your Android launcher:

- **6 Responsive Size Buckets:** Automatically adapts across `Tile`, `Strip`, `Compact`, `Square`, `Wide`, and `Hero` layouts.
- **4 Visual Styles:** `Ember`, `Focus`, `Checklist`, and `Poster`.
- **Per-Widget Goal Targeting:** Place multiple widgets on your homescreen, each linked to a distinct goal.
- **Instant Launcher Check-In:** Complete today directly from the homescreen with an on-widget 8-frame ignition ritual and private token verification.
- **Day/Night Responsive:** Dynamic light and dark mode integration matching system themes without relaunching the app.

---

## Tech Stack & Architecture

- **UI Framework:** 100% Jetpack Compose (Material 3) with pure Canvas drawing for custom physics and visual themes.
- **Typography:** Bundled offline [EB Garamond](https://fonts.google.com/specimen/EB+Garamond) family (400–800 weights) for a classic, literary editorial aesthetic.
- **Icons:** Handcrafted vector Lucide icon set running directly on Compose vector primitives without extra asset bloat.
- **Persistence:** Lightweight JSON serialization with atomic `SharedPreferences` storage and schema-safe backward compatibility.
- **Widget Engine:** `AppWidgetProvider` + custom `RemoteViews` layouts tailored for modern Android 12+ responsive size mapping.

---

## Building from Source

### Prerequisites
- Android Studio Ladybug / Meerkat or Android SDK 36
- JDK 17+ (or bundled Android Studio JBR)

### Clone & Build
```bash
git clone https://github.com/dev-hkm/AnthyStreak.git
cd AnthyStreak

# Run local unit tests
./gradlew testDebugUnitTest

# Assemble signed release APK (or unsigned)
./gradlew assembleRelease
```

---

[https://khanhminh.web.app](https://khanhminh.web.app)
