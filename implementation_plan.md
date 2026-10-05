# ROLE

You are a senior Android engineer, product designer, UX engineer, software architect, QA engineer, and AI application engineer.

Your task is to BUILD a complete, polished, production-quality Android application called:

# CareBrief — Care Plan Assistant

The application is inspired by the general concept of a CareBrief-style care-plan assistant:

> Caregivers record daily observations and care notes. The system analyzes those notes, summarizes the important information, identifies patterns and concerns, and proposes a structured care-plan draft that a human caregiver or professional can review and edit.

This is NOT intended to autonomously diagnose patients or make medical decisions.

The application must clearly position AI-generated information as a DRAFT / ASSISTIVE OUTPUT that requires human review.

---

# PRIMARY OBJECTIVE

The final deliverable is:

## A FUNCTIONAL ANDROID APK

The project must:

1. Build successfully.
2. Run on a real Android device/emulator.
3. Produce an installable APK.
4. Have a polished, modern, professional UI/UX.
5. Feel like a real healthcare product rather than a student project.
6. Work without requiring a backend for the core demonstration.
7. Have a clean architecture allowing a real AI/backend service to be connected later.
8. Include realistic demo data.
9. Include a complete end-to-end workflow.
10. Include loading, empty, success, error, validation, and offline states.
11. Avoid fake buttons that do nothing.
12. Avoid unfinished screens.
13. Avoid placeholder lorem ipsum.
14. Avoid unnecessary complexity that prevents building the APK.

The app should be demonstrable entirely from the APK.

---

# IMPORTANT PRODUCT PRINCIPLE

This is an AI-assisted care documentation and planning application.

The application MUST NOT present AI output as medical fact or diagnosis.

Use language such as:

* "AI-generated draft"
* "Suggested"
* "Potential concern"
* "Requires review"
* "Based on recorded notes"
* "Review before using"
* "Not a diagnosis"

Do NOT make claims such as:

* "The patient has..."
* "The patient definitely suffers from..."
* "Diagnosis: ..."
* "Treatment recommendation: ..."

The system assists the caregiver; it does not replace professional judgment.

---

# RECOMMENDED TECHNOLOGY STACK

Use this stack unless there is a strong technical reason to change it:

## Android

* Kotlin
* Jetpack Compose
* Material 3
* Android SDK
* Gradle Kotlin DSL

## Architecture

Use:

* MVVM
* Clean-ish layered architecture
* Repository pattern
* Kotlin Coroutines
* Kotlin Flow / StateFlow
* Dependency injection if useful, preferably lightweight

Suggested layers:

presentation/
domain/
data/
core/

Do not over-engineer the application.

---

# LOCAL STORAGE

The application must be local-first.

Use:

* Room Database

Store:

* Care recipients
* Daily notes
* Care-plan drafts
* Tasks
* AI analysis results
* App settings/preferences

The application should continue working when offline.

---

# AI ARCHITECTURE

DO NOT hard-code the entire application around a single AI provider.

Create an abstraction such as:

AiCareAssistant

with operations conceptually equivalent to:

* summarizeNotes()
* analyzePatterns()
* generateCarePlanDraft()
* generateSuggestedTasks()

Provide at least two implementations:

### 1. Demo / Local AI Provider

A deterministic mock provider that generates realistic outputs from the demo notes.

This ensures the APK works without API keys.

### 2. Remote AI Provider Interface

Create the architecture necessary for connecting to an actual LLM API later.

The remote implementation may initially be disabled or configured through environment/build configuration.

NEVER expose a secret API key directly inside the APK.

If an external AI service is implemented, design the architecture so secrets belong on a backend.

---

# VISUAL DESIGN DIRECTION

The application should look like a premium modern healthcare SaaS/mobile product.

Think:

* Calm
* Trustworthy
* Minimal
* Elegant
* Human
* Professional
* Modern
* Accessible

Avoid:

