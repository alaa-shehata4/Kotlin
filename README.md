# CareBrief — Care Plan Assistant

CareBrief turns unstructured daily care notes into structured summaries and
editable care-plan drafts. Caregivers record what they observe; the app
highlights recurring patterns, drafts a plan a human reviews and approves,
and turns the approved plan into practical daily tasks.

> **Assistive prototype, not a medical device.** Every AI output is labeled
> **DRAFT — REVIEW REQUIRED**. Nothing here diagnoses, treats, or replaces
> professional judgment.

## Problem

Caregivers record information as unstructured daily notes. The important
signals — reduced appetite mentioned three days running, broken sleep,
creeping fatigue — stay buried in prose, and turning them into a
structured plan takes time nobody has.

## Solution

```
DAILY NOTES → AI ANALYSIS → STRUCTURED INSIGHTS → CARE-PLAN DRAFT
        → HUMAN REVIEW → ACTIONABLE TASKS → DASHBOARD PROGRESS
```

Open the app, pick Sarah Johnson, read her notes, tap **Analyze**, review the
evidence-backed summary, edit the suggested plan, approve it, and work the
generated tasks. The whole story is demonstrable from the APK with no
backend and no network.

## Features

- **Dashboard** — greeting, today's metrics, attention items, recent activity
- **Care recipients** — searchable list with plan status and pending tasks
- **Recipient profile** — observations, concerns, plan, tasks, activity timeline
- **Daily notes** — free-text observations with category chips and optional
  structured fields (mood, mobility, appetite, sleep); filterable timeline
- **AI analysis** — staged offline processing with deterministic results
- **AI summary** — key observations, pattern counts, and per-insight evidence
  ("Observed in 3 of 5 recent notes" — never fake confidence percentages)
- **Care-plan draft & editor** — editable goal, reason, actions
  (add/edit/delete/reorder), monitoring indicators, priority, review date
- **Approval gate** — explicit "reviewed" confirmation before a plan goes active
- **Tasks** — generated from the plan, filterable (today / upcoming /
  completed), completable with undo
- **Settings** — theme, start screen, notifications with local reminders,
  demo/remote AI selection, JSON export, demo reset, privacy notice
- **Onboarding** — three-page intro with skip, completed flag stored locally
- **Offline-first** — Room database + DataStore; demo AI works without internet

## Architecture

Clean-ish MVVM with a repository seam, one small ViewModel per screen, each
exposing a single explicit `StateFlow` screen state
(`Loading` / `Content` / `Empty` / `Error`):

```
presentation/   Compose screens + ViewModels (dashboard, recipients, notes,
                summary, careplan, tasks, settings, …)
domain/         Pure logic (validators, timeline grouping, plan edit ops)
data/           Room repositories, DataStore settings, AI providers,
                demo dataset, export
core/           Design system, navigation, database entities/DAOs,
                responsive helpers, connectivity observer
```

- UI state flows one way: `Repository Flow → ViewModel StateFlow → Compose`.
- Database writes are `suspend` and transactional; the UI never blocks.
- Navigation: bottom bar on phones, side rail on large screens (≥ 840 dp).

## Tech Stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- Room (local-first storage, migrations 1→2→3), DataStore Preferences
- Coroutines + Flow/StateFlow, WorkManager (local reminders)
- Gradle Kotlin DSL, minSdk 26, target/compileSdk 34
- JUnit 4 + `kotlinx-coroutines-test` (66 unit tests), Compose UI tests

## AI Architecture

The `AiCareAssistant` interface (`summarizeNotes`, `analyzePatterns`,
`generateCarePlanDraft`, `generateSuggestedTasks`, …) decouples the app from
any provider:

1. **Demo provider** (`DemoAiCareAssistant`) — deterministic, offline,
   evidence-linked output. Powers the APK with no API keys.
2. **Remote provider** (`RemoteAiCareAssistant`) — disabled stub. To connect a
   real model later: implement the interface against your **backend** (never
   ship secrets in the APK), handle its errors as the existing `Error` screen
   states do, and flip the provider in Settings.

## Running

Prerequisites: Android Studio (or command-line tools), JDK 17+.

```bash
# Unit tests
./gradlew :app:testDebugUnitTest

# Installable APK (debug)
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# On-device UI tests (needs an emulator/device)
./gradlew :app:connectedDebugAndroidTest
```

Install the APK on a device/emulator (`adb install -r
app/build/outputs/apk/debug/app-debug.apk`), skip or complete onboarding,
and follow the demo: **Sarah Johnson → notes → Analyze → summary → care
plan → edit → approve → tasks → complete → dashboard**.

> Note: build with JDK 17 or 21. JDK 25 breaks the Kotlin Gradle plugin
> version used here.

## APK

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (signed, zipaligned,
  verified with `apksigner`)
- Release APK: `app/build/outputs/apk/release/app-release.apk` — built with
  `./gradlew :app:assembleRelease`, then zipaligned and signed for
  installability. It currently carries the debug key, which is fine for a
  prototype demo; sign with a real release key before any distribution.

## Disclaimer

CareBrief is an **assistive documentation prototype**. AI-generated content
is provided as a **draft requiring review by an appropriate human
professional** and must not be used as medical advice, diagnosis, or
treatment. Demo profiles are fictional; all data stays on the device unless
you explicitly export it.
