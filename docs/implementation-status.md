# Original implementation status · 0.1.0

This document records the first release. See [0.2.0 release notes](releases/0.2.0.md)
for the 310-card library, four languages, performance changes, icon and GitHub updater.
The owner made the repository public for direct APK distribution in 0.2.0.

Implemented locally on 2026-10-15 in the initially empty Shuo-APK repository.
The original specification is preserved in [implementation-plan.md](implementation-plan.md).

## Accepted scope changes

- The owner supplied Figma access after the initial access failure.
- The owner requested a small example library until the UI is ready, replacing
  the unavailable approved 150-phrase library for this iteration.
- The repository was initially local. At the owner’s request, source and signed
  APK are published in the private GitHub repository
  [tlahitte/Shuo-APK](https://github.com/tlahitte/Shuo-APK), release `v0.1.0`.
- The owner approved reuse of existing layouts and native controls for category
  browsing, history, editing, category customization, import, and rotation.

## Implemented

- Android Studio source project, Gradle Kotlin DSL/wrapper, API 26 minimum,
  Compose/Material 3, Navigation Compose, Room, DataStore, and StateFlow ViewModel.
- Fifteen bundled example phrases in ten categories, with three initial favorites.
  Phrase/category CRUD, ordering, favorites, and actual recent history persist.
- Transactional one-time seed marker, stable IDs, foreign-key protection, atomic
  validated JSON import, preservation of local ID conflicts, and JSON export.
- Accent/tone/case-insensitive partial search across French, Chinese, pinyin,
  English, keywords, and category names, with exact/prefix ranking and filters.
- Home, search, favorites, settings/categories, add/edit, presentation, and history.
  Cards have a vertical left category tab, centered Chinese/pinyin, category border,
  and bottom-right icon. Presentation hides translations until requested.
- Light/dark/system appearance, bundled fonts/icons, dynamic text sizing, scrolling
  for long text/small displays, 180-degree rotation, and presentation-only screen wake.
- Native file picker import/export, category dialogs, deletion confirmation,
  and unsaved-form protection for back navigation and tab changes.
- Optimized development-key signed preview and dedicated release-key signed
  0.1.0 APK, with documented repeatable signing configuration.
  No Internet permission, analytics, accounts, or runtime permission requests.
- Build/install/test/release instructions, Room schema, design mapping, license
  notices, test reports, emulator screenshots, and recorded visual limitations.

## Verification

- 22 local tests: normalization/ranking, every sample in three languages, codec
  validation, duplicates, invalid references/colors/timestamps, size limits,
  seed/import, CRUD, favorites/history, restart persistence, and refusal of an
  unsupported schema change without deleting existing data.
- 5 Android 15 emulator tests: search/presentation/favorites/history/rotation,
  phrase creation/editing/deletion, activity recreation, category editing/deletion,
  theme persistence, unsaved-form guard, both-theme captures, and long Chinese text.
- Additional tests/captures at 360 x 640 dp with 150% system font size. Cards grow
  and the page scrolls when text at the minimum readable size cannot fit.
- Android lint verifies API-26 compatibility. Remaining advisories are dependency
  update notices for the pinned versions; there are no lint errors.
- Preview signature/install, airplane-mode operation, and file-picker import/export
  checks are recorded in `docs/verification/release-smoke.txt`.
- Dedicated release APK build and lint passed (zero errors, 13 dependency-update
  warnings). Signature, package/version, and disabled debugging were verified.
  Clean offline launch, search/presentation, favorites, translation visibility,
  rotation, process-restart persistence, and reinstall persistence passed on the
  Android 15 emulator. See `docs/releases/0.1.0.md` and
  `docs/verification/signed-release-smoke.txt`.

XML reports and PNG evidence are under `docs/verification/`. Normal captures use
1080 x 2400 pixels at 420 dpi, approximately the Figma frame's 412 x 915 dp.

## Remaining limits and release work

- The examples are not the approved 150-phrase library. The owner will develop and
  review the complete library, including travel-critical wording.
- Figma returned a Starter-plan quota error after the main light screens and dark
  home/presentation contexts were captured. Standalone card contexts and individual
  dark search/add/favorites/settings contexts remain unverified. The extracted dark
  palette is applied consistently, without claiming full visual parity.
- Screenshot differences include actual content/counts, system bars/insets,
  accessible wrapping, approved additional controls, and hidden translations.
  Presentation centering, typography, and icon strokes were corrected after review.
- Database version 1 is the first schema. No released legacy database exists to
  migrate. Future versions require explicit migrations and migration tests.
- Release 0.1.0 is signed with a dedicated private key kept outside the repository.
  APK and checksum are available in GitHub release `v0.1.0` and ignored `dist/`.
  Store publication and physical-device verification have not been performed.

The original seven-milestone definition of done is not claimed in full. The agreed
example-library app/UI iteration is implemented; full library approval, remaining
Figma verification, physical-device checks, and store publication remain follow-up work.