* Generic Android demo appearance
* Excessive gradients
* Neon colors
* Huge shadows
* Excessive rounded cards
* Clutter
* "AI gimmick" aesthetics
* Excessive animations
* Corporate dashboard ugliness

---

# DESIGN SYSTEM

Create a consistent design system.

Suggested visual direction:

### Primary

Deep healthcare blue / teal.

### Secondary

Soft mint / green.

### Background

Very light cool neutral.

### Cards

White / slightly elevated surfaces.

### Warning

Warm amber.

### Critical

Muted red.

### Success

Accessible green.

Do not rely solely on color to communicate meaning.

Use:

* icons
* labels
* typography
* status indicators

---

# TYPOGRAPHY

Use a clean modern Android typeface.

Suggested hierarchy:

Display:
Large dashboard title.

Headline:
Screen titles.

Title:
Card titles.

Body:
Readable notes.

Label:
Status and metadata.

Keep typography spacious and readable.

Healthcare applications should prioritize readability over visual density.

---

# ACCESSIBILITY

Implement:

* Large enough touch targets
* Good contrast
* Clear labels
* Content descriptions
* Accessible icons
* No color-only information
* Dynamic text support where practical
* Clear focus states

---

# APP INFORMATION ARCHITECTURE

The application should contain these primary areas:

1. Dashboard
2. Care Recipients
3. Daily Notes
4. AI Summary
5. Care Plan
6. Tasks
7. Settings

Use a modern bottom navigation bar.

Suggested navigation:

Dashboard
People
Notes
Care Plan
More

The exact navigation structure can be adjusted if UX testing suggests a better solution.

---

# CORE USER JOURNEY

The main demonstration flow must be extremely smooth:

OPEN APP

↓

Dashboard

↓

Select care recipient

↓

View recent notes

↓

Add daily note

↓

Save note

↓

Tap "Analyze Notes"

↓

AI processing animation

↓

AI Summary

↓

Review extracted observations

↓

View suggested care-plan draft

↓

Review/edit goals and interventions

↓

Approve draft

↓

Care Plan becomes active

↓

Tasks generated

↓

Dashboard reflects current plan and progress

This is the primary "wow" workflow.

---

# PHASE 0 — PROJECT FOUNDATION

Before implementing features:

1. Create Android project.
2. Configure Gradle.
3. Configure Kotlin.
4. Configure Compose.
5. Configure Material 3.
6. Configure Room.
7. Configure navigation.
8. Establish package structure.
9. Create theme.
10. Create reusable UI components.
11. Confirm the application builds.

Suggested package structure:

com.example.carebrief

```
core/
    model/
    database/
    navigation/
    ui/
    utils/

data/
    local/
    repository/
    ai/

domain/
    repository/
    usecase/

presentation/
    dashboard/
    recipients/
    notes/
    summary/
    careplan/
    tasks/
    settings/
```

Do not proceed with a broken build.

At the end of this phase:

BUILD THE APK.

Fix all compilation errors before continuing.

---

# PHASE 1 — DESIGN SYSTEM AND APP SHELL

Build the visual foundation.

Create:

* Color system
* Typography
* Spacing system
* Shapes
* Elevation
* Buttons
* Cards
* Chips
* Input fields
* Dialogs
* Bottom sheets
* Top app bars
* Empty states
* Error states
* Loading indicators

Create reusable components such as:

CareBriefCard
StatusChip
MetricCard
SectionHeader
PrimaryButton
SecondaryButton
CareRecipientAvatar
InsightCard
NoteCard
TaskCard
AIProcessingIndicator
EmptyState
ErrorState

Do NOT duplicate UI code unnecessarily.

---

# PHASE 2 — ONBOARDING

Create a short, beautiful onboarding flow.

Screen 1:

"Care documentation, made simpler."

Explain:

Capture daily observations and turn them into structured care-plan drafts.

Screen 2:

