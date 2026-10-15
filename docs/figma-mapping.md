# Figma → Compose mapping

Source: https://www.figma.com/design/6eZ3eU8v0N2y3YKt3oWqzn/Untitled

Current page and nodes were discovered through the Figma integration on 2026-10-15.
All screens are 412 × 915 design units. The full page metadata was inspected. Design
context and screenshots were retrieved for the six light screens and dark home;
dark presentation design context was also retrieved. The provider then returned
“You've reached the Figma MCP tool call limit on the Starter plan.”

| Screen | Light node | Dark node | Compose implementation |
| --- | --- | --- | --- |
| Home | 10:8695 | 10:9185 | HomeScreen, Header, CategoryGrid, PhraseCard, OfflineFooter |
| Search | 10:8799 | 10:9289 | SearchScreen, SearchInput, Filters |
| Presentation | 10:8870 | 10:9360 | PresentationScreen, PhraseCard(presentation=true) |
| Add card | 10:8904 | 10:9394 | EditScreen, PhraseInput, CategoryGrid(form=true) |
| Favorites | 10:8990 | 10:9480 | FavoritesScreen, Filters, PhraseCard |
| Categories/settings | 10:9058 | 10:9548 | SettingsScreen, CategoryEditor |

Reusable component node IDs were discovered: compact light/dark `10:10348` /
`10:10349`, presentation light/dark `10:10350` / `10:10351`. Their standalone
contexts could not be retrieved after the quota was reached. Card measurements
below come from actual screen instances (`10:8761`, `10:8885`, `10:9251`), which
use 148/126-unit compact heights and a 630-unit presentation height. The standalone
templates advertise 158/580-unit heights; the screen instances drive implementation.

## Tokens extracted from inspected contexts

| Token | Light | Dark |
| --- | --- | --- |
| Background | #F5F6F8 | #10151D |
| Surface | #FFFFFF | #1B222D |
| Secondary surface | #EDEFF3 | #262F3D |
| Text | #18212F | #F0F3F9 |
| Secondary text | #667184 | #A9B4C6 |
| Primary | #365FDC | #A4BBFF |
| Border | #DDE2E9 | #354050 |

Category light/dark colors and icon names were extracted from home contexts and
are bundled in `starter-library.json`, with separate editable dark colors.

- Latin type: Inter regular/medium/semibold/bold. Chinese: Noto Sans SC medium.
  Both are packaged locally; no font download occurs on the device.
- Screen gutters 20; presentation gutters 16. Content gaps 14 (home), 16 (search),
  and 18 (settings/presentation). Card corner radius 20; inputs 16.
- Header 27; section 17; secondary/context 12; navigation 12.
- Compact card Chinese 30 with 1.45 line height, pinyin 14, French 12.
- Presentation Chinese starts at 68, pinyin 23; Chinese fits available space and
  the content becomes scrollable if even the minimum size cannot contain it.
- Category tab width 30, rotated label 11, content inset 48 left / 16 right.
  Bottom-right icon 26 compact / 52 presentation, category-tinted border at 40%.
- Navigation: three destinations, 80-unit bar, 64 × 32 active indicator.
- Lucide icon names were identified directly in Figma. Bundled Android vectors
  implement those icons and match the category/control roles.

## Approved extensions and explicit differences

The user approved reuse of the search/list layouts for category browsing/history,
the add-card form for editing, and native dialogs/controls for category editing,
import, and rotation. These flows do not have dedicated approved frames.

- Example counts reflect 15 real cards, never the mockup's 150 cards or 6 favorites.
- History starts empty; nothing is represented as recently used before presentation.
- The written plan requires translations hidden by default even though the Figma
  presentation frame shows French underneath the card. The app exposes a reveal
  action, keeping Chinese/pinyin presentation and the surrounding geometry.
- Content scrolls on small devices and at large font scales, rather than clipping
  to the fixed design frame. Long category labels wrap; long tab labels ellipsize.
- Import, rotation, optional English/keyword editing, category editing, and empty
  states use native controls within the inspected visual theme.
- Additional dark screens use the extracted dark palette and corresponding light
  layouts. Their individual dark frames remain unverified due to the Figma quota.
- Android owns the real system bars; their glyphs, clock, insets, and gestures vary.
