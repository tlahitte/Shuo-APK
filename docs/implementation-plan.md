# CODEX PLAN — Offline Chinese Travel Cards Android App

## Objective

Build a native Android application for quickly finding and presenting Chinese travel phrases to local people.

**Figma is the authoritative UI/UX specification.** Do not invent alternative layouts, components, navigation patterns, or visual styles when they are defined in Figma.

Figma reference:
https://www.figma.com/design/6eZ3eU8v0N2y3YKt3oWqzn/Untitled

Before implementation, inspect the latest Figma screens and components using the Figma integration. Do not assume the previously shared node IDs are still valid.

## Technology Stack

- Kotlin
- Jetpack Compose + Material 3
- Room / SQLite for local persistence
- DataStore for preferences
- Navigation Compose
- MVVM with repositories and StateFlow
- Gradle Kotlin DSL
- Minimum Android version: API 26

No backend, account, analytics, cloud dependency, or Internet permission.

## Milestone 1 — Figma Analysis & Project Setup

1. Inspect every approved Figma screen, component, variant, and design token.
2. Identify the navigation structure, typography, colors, spacing, dimensions, icons, and interaction states.
3. Create a mapping between Figma components and reusable Jetpack Compose components.
4. Initialize the Android project and configure dependencies.
5. Establish a maintainable package structure separating UI, domain, data, and navigation.

**Deliverable:** Compilable Android project, design token definitions, and a Figma-to-Compose component mapping.

**Gate:** Do not proceed if essential Figma screens are missing or ambiguous. Document gaps and request clarification rather than inventing a replacement design.

## Milestone 2 — Offline Phrase Database

Create a Room database supporting:

- Phrase ID
- Simplified Chinese text
- Tone-marked pinyin
- French translation
- Optional English translation
- Search keywords
- Category ID
- Favorite status
- Created/modified timestamps
- Recently used timestamp
- Built-in versus user-created status

Create a separate category entity with customizable names, colors, icons, and ordering.

Import the approved starter library of 150 phrases from bundled JSON assets.

Requirements:
- Seed data only once.
- Preserve user modifications across upgrades.
- Allow creating, editing, deleting, and categorizing phrases.
- Support user-defined categories.
- Implement JSON import/export with validation.
- All operations must work offline.

**Deliverable:** Functional database, repositories, seed import, and persistence tests.

## Milestone 3 — Search Engine

Implement responsive local search across French, Chinese, pinyin, and keywords.

Requirements:
- Accent-insensitive French matching.
- Tone-insensitive pinyin matching.
- Case-insensitive matching.
- Support partial words and phrases.
- Category filtering.
- Favor exact and prefix matches over weaker matches.
- Search results update while typing.

Use SQLite FTS where appropriate, supplemented with normalized searchable fields.

**Deliverable:** Tested offline search supporting all 150 starter phrases and user-created cards.

## Milestone 4 — Figma-Accurate UI Implementation

Implement the approved screens using reusable Compose components.

Screens:
1. Home
2. Search and results
3. Category browsing
4. Full-screen phrase presentation
5. Add/edit phrase
6. Favorites and recently used
7. Settings and category management

### Phrase Card — Critical Component

Follow the approved Figma component precisely.

Preserve:
- Large centered Chinese typography.
- Pinyin beneath the Chinese text.
- Prominent category-colored vertical tab on the left.
- Category name rotated vertically.
- Category icon at the bottom-right.
- Category-colored border and accents.
- Neutral readable content surface.

Never position the category label above the Chinese phrase.

In presentation mode, hide French and English translations by default.

Support dynamic text sizing and multiline Chinese text without clipping.

Implement both light and dark themes based on Figma variants.

**Deliverable:** All approved screens implemented and navigable with real database content.

## Milestone 5 — Interactions & UX

Implement the interactions defined in Figma, including:

- Tap a phrase to open full-screen presentation.
- Search directly from the home screen.
- Add and edit phrases.
- Toggle favorites.
- Browse and filter by category.
- Access recently displayed phrases.
- Create and customize categories.
- Switch appearance modes.
- Import and export the phrase library.
- Rotate presented text by 180°.
- Keep the screen awake while presenting a phrase.

Use Android-native system integrations where appropriate, without introducing unnecessary permissions.

**Deliverable:** Complete end-to-end user workflows.

## Milestone 6 — Testing & Visual Verification

### Automated Tests

Implement unit and instrumentation tests covering:

- Database initialization and migrations.
- Seed import without duplicates.
- Phrase CRUD.
- Category CRUD.
- Search normalization and ranking.
- Favorites and recent history.
- JSON import/export and malformed-file handling.
- Persistence after process restart.
- Navigation and major user interactions.

### Figma Visual Validation

For each implemented screen:

1. Capture an Android emulator screenshot.
2. Compare against the corresponding approved Figma frame at equivalent dimensions and theme.
3. Identify discrepancies in spacing, colors, typography, alignment, icons, and proportions.
4. Correct the implementation.
5. Repeat until the screen closely matches the reference.

Test long Chinese phrases, small displays, font scaling, light/dark themes, and offline operation.

Do not consider a screen complete solely because it compiles.

**Deliverable:** Passing tests, visual comparison evidence, and a documented list of remaining differences.

## Milestone 7 — Release

- Generate a signed release APK or AAB.
- Verify installation and first launch on a clean Android device.
- Verify offline operation in airplane mode.
- Confirm the initial phrase library is available immediately.
- Confirm user-created data survives application restarts.
- Provide a README with build, installation, testing, and release instructions.

**Final deliverables:** Complete Android Studio source project, installable release build, automated tests, and visual validation screenshots.

## Codex Execution Rules

1. Treat Figma as the UI/UX source of truth.
2. Do not redesign approved screens.
3. Use native Compose components, not screenshots or flattened Figma exports as UI.
4. Keep the application completely offline.
5. Never overwrite user-created or modified phrases during database updates.
6. Implement real functionality, not static mockup screens.
7. Build and test after each milestone.
8. Report completed work, test results, blockers, and deviations.
9. Stop and ask for clarification when a required design or behavior cannot be determined.
10. Do not claim a feature is complete without verifying it.

**Definition of Done:** The application installs on Android, operates entirely offline, provides the complete phrase library and editing functionality, passes its tests, and visually matches the approved Figma designs.