"Turn notes into useful insights."

Explain:

AI helps identify recurring observations, trends, and potential areas requiring attention.

Screen 3:

"Keep humans in control."

Explain:

Every AI suggestion is a draft that must be reviewed by a caregiver.

Final CTA:

"Get Started"

Do not make onboarding annoying.

Provide a way to skip it.

Store onboarding completion locally.

---

# PHASE 3 — DASHBOARD

Create the primary dashboard.

Header:

"Good morning"

or dynamically appropriate greeting.

Show:

* Current date
* Number of active care recipients
* Notes recorded today
* Pending tasks
* Care-plan review status

Example dashboard:

---

Good morning

Here's today's care overview.

[ 3 Active People ]

[ 5 Notes Today ]

[ 4 Pending Tasks ]

---

Today's Attention

[ Potential concern ]
Reduced appetite mentioned in 3 recent notes.
Review suggested.

---

Recent Activity

Sarah — Daily note added
Ahmed — Care plan updated
Mona — 2 tasks completed

---

Quick Actions

* Add Note

Analyze Notes

View Care Plan

---

The dashboard should feel calm and useful.

---

# PHASE 4 — CARE RECIPIENTS

Create a care-recipient list.

Each person should display:

* Name
* Age
* Care status
* Last note
* Active care-plan status
* Pending tasks

Example:

Sarah Johnson
72 years old

Care plan active

Last note:
Today, 09:30

2 pending tasks

Tap → profile.

---

# PHASE 5 — CARE RECIPIENT PROFILE

Build a detailed profile.

Sections:

### Overview

Name
Age
Care status
Care-plan status

### Recent observations

Timeline of notes.

### Current concerns

AI-generated potential areas requiring review.

### Care plan

Current goals.

### Tasks

Pending/completed tasks.

### Activity

Chronological history.

Include an obvious:

"Add Daily Note"

button.

---

# PHASE 6 — DAILY NOTE CREATION

This is one of the most important screens.

Create a beautiful note-entry interface.

Fields:

### Date / Time

Automatically populated.

### Observation

Large multiline text field.

Placeholder:

"Describe what you observed today..."

### Optional categories

Mood
Mobility
Nutrition
Sleep
Medication adherence
Behavior
Pain
Other

These should be selectable chips.

### Optional structured observations

Mood:

Good
Neutral
Low
Agitated

Mobility:

Independent
Assisted
Limited

Appetite:

Good
Reduced
Poor

Sleep:

Good
Interrupted
Poor

Do not make the form excessively long.

The free-text observation should remain the primary input.

CTA:

"Save Note"

After saving:

Show success confirmation.

Then provide:

"Analyze Recent Notes"

---

# PHASE 7 — DAILY NOTES TIMELINE

Create a timeline showing previous notes.

Each note should show:

* Date
* Time
* Author
* Observation
* Categories
* AI processing status

Example:

TODAY

09:30

"Sarah seemed more tired than usual during breakfast..."

Nutrition
Energy

---

YESTERDAY

18:10

"Skipped most of dinner and requested to go to bed early..."

Nutrition
Sleep

The timeline should visually communicate chronology.

---

# PHASE 8 — AI ANALYSIS EXPERIENCE

This is the centerpiece of the application.

When the user taps:

"Analyze Recent Notes"

show a premium AI processing state.

Do NOT use a fake progress bar that hangs.

Instead use a short deterministic staged animation:

Reviewing recent notes
↓
Identifying recurring observations
↓
Organizing key information
↓
Preparing care-plan draft

After processing:

Transition into the results.

---

# PHASE 9 — AI SUMMARY

Create an AI summary screen.

Top:

"AI Summary"

with a badge:

"DRAFT — REVIEW REQUIRED"

Show:

## Key observations

Example:

* Appetite appears lower across several recent notes.
* Increased fatigue was mentioned repeatedly.
* Sleep was reported as interrupted on multiple occasions.

## Emerging patterns

Example:

Nutrition
Mentioned 4 times

Energy
Mentioned 3 times

Sleep
Mentioned 2 times

## Potential areas to review

Use cautious wording:

"Reduced appetite may warrant additional observation."

"Repeated fatigue mentions may be useful to discuss with the appropriate care professional."

Never diagnose.

---

# PHASE 10 — INSIGHT CONFIDENCE / EVIDENCE

Make AI output explainable.

For every important insight, provide:

Observation
Evidence
Frequency

Example:

Potential concern

Reduced appetite

Evidence:
3 notes over the last 5 days mentioned reduced food intake.

This makes the AI feel more trustworthy.

Avoid fake scientific confidence percentages.

Do NOT say:

"92% confidence patient has X."

Instead say:

"Observed in 3 of 5 recent notes."

---

# PHASE 11 — CARE PLAN DRAFT

This is the second major centerpiece.

Create:

"Suggested Care Plan"

with:

DRAFT — HUMAN REVIEW REQUIRED

Sections:

## Goal

"Support consistent nutrition monitoring."

## Why this goal?

Based on repeated mentions of reduced appetite.

## Suggested actions

* Track meal intake daily.
* Record appetite observations.
* Note changes in energy or mood.
* Escalate significant changes according to existing care procedures.

## Monitoring

Track:

Nutrition
Energy
Mood

## Review date

Allow user to choose a date.

Every section must be editable.

---

# PHASE 12 — CARE PLAN EDITOR

The caregiver must be able to edit the AI draft.

Editable fields:

Goal
Reason
Actions
Monitoring indicators
Review date
Priority

Actions can be:

* Added
* Edited
* Deleted
* Reordered

Provide:

"Save Draft"

and

"Approve Care Plan"

buttons.

Before approval:

Show a confirmation:

"You're about to activate this care plan. Please confirm that the information has been reviewed."

---

# PHASE 13 — ACTIVE CARE PLAN

After approval, display:

ACTIVE CARE PLAN

Goal

Progress

Actions

Monitoring

Review date

History

Example:

Nutrition Monitoring

Active

Progress:
3 / 7 days documented

Actions:

✓ Record meal intake
✓ Observe appetite
○ Review weekly pattern

The UI should feel like a real workflow rather than a static document.

---

# PHASE 14 — TASK MANAGEMENT

Generate tasks from the care plan.

Example:

Today's tasks

□ Record breakfast intake

□ Record lunch intake

□ Record appetite observation

□ Review weekly nutrition pattern

Tasks should support:

* Complete
* Undo
* Due date
* Priority
* Category

Completed tasks should visibly change state.

---

# PHASE 15 — AI TASK GENERATION

The AI provider should be capable of transforming a care-plan draft into practical tasks.

Example:

Care-plan action:

"Monitor daily meal intake."

Generated task:

"Record today's meal intake"

Frequency:

Daily

The demo AI should generate deterministic results.

---

# PHASE 16 — SEARCH AND FILTER

Implement basic search.

Care recipients:

Search by name.

Notes:

Filter by:

* Date
* Category
* Recipient

Tasks:

Filter:

* Today
* Upcoming
* Completed

Keep filtering simple and intuitive.

---

# PHASE 17 — NOTIFICATIONS

If feasible within the APK:

Implement local Android notifications for:

* Pending task reminders
* Care-plan review reminders

Do NOT require a backend.

Allow notifications to be enabled/disabled in Settings.

---

# PHASE 18 — SETTINGS

Create a clean Settings screen.

Sections:

### Preferences

Notifications
Theme
Start screen

### AI

AI provider
Analysis behavior
Demo mode

### Data

Export data
Clear demo data

### About

CareBrief
Version
Privacy notice

Include:

"AI-generated content is provided as a draft and should be reviewed by an appropriate human professional."

---

# PHASE 19 — DEMO MODE

This is VERY IMPORTANT.

The application must contain a polished demo dataset.

Create several realistic care recipients.

Example:

Sarah Johnson
72

Michael Carter
68

Amina Hassan
75

Populate them with realistic notes spanning several days.

The demo data should allow the entire workflow to be demonstrated immediately.

The user should be able to:

Open app

→ Select Sarah

→ See historical notes

→ Analyze

→ Receive summary

→ View care-plan draft

→ Edit it

→ Approve it

→ Generate tasks

→ Complete tasks

without entering everything manually.

---

# PHASE 20 — DEMO SCENARIO

Create one especially strong demonstration scenario.

Example:

Care recipient:

Sarah Johnson

Recent notes:

Day 1:
"Sarah ate approximately half of her breakfast and said she wasn't very hungry."

Day 2:
"Sarah appeared more tired than usual and left part of lunch."

Day 3:
"Sarah ate very little at dinner and mentioned feeling fatigued."

Day 4:
"Sarah seemed quieter than usual and again had a reduced appetite."

The AI should identify:

Repeated reduced appetite observations.

Repeated fatigue observations.

Then produce a cautious suggested plan:

Goal:
Monitor nutrition and energy patterns.

Suggested actions:
Track meal intake.
Record appetite.
Monitor energy.
Review changes.

Potential concern:

"Repeated reduced appetite and fatigue were documented across several recent notes. Consider reviewing the pattern with the appropriate care professional if concerns persist."

Again:

NO DIAGNOSIS.

---

# PHASE 21 — ERROR HANDLING

Every important operation needs proper error handling.

Examples:

Database error

Show:

"Something went wrong while loading this information."

with:

Retry

AI analysis error

Show:

"We couldn't generate the analysis right now."

Options:

Retry

Continue reviewing notes

Empty notes

Show:

"No notes yet."

CTA:

"Add your first note"

No care recipients

Show:

"No care recipients yet."

CTA:

"Add care recipient"

---

# PHASE 22 — OFFLINE EXPERIENCE

The app should remain useful without internet.

Display an offline indicator when appropriate.

Core functionality should continue:

* View recipients
* Create notes
* View care plans
* Complete tasks
* View previous AI results

AI analysis in demo mode must continue working offline.

---

# PHASE 23 — ANIMATIONS

Use subtle, purposeful animations.

Examples:

* Screen transitions
* Card appearance
* Task completion
* AI analysis stages
* Bottom-sheet presentation
* Success confirmation

Avoid:

* Excessive bouncing
* Random animations
* Slow transitions
* Distracting effects

The app should feel premium, not like a game.

---

# PHASE 24 — MICROINTERACTIONS

Add polished details.

Examples:

When saving a note:

"Note saved"

with subtle confirmation.

When completing a task:

Check animation.

When approving a care plan:

"Care plan activated"

When AI analysis completes:

Summary cards smoothly appear.

When there are no tasks:

Show an elegant empty state.

These details matter.

---

# PHASE 25 — RESPONSIVE UI

The application must work on:

* Small Android phones
* Standard phones
* Large phones
* Emulator

Use Compose responsive layouts where appropriate.

Avoid hard-coded screen dimensions.

Handle:

* Keyboard
* Rotation if supported
* Long text
* Large font sizes
* Different screen widths

---

# PHASE 26 — DATA MODELS

Create sensible models.

Example conceptual models:

CareRecipient

id
name
age
avatar
status
createdAt

DailyNote

id
recipientId
timestamp
author
content
categories
mood
mobility
appetite
sleep

AiInsight

id
recipientId
title
description
evidence
frequency
severity
createdAt

CarePlan

id
recipientId
goal
reason
actions
monitoringIndicators
priority
status
reviewDate
createdAt
updatedAt

Task

id
carePlanId
recipientId
title
description
dueDate
priority
completed

Do not blindly copy these models if a better architecture is appropriate.

---

# PHASE 27 — DATABASE

Use Room.

Create:

Entities
DAO interfaces
Database
Repositories

Use Flow for observable lists where useful.

Make database operations asynchronous.

Do not block the UI thread.

---

# PHASE 28 — STATE MANAGEMENT

Each major screen should have explicit UI state.

Example:

Loading
Success
Empty
Error

Use StateFlow where appropriate.

Avoid massive ViewModels containing the entire application.

Keep responsibilities separated.

---

# PHASE 29 — TESTING

Create meaningful automated tests.

At minimum:

## Unit tests

Test:

* Note creation
* Note validation
* AI demo analysis
* Pattern extraction
* Care-plan generation
* Task generation

## Repository tests

Test local database operations.

## UI tests

Test at least:

* Launch app
* Navigate to recipient
* Create note
* Analyze notes
* View summary
* Approve care plan
* Complete task

Fix failing tests.

---

# PHASE 30 — VISUAL QA

Perform a full visual review.

Check:

* Alignment
* Padding
* Typography
* Overflow
* Icons
* Contrast
* Button sizing
* Navigation
* Keyboard behavior
* Long text
* Empty states
* Error states
* Loading states

The application should not look like generated boilerplate.

If a screen looks generic, redesign it.

---

# PHASE 31 — FINAL POLISH

Add the details that make the product feel premium:

* App icon
* Splash screen
* Consistent icons
* Smooth navigation
* Thoughtful empty states
* Good error messages
* Realistic demo data
* Proper spacing
* Consistent corner radii
* Consistent elevation
* Subtle animations
* Professional copywriting

Do a final UX pass from the perspective of a caregiver using the app for the first time.

Ask:

"Can I understand what this screen is for within 2 seconds?"

If not, improve it.

---

# PHASE 32 — SECURITY AND PRIVACY

Even though this is a prototype:

Do NOT:

* Hard-code API secrets
* Commit API keys
* Store secrets in Git
* Log sensitive user data unnecessarily

Do:

* Keep demo data clearly fictional.
* Include a privacy-oriented explanation.
* Make AI limitations clear.
* Separate AI suggestions from confirmed care information.

---

# PHASE 33 — README

Create a professional README.

Include:

# CareBrief

Short description.

## Problem

Caregivers often record information as unstructured daily notes.

## Solution

CareBrief transforms those notes into structured summaries and editable care-plan drafts.

## Features

List major features.

## Architecture

Explain architecture.

## Tech Stack

Kotlin
Jetpack Compose
Room
MVVM
etc.

## AI Architecture

Explain Demo AI provider and future remote AI provider.

## Screenshots

Add screenshots if possible.

## Running

Explain how to build.

## APK

Explain where the generated APK is located.

## Disclaimer

Explain that this is an assistive prototype and not a medical diagnostic system.

---

# PHASE 34 — APK GENERATION

THIS IS THE FINAL DELIVERABLE.

Build:

## Debug APK

and preferably:

## Release APK

The final output must be something like:

app/build/outputs/apk/debug/app-debug.apk

and/or

app/build/outputs/apk/release/app-release.apk

Verify the APK actually exists.

Verify it is a valid Android APK.

If possible, install it on an emulator and launch it.

---

# PHASE 35 — FINAL VALIDATION

Before declaring completion, perform this checklist.

## BUILD

[ ] Gradle build succeeds

[ ] No compilation errors

[ ] No unresolved dependencies

[ ] APK generated

[ ] APK file exists

## FUNCTIONALITY

[ ] App launches

[ ] Onboarding works

[ ] Dashboard works

[ ] Care recipients work

[ ] Recipient profile works

[ ] Notes work

[ ] Note creation works

[ ] AI analysis works

[ ] Summary works

[ ] Care-plan draft works

[ ] Care-plan editing works

[ ] Approval works

[ ] Tasks work

[ ] Task completion works

[ ] Settings work

## UX

[ ] Navigation is intuitive

[ ] Loading states exist

[ ] Empty states exist

[ ] Error states exist

[ ] No dead buttons

[ ] No broken screens

[ ] No text overflow

[ ] No ugly default components

## AI

[ ] AI output is clearly labeled

[ ] No diagnosis claims

[ ] No unsafe medical claims

[ ] Evidence is displayed

[ ] Human review is emphasized

## OFFLINE

[ ] Demo mode works without internet

[ ] Local data works

[ ] Existing AI results remain viewable

---

# DEVELOPMENT RULES

Follow these rules throughout the project.

## RULE 1

Do not stop after creating the skeleton.

The objective is a working APK.

## RULE 2

Do not leave TODO placeholders in core functionality.

## RULE 3

Do not create fake UI interactions.

If a button exists, it must perform an action.

## RULE 4

Do not use unnecessary external services.

The app must work locally.

## RULE 5

Prioritize the core demonstration workflow over obscure features.

## RULE 6

If a feature threatens the ability to build the APK reliably, simplify the feature rather than breaking the build.

## RULE 7

Use realistic data.

Never use:

"John Doe"
"Lorem ipsum"
"Test text"

unless technically necessary.

## RULE 8

The visual quality is a first-class requirement.

Do not treat UI as an afterthought.

## RULE 9

Every phase must end in a working state.

## RULE 10

After meaningful changes, build the project and fix errors immediately.

---

# IMPLEMENTATION STRATEGY

Work sequentially.

Do NOT attempt to write the entire application blindly in one enormous generation.

Use the following process:

PHASE

→ implement

→ compile

→ test

→ fix

→ continue

At the end of every major phase:

1. Run the build.
2. Fix compilation errors.
3. Run relevant tests.
4. Inspect the implementation.
5. Continue only when stable.

---

# PRIORITY ORDER

If time or complexity becomes a constraint, prioritize in this exact order:

1. APK builds
2. App launches
3. Dashboard
4. Care recipient
5. Daily notes
6. AI analysis
7. AI summary
8. Care-plan generation
9. Care-plan editing
10. Task generation
11. Task completion
12. Visual polish
13. Settings
14. Notifications
15. Additional features

Do NOT sacrifice the main workflow for secondary features.

---

# FINAL DEMONSTRATION SCRIPT

The finished application should support this demonstration:

1. Launch CareBrief.
2. Complete or skip onboarding.
3. Open Dashboard.
4. Select Sarah Johnson.
5. Review recent daily notes.
6. Add another observation.
7. Tap "Analyze Recent Notes."
8. Watch the AI analysis process.
9. View AI-generated summary.
10. Inspect evidence behind the insights.
11. Open suggested care plan.
12. Edit one action.
13. Approve the care plan.
14. View active care plan.
15. Generate/view tasks.
16. Complete a task.
17. Return to Dashboard.
18. Show updated progress.

This entire flow should feel coherent and polished.

---

# QUALITY BAR

The final application should feel like:

"An early-stage healthcare startup's polished MVP"

NOT:

"A university Android assignment."

The evaluator should immediately understand:

PROBLEM
↓
DAILY NOTES
↓
AI ANALYSIS
↓
STRUCTURED INSIGHTS
↓
CARE-PLAN DRAFT
↓
HUMAN REVIEW
↓
ACTIONABLE TASKS

That story must be visually and functionally obvious.

---

# FINAL RESPONSE FROM THE CODING AGENT

When development is complete, report:

1. What was implemented.
2. Technology stack.
3. Architecture.
4. Test results.
5. Build status.
6. Exact APK path.
7. How to install the APK.
8. Known limitations.
9. How to connect a real AI backend later.

Most importantly:

DO NOT CLAIM COMPLETION UNTIL THE APK HAS ACTUALLY BEEN BUILT SUCCESSFULLY.

The primary deliverable is the APK.
