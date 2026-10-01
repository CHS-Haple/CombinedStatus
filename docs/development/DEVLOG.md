## 2026-10-02 — Build 590 complete MIUIX action-pill Apply control

**Type:** App UI / MIUIX pill conformance  
**Display version:** 0.0.3  
**Build:** 590 / `20261002-590`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction
Use the complete MIUIX pill specification for the Apply action, including shape, width behavior and internal metrics.

### Pinned MIUIX source
The Snackbar action pill uses:
- `TextButton`;
- `minWidth = 26.dp`;
- `minHeight = 26.dp`;
- `SnackbarDefaults.ActionCornerRadius`;
- `SnackbarDefaults.ActionInsideMargin` = 12dp horizontal / 0dp vertical;
- `TextStyle(fontSize = 15.sp)`;
- primary semantic action colors.

Its width is content-driven, not fixed.

### Change
- Replace the 120dp-width scheme Apply Button with the exact pill geometry above.
- Remove the project-owned width and generic Button height.
- Keep Apply/Applied enabled state semantics and native MIUIX interaction.

### Review
No custom pill width/height/radius remains. No other layout, hierarchy, Runtime/SystemUI, hook, or persistence change.

### Validation
Run exact-head Runtime CI and Canary; verify the Apply/Applied action reads as a true MIUIX capsule and sizes naturally to its label.

## 2026-10-02 — Build 589 true MIUIX pill Apply button

**Type:** App UI / MIUIX button geometry  
**Display version:** 0.0.3  
**Build:** 589 / `20261002-589`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer correction
The Apply action must be a true capsule/pill, not merely a more-rounded rectangle.

### Root cause
Build 588 used `ButtonDefaults.MinHeight * 0.5f` as the radius. `MinHeight` is only the component minimum; native Button content padding can make the actual rendered height larger, so a 20dp radius does not guarantee a pill silhouette.

### Pinned MIUIX precedent
Pinned MIUIX Snackbar action pills use an explicit 50dp action corner radius. The squircle renderer also supports capsule/pill degradation at large corner radii.

### Change
- Keep native MIUIX `Button`.
- Keep primary semantic colors, native text style and interaction.
- Keep 120dp minimum width.
- Set `cornerRadius = 50.dp` so the control remains visually capsule-shaped regardless of its final measured height.

### Validation
Run exact-head Runtime CI and Canary. Device acceptance: Apply/Applied must read as a genuine pill.

## 2026-10-02 — Build 588 secondary gray-Sheet hierarchy and locked two-level height

**Type:** App UI / MIUIX hierarchy / sheet geometry  
**Display version:** 0.0.3  
**Build:** 588 / `20261002-588`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction
- Keep the overview as a white Sheet with gray scheme Cards.
- Make the secondary editor Sheet itself gray.
- Remove the secondary gray outer Card.
- Put Color source in a white Card; keep Common colors / Full adjustment / Precise input in white Cards.
- Preserve the original secondary spacing and component sizing.
- Level 1 and level 2 must always have exactly the same Sheet height.

### Change
- Secondary `OverlayBottomSheet.backgroundColor` uses `MiuixTheme.colorScheme.surface`; overview uses `background`.
- Remove the detail page's redundant gray outer Card.
- Wrap `OverlayDropdownPreference` for Color source in a native default Card.
- Restore original detail spacing: 24dp bottom scroll padding, 16dp function-card padding, 12dp common-color row gap, native BasicComponent spacing.
- Introduce one shared `BATTERY_COLOR_SHEET_HEIGHT_FRACTION = 0.84f`; the single shared `OverlayBottomSheet` owns this height for both Pager pages.

### Review
- Level 1/2 height ownership is singular; page content has no independent sheet-height modifier.
- Only background/title/start action vary between levels.
- MIUIX semantic hierarchy: gray `surface` page layer + white `surfaceContainer` functional Cards.
- No Runtime/SystemUI/hook/persistence changes.

### Validation
Run exact-head Runtime CI and Canary. Device acceptance should verify identical Sheet top edge/height while switching levels, gray detail Sheet, white source/function Cards, and unchanged internal spacing.

## 2026-10-02 — Build 587 battery-color sheet geometry and density pass

**Type:** App UI / MIUIX layout / pager geometry  
**Display version:** 0.0.3  
**Build:** 587 / `20261002-587`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device evidence
Build 586 showed:
- excessive gap between the sheet title and fixed scheme navigator;
- visually oversized previous/next controls;
- ordinary and Add scheme pages starting at different vertical positions;
- Add-page preview strip not optically centered;
- adjacent gray scheme Cards touching during horizontal transitions;
- mode-detail layout too tall and visually loose.

### Root causes
- Both the two-level navigation Pager and the scheme Pager inherited Compose Pager's centered vertical alignment.
- Preview strips were content-width Rows placed in a start-aligned Column.
- The visible navigation surface occupied the full MIUIX IconButton touch target.
- Scheme pages had zero page spacing.
- Overview/detail sheet height still depended on natural page content.
- Secondary editor used generous default grouping rhythm on top of the gray outer Card.

### Change
- Apply one 84%-window-height modifier to the shared OverlayBottomSheet so both levels use the same taller sheet.
- Top-align both Pagers.
- Scheme Pager page spacing: 12dp.
- Center ordinary/Add preview strips explicitly in full-width Boxes.
- Preserve native MIUIX 40dp IconButton interaction geometry but render a 32dp semantic surface and 18dp chevron; navigator-to-indicator spacing is 12dp.
- Compact secondary editor: gray/function Card vertical padding 12dp, common-row gap 8dp, HSV row vertical padding 10dp, bottom scroll padding 16dp.

### Review
- Sheet title remains outside the gray Card.
- Fixed scheme navigator remains outside the horizontally moving gray Card.
- White setting/function Cards remain nested inside the gray outer Card.
- No literal color values, custom font overrides, Runtime/SystemUI, hook, or persistence changes.

### Validation
Run exact-head Runtime CI and Canary. Device acceptance should focus on title/navigator spacing, arrow optical size, identical Add/ordinary Card top baseline, centered preview strip, page gap during swipe, shared sheet height, and secondary editor density.

## 2026-10-02 — Build 586 compile-only correction

**Type:** App UI compile fix  
**Display version:** 0.0.3  
**Build:** 586 / `20261002-586`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence
Build 585 Runtime CI #2130 failed in Kotlin compilation at the two new title-row `Modifier.heightIn` calls.

### Root cause
`androidx.compose.foundation.layout.heightIn` was not imported.

### Change
Add the missing import only. No geometry, typography, color token, pager, Runtime/SystemUI, hook, or persistence behavior changed.

### Validation
Re-run exact-head Runtime CI. Canary remains blocked until green.

## 2026-10-02 — Build 585 MIUIX geometry/token audit

**Type:** App UI / MIUIX conformance  
**Display version:** 0.0.3  
**Build:** 585 / `20261002-585`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Scope
Audit Build 584's revised hierarchy against pinned MIUIX `0.9.4-5c91d5e5-SNAPSHOT`.

### Kept upstream-owned
- OverlayBottomSheet default white background and 24dp horizontal inside margin.
- Card default 16dp corner radius.
- White nested setting Cards use default `surfaceContainer/onSurfaceContainer`.
- ArrowPreference / OverlayDropdownPreference / SmallTitle / BasicComponent typography and component spacing.
- IconButton default 40dp minimum size / 40dp corner radius.

### Project-owned only where upstream has no exact component
- Scheme preview swatch geometry.
- Pager indicator geometry because pinned MIUIX has no PagerIndicator component.
- Apply-button width for the HyperOS-style primary action; the control itself remains native MIUIX Button.

### Changes
- Apply Button keeps `ButtonDefaults.MinHeight` and derives pill radius as half that native height; no custom drawing.
- Inactive pager indicator uses `disabledOnSecondaryVariant` rather than a literal alpha from `onSurface`.
- No custom font size/weight/color overrides were added for gray-Card content.

### Validation
Run exact-head Runtime CI and Canary; verify gray outer Card/white inner Card hierarchy, fixed navigator, button geometry, and disabled/active visual states.

## 2026-10-02 — Build 584 fixed scheme navigator ownership

**Type:** App UI / pager ownership / MIUIX navigation  
**Display version:** 0.0.3  
**Build:** 584 / `20261002-584`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer correction
Horizontal switching should move the complete gray scheme Card only. The previous/next buttons and page indicator must stay fixed above that Card.

### Change
- Hoist `BatterySchemeNavigator` out of individual scheme/Add pages into `BatterySchemeOverview`.
- Keep navigator geometry fixed above the `HorizontalPager`.
- Pager pages now own only the gray outer Card and its nested content.
- Remove `pageCount/pageIndex/onNavigateTo` from scheme and Add page APIs.
- Preserve native MIUIX IconButtons, disabled first/last states, pager spring and Build-583 nested Card hierarchy.

### Review
- Navigation state has one owner: `BatterySchemeOverview`.
- Page content no longer owns or animates navigation controls.
- Only the gray Card participates in horizontal page motion.
- No Runtime/SystemUI/hook/persistence delta.

### Validation
Run exact-head Runtime CI and Canary. Verify navigator remains visually stationary while the gray Card slides, including first/last-page disabled states.

## 2026-10-02 — Build 583 nested battery-color card hierarchy

**Type:** App UI / MIUIX hierarchy / layout ownership  
**Display version:** 0.0.3  
**Build:** 583 / `20261002-583`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer correction
The intended visual hierarchy is not a gray Sheet. It is:
1. white BottomSheet;
2. one light-gray outer Card containing all battery-color page content;
3. white inner Card(s) containing actual settings/function groups.

### Implementation
- Restore native white `OverlayBottomSheet` background.
- Ordinary scheme pages: one `surface` outer Card owns title, preview, pager navigation, apply action and the nested white settings Card.
- Custom title and More menu share the same minimum 40dp title row; the title remains centered and More is aligned `CenterEnd`.
- Add page uses the same outer Card geometry and title/preview/navigation baselines as ordinary scheme pages; its plus action remains centered in a nested white Card.
- Detail editor: one `surface` outer Card owns the bare source dropdown and section labels; Common colors, Full adjustment and Precise input remain native white nested Cards.
- All colors use MIUIX semantic tokens; no hand-drawn borders or literal RGB values.

### Review
- Card hierarchy matches ownership: page container vs setting/function-group container.
- Typography remains pinned MIUIX `title2`, `SmallTitle`, `BasicComponent`, `body2` and native Button/Preference styles.
- No Runtime/SystemUI/hook/persistence change.
- Build 582 visual hierarchy is superseded and should not be used for device acceptance.

### Validation
Run exact-head Runtime CI and Canary. Validate white Sheet, visible gray outer Card, nested white settings Cards, aligned custom More button, and Add-page geometry parity.

## 2026-10-02 — Build 582 BottomSheet/Card semantic color hierarchy correction

**Type:** App UI / MIUIX semantic color hierarchy  
**Display version:** 0.0.3  
**Build:** 582 / `20261002-582`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer correction
The settings hierarchy should be a light-gray Sheet with white Cards, not a white Sheet with gray Cards.

### Pinned MIUIX evidence
Revision: `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.
- light `surface = #F7F7F7`;
- light `background = #FFFFFF`;
- light `surfaceContainer = #FFFFFF`;
- dark `surface = #000000`;
- dark `surfaceContainer = #242424`.
`OverlayBottomSheet` defaults to `background`, while `Card` defaults to `surfaceContainer`.

### Change
- Set the battery-color `OverlayBottomSheet.backgroundColor` to `MiuixTheme.colorScheme.surface`.
- Return all function-group Cards to native default `CardDefaults.defaultColors()` / `surfaceContainer`.
- Keep the top source dropdown bare on the Sheet surface.

### Review
- Semantic tokens only; no literal colors.
- Correct light and dark hierarchy.
- No custom borders, typography, Runtime/SystemUI, hook, persistence, or color-policy changes.

### Validation
Run exact-head Runtime CI and Canary. Verify the Sheet is visibly light gray in light mode, Cards are white and clearly bounded, and dark mode retains black Sheet / dark-gray Card separation.

## 2026-10-02 — Build 581 scheme navigation affordance and visible function Cards

**Type:** App UI / MIUIX controls / visual hierarchy  
**Display version:** 0.0.3  
**Build:** 581 / `20261002-581`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device evidence
Build 580 confirmed two visual problems:
- scheme arrows at the pager indicator read like plain glyphs rather than buttons;
- mode-detail function-group Cards were effectively invisible against the white BottomSheet.

### Root cause
- Pinned MIUIX `IconButton` defaults to a 40dp control with 40dp corner radius but `Color.Unspecified` background. The prior chevrons therefore had correct touch semantics but weak visible affordance.
- `OverlayBottomSheet` uses `MiuixTheme.colorScheme.background` while `Card` defaults to `surfaceContainer`; in the current light palette both resolve to white, collapsing the visual boundary.

### Change
- Keep native `IconButton` geometry and click behavior.
- Use MIUIX semantic container tokens only: `secondaryVariant/onSecondaryVariant` when enabled and `disabledSecondaryVariant/disabledOnSecondaryVariant` when unavailable.
- Keep the first/last page button slots present and disabled so the center indicator does not move.
- Add 24dp separation between each 40dp button and the page indicator for optical clarity.
- Common colors / Full adjustment / Precise input Cards use `CardDefaults.defaultColors(color = surface, contentColor = onSurface)`, producing the intended visible light-gray rounded rectangle against BottomSheet `background` without hand-drawn borders.

### Review
- No custom icon drawing or text-arrow fallback.
- No manual button size/shape override; upstream 40dp / 40dp-radius defaults remain authoritative.
- No pager timing change; pinned `PagerNavigationSpringSpec` remains intact.
- No runtime, hook, persistence, or color-policy delta.

### Validation
Run exact-head Runtime CI and Canary. Verify button affordance/disabled edge pages, stable center alignment, and visible function-group Cards in the mode detail page.

## 2026-10-02 — Build 580 mode-detail MIUIX conformance refinement

**Type:** App UI hierarchy / color editor / MIUIX conformance  
**Display version:** 0.0.3  
**Build:** 580 / `20261002-580`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction
- Source dropdown stands alone: no duplicate gray title and no wrapping Card.
- One actual function group maps to one Card.
- Remove redundant Current color and separate Restore-this-mode entry.
- Common colors need larger optical presence, long-press details, and selection that does not shrink the chosen color.
- HSV must expose live values.
- Follow inversion should look inactive but remain directly editable, promoting to Custom only on a valid edit.
- Prefer pinned MIUIX typography, spacing, tokens and controls before project tuning.

### Pinned MIUIX audit
Revision: `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.

Verified directly:
- `SmallTitle`: subtitle style, 28dp horizontal / 8dp vertical default inset.
- `BasicComponent`: native title/body2 typography, 16dp inset, disabled title/summary tokens.
- `TextField`: native text style, 16dp corners and 16dp internal margins.
- `TooltipBox`: native touch long-press tooltip; already used by Guiyuan Hot Reload.
- `ColorPalette`: selected color remains full-size; a white ring/glow is overlaid instead of shrinking the color body.
- MIUIX has no discrete common-color swatch component, so the visible swatch diameter remains an explicit project optical parameter rather than being presented as an upstream default.

### Changes
- Bare `OverlayDropdownPreference` at the top owns source + effective value.
- Common colors / Full adjustment / Precise input each use their own Card.
- Remove duplicate Current color and Management/Restore group.
- Restore SmallTitle default inset.
- Common colors use 40dp interaction cells, 28dp visible bodies, 16dp Card inset, 12dp row spacing, outer-only selection ring, and `TooltipBox(text = "#RRGGBB")`.
- HSV rows show `degree`, `saturation %`, and `brightness %` values using body2/action-color semantics.
- Follow inversion uses MIUIX disabled visual tokens while preserving interaction. A valid common-color/HEX/RGB/HSV edit continues through `setCustomColor`, which is the single copy-on-write writer for promotion to Custom.

### Review
- MIUIX-first hierarchy/typography/spacing confirmed.
- No second editor state writer.
- No Runtime/SystemUI/hook/persistence-schema change.
- No fake color is created for Follow inversion with no remembered seed.
- The only project-owned optical exception is the discrete swatch geometry because upstream exposes no equivalent component.

### Validation
Run exact-head Runtime CI only. Do not trigger Canary until explicitly requested.

## 2026-10-02 — Build 578 scheme-page vertical overflow correction

**Type:** App UI layout / MIUIX settings-page scrolling  
**Display version:** 0.0.3  
**Build:** 578 / `20261002-578`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device evidence

Build-577 Canary #630 / run `36922895008` passed exact-head signing/runtime validation. Device screenshot then showed:
- HyperOS Charging row rendered its green preview but no HEX summary;
- the following Low-battery row was absent.

### Root cause

The palette data is correct: HyperOS Charging resolves to `#1DCD3A` and Low resolves to `#FA382E`.

The defect is layout ownership. The scheme overview uses a fixed 455dp inner Pager, while the centered header plus six MIUIX setting rows can exceed that viewport on the real device. Build 577's clipping boundary therefore cut the Charging row between title and summary, then placed Low entirely below the viewport.

This is one defect, not two color-state failures.

### Change

- Make `BatterySchemePageContent` vertically scrollable inside its existing Pager viewport.
- Keep the outer BottomSheet at 520dp and the inner Pager at 455dp.
- Keep MIUIX BasicComponent / ArrowPreference row typography, minimum height, spacing, and interaction unchanged.
- Keep the Pager indicator outside the scrolling page so it remains fixed.
- Do not compress rows, shrink text, enlarge the drawer, or special-case Charging/Low geometry.

### 审查 / review

- root cause is bounded to the scheme-page viewport;
- no color source/palette/persistence change;
- no Runtime/SystemUI/hook change;
- no second sheet, nested vertical owner, timer, or geometry compensation;
- vertical scrolling is page-local and coexists with the non-user-scrollable horizontal Pager.

### Validation

Run exact-head Runtime CI. If green, generate signed Work Branch Canary and verify that Charging shows `#1DCD3A`, Low battery is fully reachable, the Pager indicator stays fixed, and no header/content bleed returns.

## 2026-10-01 — Build 577 mode-detail MIUIX dropdown redesign

**Type:** App UI hierarchy / MIUIX preference semantics / visual consistency  
**Display version:** 0.0.3  
**Build:** 577 / `20261001-577`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction

The mode-detail page should behave as a real settings page inside the existing battery-color drawer. The maintainer explicitly requested:
- smooth level-1 -> level-2 drawer navigation without stacking two independent drawers;
- MIUIX components wherever they exist, with native/HyperOS-style emulation only when MIUIX lacks an equivalent;
- Color source to use a dropdown rather than expanding a full option list inline;
- color chips may keep outlines for background separation, but the outline must be visually consistent at every chip size and on every preceding page;
- submit and run CI only; do not trigger Canary until explicitly instructed.

### Exact MIUIX basis

Pinned revision: `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.

Verified directly:
- `OverlayDropdownPreference` is built on `BasicComponent` and owns:
  - standard title / summary typography and 56dp setting-row geometry;
  - selected-value display;
  - `DropdownArrowEndAction`;
  - pressed/hold-down state;
  - ContextClick haptic on open;
  - Confirm haptic on selection;
  - native `OverlayListPopup` rendering and dismissal.
- The existing internal Pager already uses MIUIX `springAnimateToPage` for programmatic level transitions, so a second nested `OverlayBottomSheet` is unnecessary and would create competing sheet geometry/overlay ownership.

### Changes

#### Single-sheet navigation

- Keep one outer `OverlayBottomSheet` only.
- Level-1 scheme page and level-2 mode detail remain two pages of the internal non-user-scrollable Pager.
- Add `clipToBounds()` to the fixed 520dp Pager viewport so vertically scrolled detail content cannot paint into the sheet title/header area.
- Back/dismiss from detail uses the same MIUIX spring page transition to return to level 1.

#### Color source

- Remove project-owned `sourceExpanded` state.
- Remove the inline expandable `RadioButtonPreference` list and the manually selected ExpandMore/ExpandLess icons.
- Replace them with one native `OverlayDropdownPreference`.
- Source order remains:
  1. HyperOS
  2. iOS
  3. Low saturation
  4. Follow inversion
  5. Custom
- The setting row shows:
  - title = Color source;
  - summary = currently effective HEX / Follow inversion / Not set;
  - start action = current fixed/checker preview;
  - MIUIX-owned selected value + dropdown affordance on the end side.

#### No fake fixed color for Follow inversion

- Remove the old fallback to `MiuixTheme.colorScheme.onSurface`, which caused Follow inversion to silently appear as black in HSV/HEX controls.
- Add a nullable editor seed:
  - active fixed-template color wins;
  - otherwise remembered custom color may be reused as an editing seed;
  - otherwise the editor has no fixed seed.
- With no fixed seed:
  - current output stays Follow inversion / Not set rather than `#000000`;
  - HSV controls are replaced by a normal MIUIX explanatory `BasicComponent`;
  - common colors and HEX/RGB remain available, so the first valid user edit still performs copy-on-write into Custom.
- Add unit coverage for Follow inversion with and without remembered custom color.

#### Unified swatch outline

All fixed/checker preview circles now use one shared outline rule rather than page-specific border/no-border decisions:
- stroke width = chip diameter / 24;
- stroke color = `onSurface` at alpha 0.12.

Examples:
- 12dp compact Function preview -> ~0.5dp stroke;
- 20dp editor/common chip -> ~0.83dp;
- 24dp mode setting chip -> 1dp;
- 28dp scheme-header chip -> ~1.17dp.

The same rule is used for fixed-color `Surface`, checkerboard `Surface`, and common-color disks. This intentionally supersedes Build 574's drawer-header/mode-row borderless exception.

### 审查 / review — pre-commit

- **MIUIX-first:** source selection is the exact pinned `OverlayDropdownPreference`; no custom dropdown implementation.
- **drawer ownership:** still one `OverlayBottomSheet`; no nested sheet stack.
- **transition:** existing MIUIX Pager spring remains the sole level transition.
- **scroll boundary:** detail viewport is clipped rather than padded around the bleed symptom.
- **color semantics:** Follow inversion remains dynamic/no-fixed-color and is never represented as fake black.
- **copy-on-write:** common color, HSV, HEX and RGB edits still call the existing custom-color setter; template references remain references until edit.
- **visual consistency:** all preview chips share one proportional outline rule.
- **self drawing:** no project Canvas/drawCircle/drawRect/drawWithCache/raw clickable introduced; checkerboard remains MIUIX public `drawCheckerboard()`.
- **Runtime / persistence:** no hook, writer, schema, projection or color-policy change.
- **Canary:** explicitly prohibited until maintainer instruction.

### Validation

Runtime CI #2108 / run `36921379961` failed during Kotlin compilation before tests because the mode-detail redesign referenced three Android string resources that did not exist:
- `battery_color_unset`;
- `battery_color_no_fixed_color`;
- `battery_color_no_fixed_color_summary`.

Root cause was resource-contract incompleteness in the UI-only refactor, not MIUIX API incompatibility or Runtime behavior. Review found an existing canonical `battery_color_custom_unset` string already expresses the first state, so the correction reuses it rather than adding a duplicate. The two no-fixed-color explanatory strings are added in English and Simplified Chinese.

Runtime CI #2109 / run `36921996822` is green on exact code SHA `621800833c181fd65dd6d6f4c13c2a4e3cb1c7e8`: unit tests, Debug assembly, pinned HyperOS target verification, and Modern Xposed metadata validation all pass. No Canary was generated. A final exact-head Runtime CI follows this documentation-only closure; no further code change is planned.


## 2026-10-01 — Build 576 strict MIUIX typography correction

**Type:** App UI typography conformance only  
**Display version:** 0.0.3  
**Build:** 576 / `20261001-576`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Second audit finding

A second exact-revision review of Build 574 found that the component choices were correct, but three text nodes still used hand-added font weights:
- style name: `title2.fontSize + FontWeight.Medium`;
- Add-page title: `title2.fontSize + FontWeight.Medium`;
- Add-page caption: `headline1.fontSize + FontWeight.Medium`.

Pinned MIUIX `TextStyles.kt` defines `title2 = 24sp` and `headline1 = 17sp` **without** those weights. Therefore the prior code was MIUIX-adjacent rather than strictly default.

### Change

- Style name now uses `style = MiuixTheme.textStyles.title2`.
- Add-page title now uses `style = MiuixTheme.textStyles.title2`.
- Add-page caption now uses `style = MiuixTheme.textStyles.headline1`.
- Remove the now-unused `FontWeight` import.

### Deliberate product-specific exceptions

These are retained and must not be represented as MIUIX defaults:
- Add glyph at 32dp: explicit maintainer request for a stronger/larger plus; the container itself remains native 60dp MIUIX FloatingActionButton.
- Header swatches 28dp and mode-row swatches 24dp: pinned MIUIX provides no static color-preview-circle component/spec. Guiyuan therefore defines presentation sizes while still using MIUIX Surface/public drawCheckerboard and no project Canvas.

### 审查 / review

- no hierarchy change;
- no spacing/layout change;
- no state/persistence/runtime change;
- no custom font weight remains in the scheme/Add page;
- native ArrowPreference/BasicComponent/FloatingActionButton ownership remains intact;
- no Canary after CI without explicit maintainer instruction.


## 2026-10-01 — Build 575 custom-style dialog MIUIX state audit

**Type:** App UI state semantics / MIUIX conformance  
**Display version:** 0.0.3  
**Build:** 575 / `20261001-575`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Trigger

Maintainer device screenshots showed the custom-style management / rename controls and explicitly required MIUIX conformance at every detail level, including typography and font weight.

### Exact pinned MIUIX audit

Revision: `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.

Verified directly:
- `OverlayDialog`
  - title: MIUIX `title4` = 18sp, Medium, centered;
  - default inside margin: 24x24dp;
  - default outside margin: 12x12dp;
  - mobile bottom-attached corner radius derives from screen corners and is clamped to 32..48dp.
- `TextField`
  - default text style: `main` = 17sp;
  - floating label: 10dp;
  - corner radius: 16dp;
  - inside margin: 16x16dp.
- `TextButton`
  - default text style: MIUIX `button` = 17sp;
  - min height: 40dp;
  - corner radius: 16dp;
  - inside margin: 16dp horizontal / 13dp vertical.
- Official OverlayDialog two-action example:
  - two equal-weight TextButtons;
  - 20dp spacer;
  - affirmative action uses `ButtonDefaults.textButtonColorsPrimary()`.
- `BasicComponent`
  - title: headline1 17sp Medium;
  - minimum row height: 56dp;
  - inset: 16dp;
  - summary: body2 14sp.

### Change

- Keep all existing native dimensions and typography; do **not** hard-code substitute font sizes/weights.
- Create dialog: drive `TextButton.enabled` directly from `name.trim().isNotEmpty()`; invalid state therefore uses MIUIX native disabled colors and interaction.
- Rename dialog: affirmative button is enabled only when trimmed text is non-empty **and** differs from the current displayed scheme name.
- Callbacks receive the already-trimmed value; remove silent no-op guards from the click callback.
- Management action list remains `BasicComponent`; Copy already uses its native disabled state when custom-style capacity is full.
- Delete remains the only destructive action; its title color now goes through `BasicComponentDefaults.titleColor(color = error)` instead of a hand-built `BasicComponentColors`, keeping disabled/title semantics under MIUIX ownership.
- Rename pre-fills the same effective display name used by the style page/management title, including the legacy unnamed-style fallback.

### 审查 / review — pre-commit

- no custom typography added;
- no custom font weight added;
- no custom dialog radius/margin/size added;
- no project drawing added;
- no persistence/runtime/model change;
- Build-574 first-page hierarchy untouched;
- no Canary after CI without explicit maintainer instruction.

### Validation

Run exact-head automated CI only. Freeze the green SHA and wait.


## 2026-10-01 — Build 574 scheme page becomes a MIUIX settings page

**Type:** App UI hierarchy / MIUIX setting-row semantics  
**Display version:** 0.0.3  
**Build:** 574 / `20261001-574`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Maintainer direction

The battery-color drawer is a **settings page**, not an information-display page. The first Pager page must visibly establish the selected style, then present every battery semantic mode as one setting item. The style title and preview are centered. Drawer swatches should be larger and cleaner than the compact Function-page summary. The Add Pager page should preserve the visual “large + on a circle” concept while using MIUIX rather than project drawing.

### Exact MIUIX basis

Verified against pinned revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`:
- `ArrowPreference` is implemented on top of `BasicComponent` and provides the standard MIUIX title + summary + native Basic ArrowRight affordance;
- `BasicComponent` owns the common 56dp minimum row height / 16dp inset / title-summary typography;
- `FloatingActionButton` is a MIUIX `Surface` with `CircleShape`, default 60x60dp minimum size and 4dp shadow;
- MIUIX text styles expose `title2 = 24sp`;
- theme roles provide `primary/onPrimary` and disabled button colors.

### Changes

- Replace the previous small header Card with a centered style header:
  - style name uses MIUIX `title2` (24sp, Medium);
  - six style-preview swatches are 28dp with 10dp spacing;
  - drawer header swatches use MIUIX `Surface` and no outline.
- Replace the previous mode/color/HEX/action table with six actual setting rows:
  - custom scheme mode -> `ArrowPreference`;
  - built-in scheme mode -> the same underlying `BasicComponent` setting geometry without a misleading edit arrow;
  - title = semantic mode;
  - summary = current HEX or Follow inversion;
  - right-side preview = 24dp borderless fixed/checker swatch;
  - MIUIX owns row height, inset, typography, native arrow and click feedback.
- Update Chinese row labels to the maintainer wording: 普通 / 省电模式 / 性能模式 / 超级省电 / 充电 / 低电量. English Power save / Performance become Power save mode / Performance mode.
- Keep the Function-page compact preview unchanged through parameterized defaults (12dp / 6dp / bordered); drawer-only calls request the larger borderless presentation.
- Replace the Add-page interactive Card with native MIUIX `FloatingActionButton`:
  - default 60dp circular FAB and 4dp shadow;
  - theme primary/onPrimary colors;
  - `MiuixIcons.Add` at 32dp for the maintainer-requested stronger plus;
  - centered MIUIX title2 “新建样式” and action label;
  - max-cap state keeps the 60dp circle using a non-clickable MIUIX Surface + disabled button colors.
- Remove the obsolete Add-card `PressFeedbackType`, `holdDownState` wiring and the previously misused `Forward` icon.

### 审查 / review — pre-commit

- **MIUIX-first:** mode rows are real MIUIX setting components rather than a hand-built Row imitation; the Add action is the native MIUIX FAB.
- **visual hierarchy:** style identity is the first visual level; settings follow below.
- **swatch semantics:** Function-page summary stays compact; drawer header and mode rows are explicitly larger. Drawer fixed/checker circles have no outline as requested.
- **built-in behavior:** built-ins retain normal visual weight and the same setting-row geometry but do not display a false edit arrow.
- **custom behavior:** only custom scheme rows expose MIUIX ArrowPreference navigation to the existing mode editor.
- **Runtime / persistence:** untouched.
- **drawing:** no new project Canvas/draw primitive is introduced; checkerboard remains MIUIX public `drawCheckerboard()`.
- **scope:** no second-level editor redesign in this build.

### Validation / gate

Run exact-head automated CI. **Do not trigger Canary after CI.** Freeze the green SHA and wait for explicit maintainer instruction.


## 2026-10-01 — Build 573 final MIUIX proportion / optical pass

**Type:** App UI proportion / spacing normalization  
**Display version:** 0.0.3  
**Build:** 573 / `20261001-573`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Basis

After the component-first pass was structurally closed in Build 572, the maintainer requested one final global layout review against MIUIX itself rather than screenshot-fitted tuning.

The exact pinned dependency is:
- MIUIX `0.9.4-5c91d5e5-SNAPSHOT`;
- revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.

Exact defaults read from that revision:
- `BasicComponent`: 56dp minimum height, 16dp internal padding, 8dp action spacing; title uses headline1 (17sp Medium), summary uses body2 (14sp);
- `Card`: 16dp corner radius, 0dp default internal padding;
- `BottomSheet`: 28dp top corner radius, 24dp horizontal internal margin, 640dp max width; title uses title4 18sp Medium; title row top/bottom 6/12dp;
- official BottomSheet demo: in-sheet `SmallTitle` uses 16dp horizontal / 8dp vertical margins and Cards are not given an additional horizontal inset;
- `IconButton`: 40x40dp minimum hit target;
- `TextField`: 16dp corner radius, 16x16dp internal margin;
- MIUIX HSV sliders: 26dp track height, 20dp indicator;
- `FloatingToolbar`: 50dp corner radius, surfaceContainer background, 4dp default shadow.

### Changes

- Fix the inner BottomSheet navigation Pager at the previously chosen 520dp baseline instead of letting `heightIn(520..650)` expand the page and create unnecessary blank vertical space.
- Remove the extra 12dp horizontal page/editor inset because `OverlayBottomSheet` already supplies 24dp.
- Normalize scheme-header custom padding to 16dp and remove the mode-card's ad-hoc 2dp internal margin; mode rows therefore use native `BasicComponent` 56/16 geometry.
- Scheme title uses MIUIX headline1 17sp Medium.
- Scheme mode value/status column uses MIUIX body2 14sp + summary color rather than default-size secondary text.
- Keep the approved aligned mode / swatch / value / action columns and their functional widths.
- Add page keeps the approved centered add-card concept, but centers the card within the Pager, uses a 24dp MIUIX Add icon, 8dp icon-label gap and 24dp vertical card inset.
- Replace the page-indicator container Card with actual MIUIX `FloatingToolbar`; page dots remain MIUIX `Surface` primitives and preserve the active short-pill behavior.
- Detail page title becomes `<mode> color` / `<模式>颜色`; remove the redundant custom-scheme subtitle below it.
- Use the official BottomSheet-demo section-title margin of 16x8dp throughout the editor.
- Keep custom-content Card insets at 16dp; remove the residual 16x14dp asymmetry.
- Current color preview moves from 28dp to 26dp to align with the native HSV track height.
- Palette strip mini swatches use 12dp with 6dp spacing.
- Common-color options retain a 40dp clickable MIUIX `Surface` target but reduce the visible swatch to ~20dp; selected state uses a white MIUIX Surface ring + 2dp shadow around the smaller color core instead of a giant filled 36dp disc.
- Reset action icon uses the standard 24dp icon footprint.

### 审查 / review — pre-commit

- **hierarchy:** unchanged; one BottomSheet, one whole-scheme Pager, fixed floating indicator, in-sheet detail page, default-collapsed source selector;
- **behavior:** source order, copy-on-write, template references, custom persistence, five-style cap, create/rename/delete and destructive error semantics unchanged;
- **MIUIX:** all visual primitives remain MIUIX components/public APIs; no project Canvas/drawCircle/drawRect/drawWithCache/raw clickable was introduced;
- **Runtime:** no Xposed hook, state source, painter, transition, geometry, reservation, color policy or Runtime preference-key change;
- **no screenshot fitting:** numeric changes are traced to exact MIUIX defaults/examples or preserve an already-approved Guiyuan functional alignment constraint.

### Validation

Run exact-head automated CI. If green, perform a final code review and build one signed exact-head Canary for App-UI device validation.


## 2026-10-01 — Build 572 final battery-color editor MIUIX semantics

**Type:** App UI component semantics only  
**Display version:** 0.0.3  
**Build:** 572 / `20261001-572`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Goal

Close the remaining component-semantic gaps in the final custom-mode editor before beginning the maintainer-requested global MIUIX ratio/proportion review. The approved page hierarchy, source model, copy-on-write behavior and layout remain unchanged.

### Change

- Color source stays an in-place expandable MIUIX `BasicComponent`.
- While expanded, that component now uses its built-in `holdDownState`.
- Its end area keeps the current fixed-color/MIUIX-checker preview and adds:
  - `MiuixIcons.ExpandMore` while collapsed;
  - `MiuixIcons.ExpandLess` while expanded;
  - `onSurfaceVariantActions` tint.
- Restore-this-mode remains a normal, non-destructive `BasicComponent` action and gains `MiuixIcons.Reset` with the same MIUIX action tint.
- Existing `RadioButtonPreference`, `HsvHueSlider`, `HsvSaturationSlider`, `HsvValueSlider`, `TextField`, `Card`, `SmallTitle`, and MIUIX `Surface` color chips remain authoritative.
- No switch to full `ColorPicker`: its alpha channel would expose unsupported transparency semantics, while the separate MIUIX HSV controls already match Guiyuan's opaque-color contract.

### Exact-version verification

Verified directly against pinned MIUIX revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`:
- `MiuixIcons.ExpandMore`;
- `MiuixIcons.ExpandLess`;
- `MiuixIcons.Reset`;
- `BasicComponent.holdDownState`;
- `MiuixTheme.colorScheme.onSurfaceVariantActions`.

### 审查 / review

- information architecture unchanged;
- source remains default-collapsed and expands in place;
- no new project drawing;
- no new dependency;
- no persistence/runtime/color-policy change;
- Restore remains non-destructive; Delete remains the only error-colored destructive action;
- copy-on-write from template/follow source to Custom is untouched;
- final numeric sizing/spacing/typography tuning is intentionally deferred to the next single global MIUIX proportion pass.

### Validation

Run exact-head automated CI. A green result closes component semantics for the entire battery-color flow and unlocks the final MIUIX ratio/proportion review before the next device Canary.


## 2026-10-01 — Build 571 Add-card hold-state compile correction

**Type:** compile-only correction  
**Display version:** 0.0.3  
**Build:** 571 / `20261001-571`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence

Build 570 Runtime CI run 36909145146 reported one Kotlin error only: the sole `BatterySchemeOverview` call did not pass `addHeldDown`, which was introduced solely to feed MIUIX Card `holdDownState` while the create-style dialog is visible.

### Correction / review

- pass `addHeldDown = showCreateDialog` at the only overview call;
- verify the generated source contains both `managedCustomId = manageCustomId` and `addHeldDown = showCreateDialog` before commit;
- no UI structure, proportion, color, persistence, or Runtime change.

### Validation

Run exact-head automated CI. If green, close the pre-editor component pass and continue with the final custom-mode editor component semantics.


## 2026-10-01 — Build 570 pre-editor MIUIX interaction-state pass

**Type:** App UI component semantics only  
**Display version:** 0.0.3  
**Build:** 570 / `20261001-570`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Goal

Continue the agreed component-first pass through the pages before the final color editor, without changing the approved layout or beginning the final proportion-tuning phase.

### Change

- Feature-page `Battery colors` remains an MIUIX `ArrowPreference`, and now feeds `showBatteryColorSheet` into its built-in `holdDownState`; the originating preference therefore retains the standard MIUIX pressed ownership while its BottomSheet is open.
- The Add-scheme page keeps the existing centered MIUIX `Card + Add icon + label` composition.
- Its interactive Card now uses MIUIX `PressFeedbackType.Sink`, native indication, and `holdDownState` while the create-style OverlayDialog is visible.
- Custom-scheme More already uses MIUIX `IconButton.holdDownState` while the management dialog is open; no change needed.
- Create/rename/delete dialogs remain `OverlayDialog + TextField/BasicComponent + TextButton`; their two-button/20dp confirmation layout matches the pinned MIUIX 0.9.4 documentation and is intentionally retained.

### Exact-version basis

Verified against pinned MIUIX revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`:
- `ArrowPreference.holdDownState`;
- interactive `Card.pressFeedbackType / showIndication / holdDownState`;
- `PressFeedbackType.Sink`;
- `IconButton.holdDownState`.

### 审查 / review

- call-chain scan: `BatteryColorPreference`, `BatterySchemeOverview`, and `BatteryAddSchemePage` each have one invocation and one declaration in their owning files;
- no new project drawing;
- no layout/proportion tuning;
- no data/persistence/runtime modification;
- no change to destructive Delete semantics.

### Validation

Run exact-head automated CI. If green, the entry/scheme/add/custom-management layers are considered component-closed. Continue next with the final custom-mode editor's component semantics, then perform the single global MIUIX ratio/proportion review requested by the maintainer.


## 2026-10-01 — Build 569 MIUIX management-state compile correction

**Type:** compile-only correction  
**Display version:** 0.0.3  
**Build:** 569 / `20261001-569`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence

Build 568 Runtime CI run 36908190302 reached `:app:compileDebugKotlin` and reported one error only: the sole `BatterySchemeOverview` invocation did not pass the newly introduced `managedCustomId` parameter used to drive MIUIX `IconButton.holdDownState`.

### Correction

- Pass `manageCustomId` into the existing overview call.
- Directed scan confirms `BatteryColorControls.kt` has exactly one invocation and one function declaration, so there is no second call site to reconcile.

### 审查 / review

- compile-only wiring change;
- no layout/proportion adjustment;
- no new drawing primitive;
- no Runtime/persistence/data-model change;
- Build-568 MIUIX component substitutions remain otherwise byte-for-byte unchanged.

### Validation

Run exact-head automated CI. If green, resume the component-first cleanup; final proportion tuning remains deferred until the last editor page is structurally complete.


## 2026-10-01 — Build 568 battery-color MIUIX component-conformance pass

**Type:** App UI component-conformance only  
**Display version:** 0.0.3  
**Build:** 568 / `20261001-568`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device-review trigger

Build 567 was functionally valid and automated validation was fully green, but maintainer screenshots showed that several battery-color surfaces merely used Compose/MIUIX-adjacent styling rather than consistently expressing MIUIX component semantics. The requested correction is not a new layout. The previously approved hierarchy and interaction model remain authoritative; implementation primitives should be MIUIX wherever available, with project drawing avoided.

### Change

- Replace the in-sheet text `Back` pill with MIUIX `IconButton` + `MiuixIcons.Back`.
- Replace the project text glyph `›` with `MiuixIcons.Forward`.
- Refactor each semantic mode row from a project-owned title Row into MIUIX `BasicComponent(title, endActions)`; only the already-approved color/value/action columns remain composed in `endActions`.
- Replace project clip/background pagination primitives with MIUIX `Surface`.
- Replace project-drawn fixed-color chips with MIUIX circular `Surface` components and `BorderStroke` parameters.
- Replace the project Canvas checker swatch with a circular MIUIX `Surface` whose content uses the **public MIUIX 0.9.4 `Modifier.drawCheckerboard()`** from `ColorPicker.kt`.
- Replace project common-color Box/background/border/clickable construction with clickable MIUIX `Surface` composition. Selection remains a simple white MIUIX-surface ring, consistent with the current ColorPalette/ColorSlider visual language, without copying their private draw implementation.
- Keep custom management on MIUIX `OverlayDialog + BasicComponent` because current DropdownItem coloring is menu-wide and cannot preserve the already-approved error-red Delete row independently. The More `IconButton` now uses MIUIX `holdDownState` while its management dialog is visible.
- Keep MIUIX `HsvHueSlider / HsvSaturationSlider / HsvValueSlider`, `TextField`, `Card`, `RadioButtonPreference`, `OverlayDialog`, and Pager spring/gesture APIs unchanged.

### Exact-version verification

The project is pinned to `miuix.version=0.9.4-5c91d5e5-SNAPSHOT`, revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`. Before commit, that exact revision was checked for:
- `Surface` clickable/non-clickable overloads with shape/border/shadow;
- public `Modifier.drawCheckerboard()`;
- `MiuixIcons.Back` and `MiuixIcons.Forward`;
- `IconButton.holdDownState`;
- `surfaceContainer` theme role.

### 审查 / review

- **layout contract:** unchanged; this is component substitution, not a re-layout.
- **self-drawing:** directed scan of the new `BatteryColorControls.kt` reports zero project `Canvas`, `drawWithCache`, `drawCircle`, `drawRect`, raw `.background(`, raw `.border(`, raw `.clickable`, or text-glyph chevrons.
- **MIUIX boundary:** checkerboard rendering is invoked only through MIUIX's own public API; Guiyuan does not copy/reimplement its drawing algorithm.
- **destructive semantics:** Delete remains error-colored and confirmation-gated.
- **Runtime / persistence:** untouched; no hook, state source, writer, color projection, preference schema, or SystemUI behavior changes.
- **proportion discipline:** no final spacing/size/typography tuning is attempted in this pass. That review is intentionally deferred until the last editor page is structurally complete, then will be based on pinned MIUIX defaults rather than screenshot fitting.

### Validation

Run exact-head automated CI. If green, continue the component-first cleanup. Do not request broad device testing yet; the next device visual gate belongs after the final MIUIX ratio/proportion pass.


## 2026-10-01 — Build 567 HyperOS-default contract-test correction

**Type:** test-only contract correction  
**Display version:** 0.0.3  
**Build:** 567 / `20261001-567`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence

Full CI #2096 / run 36904240233 compiled Debug and Canary successfully and reached `:app:testDebugUnitTest`. Of 426 tests, exactly one failed: `CombinedStatusColorPolicyTest.modeColorOnlyChangesBatteryByDefault`. That assertion still expected `CombinedStatusRecommendedBatteryPalette.PERFORMANCE`, which contradicts the maintainer-approved Build-564 contract that HyperOS is the default scheme.

### Correction

- Change the default color-policy expectation from Recommended/Low-saturation to `CombinedStatusHyperOsBatteryPalette.PERFORMANCE`.
- Strengthen the HyperOS charging test: supply a deliberately non-HyperOS runtime semantic input (`#123456`) and assert the selected HyperOS fixed template still resolves to the pinned `#1DCD3A`. This distinguishes the new fixed-template contract from the old `SystemDefault` behavior rather than passing accidentally because the target runtime color happens to equal the template.

### 审查 / review

- production source is untouched;
- the failed assertion is demonstrably stale relative to the user-approved default/order and exact-target template model;
- center/mobile remain status-tint by default; only battery-family outputs consume the selected scheme unless linkage switches are enabled;
- this change increases regression strength by explicitly separating fixed-template resolution from runtime semantic input.

### Validation

Run exact-head Full CI. A green result closes automated validation for the battery-color redesign and permits exact-head signed Canary generation.


## 2026-10-01 — Build 566 Compose padding compile correction

**Type:** compile-only correction  
**Display version:** 0.0.3  
**Build:** 566 / `20261001-566`  
**Branch / PR:** `feat/battery-top-readout` / #181

### CI evidence

Full CI #2095 / run 36903859758 cleared Build 564's Pager `PagerSnapDistance` mismatch and reached the next Kotlin compile check. The only reported source failure was `BatteryColorControls.kt:740`: Compose has separate `padding(horizontal, vertical)` and `padding(start, top, end, bottom)` overloads, so `padding(horizontal = 12.dp, bottom = 24.dp)` is invalid.

### Correction

- Replace that call with `padding(start = 12.dp, end = 12.dp, bottom = 24.dp)`.
- Directed scan of the same file's remaining `padding`, `PagerDefaults.flingBehavior`, and `heightIn` calls found no second matching overload misuse.
- No UI geometry value changes: horizontal 12dp and bottom 24dp are preserved exactly.

### 审查 / review

- compile-only; no behavior or state transition changes;
- Build-565 migration compatibility correction remains intact;
- MIUIX interaction ownership and destructive-error semantics remain intact;
- App-only scheme metadata / Runtime projection boundary remains unchanged;
- no new dependency, hook, listener, animator, writer, or persistence key.

### Validation

Run exact-head Full CI. If green, close automated validation and proceed to one signed exact-head Canary for the new battery-color BottomSheet visual/interaction review.


## 2026-10-01 — Build 565 battery-color post-review correction

**Type:** compile correction + compatibility review  
**Display version:** 0.0.3  
**Build:** 565 / `20261001-565`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Build 564 CI finding

Full CI #2094 / run 36903357998 reached Kotlin compilation and failed on one API binding in `BatteryColorControls.kt`: the positional `PagerNavigationSpringSpec` argument was interpreted as `PagerSnapDistance` by the current Compose `PagerDefaults.flingBehavior` signature. MIUIX's current guide/example uses named `state` and `snapAnimationSpec`; Build 565 follows that exact form.

### Review corrections

- Use:
  `PagerDefaults.flingBehavior(state = pagerState, snapAnimationSpec = PagerNavigationSpringSpec)`.
- Legacy migration now preserves **effective behavior**, not invalid raw state: an old slot marked CUSTOM with no stored color maps back to the active preset source, matching the previous Runtime fallback. A pure helper/test locks this rule.
- Dormant stored custom colors remain preserved when the old slot currently uses Preset.
- Aligned custom mode rows now route click ownership through MIUIX `BasicComponent`; the Add scheme page uses MIUIX interactive `Card` feedback.
- Destructive Delete remains `MiuixTheme.colorScheme.error` and confirmation-gated.
- No scheme-library key is added to Runtime visual-key classification; only the flattened active color keys synchronize to SystemUI.

### 审查 / review

- **ownership:** unchanged; no new SystemUI hook/listener/animator/geometry writer/painter.
- **MIUIX:** Pager spring invocation now matches the current upstream documentation and library API; interaction surfaces use MIUIX components where available.
- **migration:** active behavior and dormant custom memory are both retained; invalid legacy CUSTOM-without-color cannot create an empty custom source.
- **scope:** App UI/settings + existing color-policy projection only.
- **device gate:** still UI-focused after automated validation; no need to repeat the broader transition matrix unless colors/runtime unexpectedly diverge.

### Validation

Run exact-head Full CI. If green, freeze Build 565 and produce one signed exact-head Canary for the battery-color BottomSheet interaction/visual review.


## 2026-10-01 — Build 564 MIUIX battery-color scheme library

**Type:** App UI / settings schema / Runtime projection  
**Display version:** 0.0.3  
**Build:** 564 / `20261001-564`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Goal

Replace the temporary Build-561/562 battery-color selector with the agreed hierarchy: one BottomSheet, whole-scheme horizontal paging, aligned six-mode previews, up to five named custom schemes, and one in-sheet mode editor whose first edit automatically becomes Custom.

### Exact-target color basis

Directed review of `CHS-Haple/SystemUI-Reference` for SystemUI `17.03.260226.r` confirms the status-bar semantic resources:
- charging `#1DCD3A`;
- power save `#FF9F05`;
- performance `#3482FF`;
- low battery `#FA382E`;
- no distinct target-proven super-power-save progress color; Guiyuan's HyperOS template reuses power-save for that slot;
- Normal remains the status-icon tint/inversion path and is represented by the checker/mosaic semantic rather than a fake fixed HEX.

The new HyperOS built-in is intentionally a fixed verified template. `Follow inversion` remains a separate per-mode source.

### Data / migration

- Add an App-side `BatteryColorSchemeLibraryRepository` in the existing visual preferences file.
- Built-in order/default: HyperOS -> iOS -> Low saturation.
- Custom scheme cap: five, with stable IDs and smallest-free-ID naming support.
- Each custom mode stores a source reference: HyperOS / iOS / Low saturation / Follow inversion / Custom.
- Template references remain references in App metadata; only Custom stores an authored fixed color.
- Activating a built-in/custom scheme projects to the pre-existing Runtime `batteryColorPreset / mode / override` keys. Scheme names/order/library metadata are not Runtime keys and therefore never cross into SystemUI.
- Legacy migration keeps the currently effective preset/mode result and retains dormant stored custom colors even when the old slot was currently set back to Preset.
- Deleting the active custom scheme returns to its recorded base built-in.

### MIUIX UI

- Keep one `OverlayBottomSheet`; detail editing is an internal spring page transition rather than stacked sheets.
- Scheme card + six mode rows form one `HorizontalPager` page and move together.
- Pager uses the upstream `PagerNavigationSpringSpec`, `pagerGestureOverride`, and `PagerGestureNestedScrollConnection`.
- A fixed adaptive MIUIX `Card` capsule below the pager contains dot indicators; the active page stretches to a short pill.
- Built-in rows are read-only and reserve the same action-column width as custom rows.
- Custom rows expose the mode editor.
- Color source is collapsed by default; selecting a source updates the editor, and changing common colors / HSV / HEX / RGB performs copy-on-write to Custom.
- Create and rename use `OverlayDialog`; Add/More use MIUIX icons from the already-present icons dependency.
- Delete uses `MiuixTheme.colorScheme.error` in both the management row and confirmation action.

### 审查 / review — pre-commit

- **single writer:** no new SystemUI painter, geometry writer, state observer, hook, or animator; Runtime still consumes the existing flattened color keys.
- **process boundary:** custom scheme metadata is App-only; `isCombinedStatusVisualPreferenceKey` is intentionally unchanged for library keys.
- **native-first:** Pager spring/gesture, Card, Dialog, Button, Radio preference, HSV sliders, TextField and icons use MIUIX APIs. Only the checker swatch and compact page dots are project-drawn display primitives because MIUIX 0.9.4 has no PagerIndicator component.
- **migration:** old active color behavior is representable; dormant custom values are retained instead of silently discarded.
- **destructive action:** Delete is error-colored and confirmation-gated.
- **copy-on-write:** a template/follow source remains referenced until the first actual edit; the first edit is applied without a value jump and changes the slot source to Custom.
- **Fail-native / Runtime:** the fixed HyperOS template is tied to the verified target and documented as target evidence, not a universal Xiaomi constant.

### Validation

Run exact-head CI first. If green, a signed Canary is warranted for visual/interaction review of the new BottomSheet hierarchy; Runtime device testing is only required if observed colors differ from the projected fixed/template result.




## 2026-10-01 — Build 549 faster continuous retract AB

**Type:** focused device-evidence timing refinement  
**Display version:** 0.0.3  
**Build:** 549 / `20261001-549`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Device evidence

Build-548 video confirms the continuous retract topology remains correct and the Build-545 discontinuity does not return. A small outlet-side residual arc is still visible for several frames after CENTER/network has already committed to the exit, so the remaining defect is only late completion.

### Root cause

Build 548 finishes the ring at 45% of the existing HyperOS transition clock. The symmetric smoothstep keeps the last visible arc continuous, but the completion point is still later than the desired choreography.

### Change

Single-variable AB:
- `TRANSITION_COMPLETE_PROGRESS: 0.45f -> 0.35f`;
- local ring progress reaches 0.5 at global progress 0.175 and 1.0 at 0.35;
- existing symmetric smoothstep remains unchanged;
- LEFT/RIGHT/NONE ordered-arc semantics remain unchanged;
- CENTER/network native target geometry and timing remain unchanged;
- Battery target/handoff, reverse symmetry, and Build-542 island behavior remain unchanged.

### 审查 / review

- timing scalar only; no new geometry gate, Animator, delay, or second timeline;
- Build-546/547/548 continuity preserved; Build-545 live gate remains rejected;
- ownership, lifecycle, single-writer, Fail-native, and performance behavior unchanged;
- steady rendering, battery-top controls, typography handoff, native peer motion, and island projection untouched;
- remap test now locks 0.175 -> 0.5 and 0.35 -> 1.0; existing arc/direction tests remain authoritative.

### Validation

Run exact-head Runtime CI, then one signed exact-head Canary. Primary device check: residual outlet-side arc should clear earlier than Build 548 while remaining visually continuous on normal/fast pulls and symmetric on reverse collapse.


## 2026-10-01 — Build 550 front-loaded continuous retract AB

**Type:** focused device-evidence curve refinement  
**Display version:** 0.0.3  
**Build:** 550 / `20261001-550`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Device evidence

Build 549 moves full Battery-ring completion to 35% of the existing HyperOS transition clock. Maintainer feedback says the first half of the visible retract still feels too similar to the prior version. The requested change is therefore not another earlier terminal cutoff; it is a faster first half while preserving the accepted continuous topology.

### Root cause

The Build-549 local curve is still symmetric smoothstep. Its zero start slope intentionally eases in, so even with an earlier 35% terminal point the first part of the retract remains visually conservative.

### Change

Keep `TRANSITION_COMPLETE_PROGRESS = 0.35` and front-load only the shape-progress curve:
- warp normalized local progress with `p + 0.45 * p * (1 - p)`;
- feed the warped value into the existing smoothstep;
- at local p=0.25, consumed sweep increases from 15.625% to about 26.065%;
- at local p=0.50, consumed sweep increases from 50.0% to about 66.590%;
- endpoints remain exact (0 -> 0, 1 -> 1), with smoothstep retaining zero endpoint slope;
- no piecewise threshold, jump, delay, Animator, or second timeline is introduced.

### 审查 / review

- **scope:** curve shape only; 35% completion point unchanged.
- **continuity:** one monotonic continuous warp followed by the existing continuous smoothstep; no Build-545 gate behavior.
- **ownership / lifecycle / single writer:** unchanged.
- **native motion:** CENTER/network target path and HyperOS expansion clock remain authoritative.
- **reverse:** the same stateless mapping is evaluated in reverse; no separate collapse animator.
- **performance:** constant arithmetic only; no allocation, probe, reflection, listener, or hierarchy traversal.
- **tests:** explicit curve checkpoints lock the faster first half; ordered-arc tests derive geometry from the policy's remaining fraction so they continue to verify topology rather than hard-code the old easing.

### Validation

Run exact-head Runtime CI and then one signed exact-head Canary. Device focus: compare Build 549 vs 550 during the first half of a normal and slow pull. The ring should yield visibly sooner from the start while the last part remains continuous, with no chunk disappearance or change to CENTER/network trajectory.


## 2026-10-01 — Build 551 Preview Sandbox mobile-network coverage

**Type:** preview/UI coverage + regression tests  
**Display version:** 0.0.3  
**Build:** 551 / `20261001-551`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Expose the mobile standards already handled by the generic native-label rendering path in Preview Sandbox instead of limiting manual preview to None / 4G / 5G / 5G-A.

### Implementation

- Add Preview Sandbox choices for `2G`, `E`, `3G`, `H+`, and `LTE`.
- Final UI order: None / 2G / E / 3G / H+ / 4G / LTE / 5G / 5G-A.
- Keep the original PreviewMobileNetwork ordinals (None=0, 4G=1, 5G=2, 5G-A=3) stable; new enum values append after them. The UI uses an explicit ordered choice list rather than enum ordinal order.
- Add `systemLabel` to PreviewMobileNetwork and feed it directly into the existing `CenterIndicator.MobileType` model.
- Use one horizontally scrollable MIUIX `TabRowWithContour` at a fixed comfortable content width rather than squeezing nine labels into the card width.
- Add bilingual resource entries; technology labels remain standards notation in both locales.
- Add production-path regression coverage proving 2G / E / 3G / H+ / 4G / LTE labels pass through `NativePresentationResolver.normalizeDrawableNetworkType` unchanged.

### 审查 / review

- **runtime architecture:** unchanged; production still reads HyperOS `mobile_type` / `mobile_type_single` and renders generic native text.
- **no per-standard fork:** no extra production branch for 2G/3G/LTE/H+ is introduced.
- **state compatibility:** legacy preview ordinals are preserved to avoid rememberSaveable restoring an old 4G/5G selection as a newly inserted standard.
- **layout:** scrolling prevents label compression; no custom density/touch geometry.
- **scope:** sandbox UI/model/resources and tests only; current Build-550 battery-ring transition runtime remains untouched.

### Validation

Run Runtime CI. Because the functional mapping is deterministic, no runtime-transition device gate is required. A Canary is useful only to visually review the nine-option sandbox control and confirm scrolling/touch ergonomics on the target device.


## 2026-10-01 — Build 552 smooth long-tail Battery-ring retract AB

**Type:** focused device-evidence curve refinement  
**Display version:** 0.0.3  
**Build:** 552 / `20261001-552`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Keep the accepted fast first half from Build 550, but make the latter half slower and finish later without creating an obvious two-speed or piecewise animation.

### Root cause / design

A literal 50/50 piecewise timing split would create a slope handoff that can read as layered speed. Build 552 instead keeps the same single analytic form used by Build 550 and retunes its two scalar parameters:
- completion point: `0.35 -> 0.45`;
- continuous front-load coefficient: `0.45 -> 0.92`;
- warp remains `p + FRONT_LOAD * p * (1 - p)`, followed by the same smoothstep.

This produces one monotonic continuous curve. The stronger front-load offsets the longer completion window during the early phase, while the longer terminal window and low end slope stretch the tail naturally.

### Checkpoints

Composite global progress -> consumed ring:
- 0.0875 -> about 26.62% (Build 550: about 26.06%);
- 0.175 -> about 65.88% (Build 550: about 66.59%);
- 0.35 -> about 98.85% (Build 550: 100%);
- 0.45 -> 100%.

So the first half stays visually close to Build 550, while the final ~1.15% decays through the extra tail instead of disappearing at 35%.

### 审查 / review

- one continuous stateless curve; no piecewise speed tier, threshold gate, delay, Animator, or second clock;
- ordered-arc topology and LEFT/RIGHT/NONE semantics unchanged;
- CENTER/network native target path and timing authority unchanged;
- Battery handoff, reverse symmetry, Build-542 island projection, and Build-551 sandbox coverage unchanged;
- constant arithmetic only; no new runtime allocations or hierarchy work;
- tests now lock both local curve continuity checkpoints and the intended global early/tail relationship.

### Validation

Run exact-head Runtime CI, then signed exact-head Canary because the requested difference is visual. Device focus: normal and slow pulls. The first half should feel essentially as quick as Build 550, then decelerate naturally into a slightly later tail without any visible speed step or chunk disappearance.


### Build 552 CI correction

Runtime CI #2066 failed only in `leftExitPreservesBatterySemanticsByIntersection`: the stronger continuous front-load means that at local progress 0.5 the retained LEFT suffix begins after the original 75% active-fill end, so `result.active` is correctly empty. The test's unconditional `active.single()` assumption was stale.

Correction is test-only: sample the active-fill intersection at local progress 0.35, where the retained suffix still overlaps the original active fill. Runtime policy, Build ID, curve parameters, topology, and APK behavior remain unchanged.


## 2026-10-01 — Build 553 compact mobile-standard selector

**Type:** Preview Sandbox UI refinement  
**Display version:** 0.0.3  
**Build:** 553 / `20261001-553`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Device / design feedback

The nine-standard horizontally scrollable segmented control from Build 551/552 exposes every standard but creates too many visible slots and dominates the Network card.

### Implementation

- Replace only the mobile-standard control with MIUIX `OverlayDropdownPreference`.
- Keep Mobile/Wi-Fi as the existing two-option `TabRowWithContour`, since that is a primary mutually-exclusive mode switch with only two choices.
- The new row shows the current standard inline and opens a single-choice MIUIX popup for None / 2G / E / 3G / H+ / 4G / LTE / 5G / 5G-A.
- Limit popup height to 360dp so long option lists scroll inside the native popup instead of expanding the page.
- Reuse `SandboxPreferenceInsideMargin` so title/value spacing aligns with `SliderPreference` and `SwitchPreference`.
- Remove the custom horizontal-scroll segmented helper and its scroll-state imports.

### 审查 / review

- Uses the library's purpose-built preference component rather than custom geometry.
- No preview model, enum ordinal, runtime SystemUI path, transition curve, or state ownership changes.
- All nine network standards remain available in the same explicit display order.
- Build-552 Battery-ring transition runtime remains byte-for-byte untouched by this UI refinement.

### Validation

Run Runtime CI. One signed Canary is justified only to inspect popup placement, row density, current-value alignment, and interaction feel on the target device.


## 2026-10-01 — Build 554 customization settings foundation

**Type:** settings schema / color policy foundation  
**Display version:** 0.0.3  
**Build:** 554 / `20261001-554`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Prepare the requested combined-icon sizing, ring thickness, Wi-Fi/mobile-type tuning, feature reset, and per-battery-mode color customization before exposing the controls.

### Settings model

Profile-scoped geometry (independent for Network-centered and Battery-centered layouts):
- combined scale 85%-115%, default 100%;
- ring stroke scale 70%-130%, default 100%;
- Wi-Fi size 80%-125%, default 100%;
- mobile-type size 80%-125%, default 100%;
- mobile-type weight 500-950, default 800.

Wi-Fi weight is deliberately not exposed: the steady path preferentially renders a native SystemUI drawable. Synthetic dilation or blur would violate native-first rendering and risks optical fuzziness.

Global battery-color state:
- presets: HyperOS native / iOS style;
- custom opaque overrides for Normal, Power Save, Performance, Super Power Save, Charging, and Low;
- iOS-style defaults use yellow #FFCC00, blue #007AFF, orange #FF9500, green #34C759, red #FF3B30; Normal follows status-icon tint;
- custom per-slot overrides win over the selected preset.

### Runtime semantic groundwork

- Add `SUPER_POWER_SAVE` as a distinct semantic state and accept common native enum aliases if HyperOS exposes one.
- Probe optional `mBatterySuperPowerSaveColor` / `mBatterySuperSaveColor`; if absent, HyperOS-native fallback uses the existing power-save color field.
- Existing semantic authority remains `MiuiBatteryMeterIconView.getProgressStatus()`.

### Reset semantics

- `CombinedStatusVisualSettingsRepository.resetToDefaults()` clears all visual settings back to schema defaults.
- `CombinedStatusFeatureSettingsRepository.resetToDefaults()` clears feature settings and refreshes the feature-change timestamp.
- Battery-color overrides also have a dedicated reset helper so a palette can be restored without resetting unrelated controls.

### 审查 / review

- Color preset/overrides are global; geometry remains layout-profile scoped.
- No second runtime settings owner is introduced; `RuntimeVisualPreferencesOwner` stays the single visual-settings bridge.
- Build-552 transition curve and Build-553 sandbox selector are untouched.
- Geometry fields are not consumed by Painter in this checkpoint, preventing half-wired steady vs transition geometry.

### Validation

Runtime CI must lock normalization, runtime-key participation, preset resolution, override precedence, super-power-save parsing, and compilation before geometry/UI wiring proceeds.


## 2026-10-01 — Build 555 reset lifecycle correction

**Type:** lifecycle / settings synchronization fix  
**Display version:** 0.0.3  
**Build:** 555 / `20261001-555`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Pre-commit 审查 / review

The Build-554 foundation was re-reviewed before continuing geometry/UI work. The review found that both visual and feature reset helpers used `SharedPreferences.clear()`, while App/Runtime listeners filtered only concrete keys. Android clear notifications may use `key == null`, so reset could restore persisted defaults without immediately refreshing observers.

The candidate correction was reviewed before branch update:
- feature key relevance is single-source through `isCombinedStatusFeaturePreferenceKey()`;
- App and Runtime feature listeners share that predicate;
- visual key relevance treats `null` as a whole-domain change and Runtime already delegates to that same predicate;
- unrelated non-null keys still do not trigger feature updates;
- feature reset keeps the existing change timestamp in the same editor transaction;
- no second settings owner, poller, or restart path is introduced.

### Change

- Accept `key == null` as a relevant whole-domain change for the dedicated feature and visual preference files.
- Add shared feature-key predicate to prevent App/Runtime filter drift.
- Add unit coverage for clear notification relevance.
- No changes to Build-554 geometry ranges, color presets, semantic mapping, or painter behavior.

### Validation

Run exact-head Runtime CI. No real-device gate is required because this change only repairs observer invalidation semantics; UI reset controls are not exposed yet.


## 2026-10-01 — Build 556 outer-weight geometry wiring

**Type:** runtime geometry wiring  
**Display version:** 0.0.3  
**Build:** 556 / `20261001-556`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Requirement correction

Maintainer clarified that “outer-ring thickness” is intentionally a coupled visual family: changing it should also change the four mobile dots and the unavailable X mark, with dot spacing adapting so the whole lower opening remains visually even.

The repository already contains the correct primitive: `CombinedStatusOuterGeometry.resolve(weightScale)` scales:
- ring stroke;
- mobile-dot radius;
- unavailable-mark stroke and extent;

and then solves dot angular spacing so ring-to-dot and dot-to-dot edge gaps remain balanced.

### Pre-commit 审查 / review

The Build-556 candidate was reviewed before branch update:
- foundation naming changed from `ringStrokeScale` to `outerWeightScale` so UI/schema semantics match the actual coupled behavior;
- setting remains profile-scoped, default 1.0, supported UI range 0.70-1.30;
- all runtime outer-geometry entry points use `visualSettings.outerWeightScale`;
- steady draw, Battery/Battery-number transition draw, Mobile transition draw, and transition source bounds therefore share one geometry source;
- the only remaining static default is the painter cache initializer, which is replaced on first resolved draw and is not an authoritative runtime path;
- no new solver/animator/listener/writer is introduced;
- existing balanced-gap solver remains authoritative;
- existing `fiveVisualEdgeGapsStayBalancedAcrossSupportedScales` regression coverage is preserved;
- new test explicitly locks that ring, dots, and unavailable mark scale as one family.

### Compatibility

No user-facing Build-554/555 UI exposed the foundation-only `ring_stroke_scale` key, so renaming it to `outer_weight_scale` does not migrate a released user setting. Default 100% preserves the accepted 8.25 ring baseline and existing dot/X geometry.

### Validation

Run exact-head Runtime CI. No device gate is required yet because the control is not exposed in UI and the default value leaves runtime appearance unchanged.


## 2026-10-01 — Build 557 independent center geometry

**Type:** runtime geometry wiring  
**Display version:** 0.0.3  
**Build:** 557 / `20261001-557`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Expose only the requested center-family controls without letting Wi-Fi sizing accidentally resize native airplane/no-SIM icons.

### Pre-commit 审查 / review

The candidate was reviewed before branch update:
- remove the old shared `centerSizeScale` / `centerTextWeightScale` painter override API;
- one settings-backed resolver now supplies Wi-Fi size, mobile-type size, and mobile-type source weight to all center draw/transition/source-bound paths;
- Wi-Fi fallback vector uses only `wifiSizeScale`;
- airplane/no-SIM max sizes remain fixed at the accepted native optical baselines;
- mobile-type size scales text/suffix geometry only;
- mobile-type weight is absolute 500-950, default 800;
- custom mobile weight remains the transition source weight; existing `MobileTypeTransitionPolicy` still interpolates to the SystemUI target weight;
- settings UI bounds remain 80%-125% while lower-level geometry keeps a wider defensive clamp;
- zero legacy shared-size/shared-weight tokens remain in the candidate painter;
- no new writer, listener, animator, or target-geometry owner is introduced.

### Tests

Replace the obsolete “all center families share one size” test with independent contracts:
- Wi-Fi scale changes Wi-Fi only;
- mobile-type scale changes mobile text/suffix only;
- mobile-type weight changes typography only;
- airplane/no-SIM remain fixed when Wi-Fi changes;
- invalid/out-of-range inputs clamp safely;
- existing 5GA lower-right suffix direction remains locked.

### Validation

Run exact-head Runtime CI. Default values preserve current runtime appearance, so no device gate is required until UI controls are exposed.


## 2026-10-01 — Build 558 shrink-only overall combined scale

**Type:** runtime geometry wiring  
**Display version:** 0.0.3  
**Build:** 558 / `20261001-558`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Final range

Maintainer set 100% as both the default and maximum overall size. The supported range is therefore 75%-100%, with 100% as the future slider key point/magnet.

This intentionally permits shrinking only. It avoids increasing the host viewport requirement and remains safe when outer weight is independently increased.

### Pre-commit 审查 / review

The candidate was reviewed before branch update:
- `COMBINED_SCALE_MIN = 0.75`, `MAX = DEFAULT = 1.00`;
- one `resolveCanvasTransform()` owns effective scale and centered offsets;
- all ten runtime geometry paths use the same helper;
- raw `min(width / CANONICAL_SIZE, height / CANONICAL_SIZE)` calculation remains only inside that helper;
- steady draw, top-overflow calculation, transition drawing/specs, airplane/no-SIM bounds, mobile-type current bounds, and Battery Number current bounds therefore cannot diverge;
- default 100% preserves Build-557 geometry exactly;
- no new View size, LayoutParams, writer, listener, animator, or transition clock is introduced;
- range constants are sourced from settings schema rather than duplicated in Painter.

### Tests

Settings normalization now locks:
- 100% is both default and maximum;
- values above max clamp to 100%;
- values below the supported range clamp to 75%.

### Validation

Run exact-head Runtime CI. No device gate yet because no UI exposes the new setting and the default leaves runtime output unchanged.


### Build 558 CI correction

Runtime CI #2075 failed at Kotlin compilation because two functions retained an obsolete local `NativeRenderTransform(...)` construction after being migrated to `resolveCanvasTransform()`, producing duplicate `nativeTransform` declarations.

Pre-commit review of the correction confirmed:
- both duplicate constructions are removed;
- every function using `resolveCanvasTransform()` now has at most one local `nativeTransform`;
- the raw canonical scale calculation still exists only inside `resolveCanvasTransform()`;
- helper call count and all Build-558 scale semantics remain unchanged.

This correction is compile-only. Build ID, 75%-100% range, default/max 100%, transition geometry, and runtime behavior are unchanged.


### Build 558 CI correction 2

Runtime CI #2076 exposed one remaining compile-only residue in `transitionBatteryNumberCurrentBounds()`: after the duplicate local transform was removed, the returned bounds still referenced deleted local `offsetX/offsetY` names.

Pre-commit review of the correction confirmed:
- the function owns exactly one `nativeTransform`;
- all four returned bound coordinates use `nativeTransform.offsetX/offsetY` directly;
- raw canonical scale calculation remains only inside `resolveCanvasTransform()`;
- helper call count and all Build-558 scale semantics remain unchanged.

No runtime behavior, range, transition timing, or visual default changed.


## 2026-10-01 — Build 559 per-mode battery color sources and Recommended preset

**Type:** color settings schema / runtime policy foundation  
**Display version:** 0.0.3  
**Build:** 559 / `20261001-559`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### User-facing model

Palette order/naming for the upcoming UI:
1. Recommended
2. HyperOS
3. iOS

Recommended is the default for new installs and after a full feature reset. HyperOS keeps the persisted value `hyperos_native` for backward compatibility; only the UI label changes.

Each battery semantic slot independently selects one source mode:
- preset color;
- follow system tint/inversion;
- custom color.

The global palette therefore supplies defaults only for slots currently using “preset color”; it never locks the whole color set.

### Recommended palette candidate

Initial muted status-bar candidate:
- Power save: `#D5A623`
- Performance: `#4A7FC1`
- Super power save: `#D8752C`
- Charging: `#3FA760`
- Low battery: `#D64A4A`
- Normal: follow status-icon tint

These values are intentionally less luminous than the iOS semantic set and are not treated as final until the color BottomSheet/preview receives optical and device review.

### Backward compatibility

- Existing installs without an explicit palette are detected through the pre-existing visual schema marker. The shared `readCombinedStatusVisualSettings()` fallback resolves them as HyperOS immediately, and the App-side one-time migration persists that choice when the repository initializes. SystemUI therefore cannot transiently switch an old install to Recommended merely because it starts first.
- Fresh installs have no previous visual schema marker and default to Recommended.
- Existing `hyperos_native` persisted values map directly to the renamed HyperOS enum member.
- Legacy custom colors that predate per-slot mode keys infer `CUSTOM` automatically.
- A stored custom color remains persisted when a slot switches to Preset or Follow System; it becomes active again if the slot later returns to Custom.
- Custom mode without a valid stored color falls back to that slot’s current preset source.

### Pre-commit 审查 / review

- Palette selection remains global; source mode remains per semantic slot.
- Runtime resolution order is explicit: per-slot mode -> selected palette/custom/system source -> existing visibility fallback.
- Follow System always resolves to the current status-icon tint and therefore retains native black/white inversion behavior.
- HyperOS preset still delegates to SystemUI semantic colors instead of duplicating fixed hex values.
- Recommended/iOS Normal remain monochrome by following status-icon tint.
- Mode keys are included in the visual runtime-key set, so App and SystemUI hot updates use the existing single visual-settings bridge.
- No second battery observer, color owner, listener, or writer is introduced.

### Tests

Coverage added/updated for:
- Recommended as the new default;
- old-install missing-preset migration to HyperOS;
- fresh-install missing-preset default to Recommended;
- legacy stored custom color -> Custom mode inference;
- Recommended semantic values;
- per-slot Follow System overriding an iOS preset;
- stored custom color ignored while slot mode is Preset;
- stored custom color used again when slot mode is Custom;
- all new mode keys participating in runtime synchronization.

### Validation

Run exact-head Runtime CI before any BottomSheet/UI work is committed.


### Build 559 CI correction — legacy color-policy expectations

Runtime CI #2078 compiled the new palette/mode model but exposed four existing `CombinedStatusColorPolicyTest` cases whose expectations still assumed the old global default was HyperOS.

Pre-commit review separated test intent instead of blindly replacing expected colors:
- the native semantic-color test now explicitly selects the HyperOS preset;
- the default performance-mode test now validates the Recommended performance color;
- the optional center/mobile follow test explicitly selects HyperOS so it continues to test propagation of the final battery color rather than palette choice;
- the battery-text / charging-icon independent tint test explicitly selects HyperOS so it continues to isolate its intended follow-system behavior.

Runtime production code is unchanged. This is a test-contract correction for the intentional default-palette change introduced by Build 559.


### Build 559 validation closure

Exact-head Runtime CI #2079 (run `36880365780`) completed successfully on `e37d516`.
- unit tests passed after old HyperOS-default expectations were separated from new Recommended-default behavior;
- debug APK build succeeded;
- pinned HyperOS target verification passed;
- modern Xposed metadata verification passed.

Build 559 color-source foundation is closed. No device gate is required before UI exposure because existing installs remain on HyperOS unless the user explicitly changes the palette, while fresh/reset defaults are not user-visible until the settings UI is completed.


## 2026-10-01 — Build 560 MIUIX feature-page size controls and reset card

**Type:** settings UI / feature-page organization  
**Display version:** 0.0.3  
**Build:** 560 / `20261001-560`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Scope

Keep the feature page as the primary settings surface, grouped by the existing card structure instead of turning it into a navigation-only page.

Cards:
1. Global
2. Network
3. Battery
4. Management

### New direct controls

Global:
- Overall size: 75%-100%, 5% steps, default/max 100%;
- Outer weight: 70%-130%, 5% steps, default 100%; this is the existing coupled ring + four-dot + unavailable-mark family.

Network:
- Wi-Fi size: 80%-125%, 5% steps, default 100%;
- Mobile type size: 80%-125%, 5% steps, default 100%;
- Mobile type weight: 500-950, 50-weight steps, default 800.

All five controls use MIUIX `SliderPreference`, `showKeyPoints = true`, a single default `keyPoints` value, and the existing magnetic snap threshold. No custom slider or gesture implementation is introduced.

### Restore defaults

A Management card adds “Restore defaults”.
- It is intentionally available even when the feature master switch is off.
- Confirmation uses the existing MIUIX `OverlayDialog`.
- Confirming resets both `CombinedStatusFeatureSettingsRepository` and `CombinedStatusVisualSettingsRepository`.
- Feature defaults restore the master feature to enabled and lock-screen combined status to disabled.
- Visual defaults restore the active schema defaults, including Recommended palette and 100% geometry defaults.

### Copy review

Chinese and English copy was shortened and normalized during the same UI pass:
- layout summary is reduced to the memory behavior;
- battery readout summary focuses on percentage + automatic avoidance;
- charging summary removes redundant phrasing;
- lock-screen summary removes repeated “combined icon” wording;
- network color-follow summaries use consistent terminology;
- new controls use concise titles such as “Overall size / 整体大小” and “Mobile type weight / 移动制式字重”.

### Pre-commit 审查 / review

- Existing MIUIX Card / SmallTitle spacing is reused; no custom card style is added.
- `HubPage` gains only an optional fourth section, so Settings and other existing three-section callers remain unchanged.
- New controls bind directly to the existing single visual-settings repository; no additional state owner is introduced.
- Slider ranges and default key points come from the same schema constants consumed by runtime.
- Restore is the only control intentionally not gated by `featureSettings.enabled`.
- Existing battery-number/charging detailed sliders remain on the page in this checkpoint; they are not prematurely moved to drawers before final density review.
- No runtime drawing, transition, or SystemUI hook behavior changes in Build 560.

### Validation

Run exact-head Runtime CI to compile the new MIUIX calls/resources and lock repository wiring. Device review is deferred until the color BottomSheet and final feature-page density pass are complete.


### Build 560 validation closure

Exact-head Runtime CI #2081 (run `36881730709`) completed successfully on `c857759`.
- all unit tests passed;
- MIUIX feature-page controls/resources compiled successfully;
- debug APK build succeeded;
- pinned HyperOS target verification and modern Xposed metadata checks passed.

Build 560 is closed. The next change is isolated to battery-color BottomSheet UI and will use a separate, descriptive commit.


## 2026-10-01 — Build 561 MIUIX battery-color BottomSheet

**Type:** settings UI / battery color overview  
**Display version:** 0.0.3  
**Build:** 561 / `20261001-561`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Scope

Expose the Build-559 color-source model through a native MIUIX BottomSheet without introducing a second battery-state authority.

The Battery card gains one concise entry:
- title: Battery colors / 电量颜色;
- summary: current palette;
- end area: five semantic preview dots for power save, performance, super power save, charging, and low battery.

### Preview semantics

The App process does not own HyperOS battery semantic state; the authoritative source remains SystemUI `MiuiBatteryMeterIconView.getProgressStatus()`.

Therefore:
- Recommended/iOS fixed palette colors render as filled preview dots;
- stored Custom colors render as filled preview dots;
- HyperOS preset colors render as outlined/dynamic dots because the actual semantic value comes from SystemUI at runtime;
- Follow System also renders as outlined/dynamic;
- Custom mode without a stored custom color follows the runtime policy and previews its preset fallback;
- Normal remains dynamic for Recommended/iOS because it follows status-icon tint.

This deliberately does not add PowerManager/BatteryManager inference or another cross-process writer merely to fake a “current mode” preview.

### BottomSheet interaction

A single MIUIX `OverlayBottomSheet` is used as a state machine:
- overview: Recommended / HyperOS / iOS palette selection plus all semantic slots;
- slot detail: Scheme color / Follow system / Custom source selection;
- backing out of slot detail returns to the overview rather than stacking another sheet.

MIUIX `RadioButtonPreference`, `ArrowPreference`, `Card`, and `SmallTitle` are reused. No custom drawer implementation is introduced.

### Pre-commit 审查 / review

- Only one `OverlayBottomSheet` exists in the new color UI.
- Palette selection order is Recommended -> HyperOS -> iOS.
- Slot source writes use the existing `CombinedStatusVisualSettingsRepository.setBatteryColorMode`.
- Palette writes use the existing `setBatteryColorPreset`.
- The existing feature reset dialog remains independent from the color sheet.
- UI copy is bilingual and concise.
- No SystemUI runtime/hook/transition code changes.
- No new state observer, listener, or cross-process writer.

### Tests

Pure preview-resolution tests cover:
- Recommended charging and iOS low-battery fixed colors;
- HyperOS and Follow System remain dynamic;
- Custom uses its stored color;
- Custom without a stored color falls back to the selected preset;
- Recommended Normal remains dynamic/status-tint based.

### Deferred to next isolated change

Custom color editing UI:
- common colors;
- MIUIX ColorPicker / ColorPalette;
- RGB and HEX input;
- per-mode reset-to-default.

The source mode is already persisted in Build 561, but no incomplete custom editor is represented as finished.

### Validation

Run exact-head Runtime CI before adding the custom color editor.


### Build 561 validation closure

Exact-head Runtime CI #2083 (run `36884110591`) completed successfully on `b0ba53f`.
- new MIUIX BottomSheet UI compiled successfully;
- all preview-resolution tests passed;
- debug APK build succeeded;
- pinned HyperOS target and modern Xposed metadata verification passed.

Build 561 is closed. Custom color editing remains isolated to the next commit.


## 2026-10-02 — Build 563 scale-aware compact reservation and Mobile Type weight range

**Type:** runtime geometry / settings correction  
**Display version:** 0.0.3  
**Build:** 563 / `20261001-563`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device evidence and root cause

Build-562 device feedback identified two independent issues:
- Mobile Type weight still exposed the old 500-950 / 800 contract rather than the requested 400-1400 / 900 midpoint.
- Overall size scaled only Guiyuan painter pixels. The native replacement reservation remained the full stable Battery carrier width, so neighboring HyperOS status icons could not close the visual gap.

The Build-562 diagnostic confirms `compactSlotWidth=105` remained unchanged while the renderer accepted live visual settings. The correction therefore belongs to the existing reservation geometry, not to a new spacing offset.

### Implementation

- Mobile Type weight: 400-1400, 50-weight slider intervals, default/key point 900.
- Add one centered-scale reservation rule: because painter shrink is centered in the stable Battery carrier, peer reservation ends at the scaled visual's leading edge while retaining the transparent end-side inset.
- Reuse that rule for Home/Keyguard/Control Center native padding and the transition reservation/latent-reveal compact baseline.
- Visual preference changes ask the existing `SystemUiHomePresentationOwner` to resync its reservation; no second padding/translation writer is added.

### Review

- Geometry is derived from the same base carrier width + user scale; no device-specific px compensation.
- `paddingEnd` remains single-writer owned by the existing presentation session.
- Scale remains shrink-only and the painter remains the sole Guiyuan pixel owner.
- Transition target geometry, HyperOS island width authority, animation clocks, and native peer motion are unchanged.
- Failure paths remain native because unavailable carrier/layout inputs still abort the existing reservation path.

### Validation

Automated validation is expected to be Full while #181 still includes the independently reviewed CI run-title delta. No work-branch Canary is requested by this change alone; device evidence is deferred until the color-UI/runtime palette work is grouped into one focused checkpoint.

## 2026-10-01 — Build 562 MIUIX custom battery color editor

**Type:** settings UI / custom battery colors  
**Display version:** 0.0.3  
**Build:** 562 / `20261001-562`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Scope

Complete the Custom source path introduced by Build 559/561 without adding another screen or another BottomSheet instance.

The existing single BottomSheet now has a third internal state:
1. palette/mode overview;
2. per-mode source selection;
3. per-mode custom color editor.

### Custom editor

The editor provides:
- 10 common color shortcuts;
- full opaque HSV adjustment using MIUIX `HsvHueSlider`, `HsvSaturationSlider`, and `HsvValueSlider`;
- exact six-digit HEX input;
- exact RGB input;
- current-color preview;
- per-mode restore-default action.

The built-in MIUIX `ColorPalette` / `ColorPicker` components were reviewed but intentionally not used because MIUIX 0.9.4 always exposes an alpha slider while Guiyuan persists battery semantic colors as opaque. Showing a control whose result is discarded would violate the UI/runtime contract.

### Common colors

Ten compact shortcuts cover red, orange, yellow, green, cyan, blue, indigo, purple, pink, and neutral gray. They are shortcuts only, not a fourth named palette.

### Persistence

- Picking/editing a custom color guarantees the slot is in `CUSTOM` mode and writes the opaque ARGB value through the existing visual-settings repository.
- `resetBatteryColorSlot(slot)` removes that slot’s source-mode key and override in one SharedPreferences editor transaction.
- Resetting a slot therefore returns it to `PRESET` mode using the currently selected Recommended / HyperOS / iOS scheme.
- No global palette or other slot is changed.

### Input rules

- HEX accepts exactly six hexadecimal digits (optional leading `#`) and forces alpha to FF.
- RGB accepts integer channels 0-255.
- Invalid or incomplete input does not mutate the persisted color.
- For an unset custom color, editor initialization prefers: stored override -> selected fixed palette color -> current MIUIX foreground only as a local editing seed for dynamic SystemUI colors. This seed is not persisted until the user changes a value.

### Pre-commit 审查 / review

- No alpha control is exposed.
- No Material color picker or text field is introduced.
- The BottomSheet instance count remains one.
- The new editor uses only existing repository ownership.
- Per-mode reset is atomic.
- Common colors are UI shortcuts, not persisted as a separate scheme.
- No SystemUI hook, transition, or battery-state ownership changes.

### Tests

Battery color UI tests now also cover:
- valid/invalid six-digit HEX parsing;
- RGB 0-255 bounds;
- RGB split/round-trip;
- editor initial-color precedence and dynamic fallback opacity.

### Validation

Run exact-head Runtime CI before any device review.


### Build 562 CI correction — final reviewed editor candidate

Runtime CI #2085 (run `36885816625`) used an earlier editor candidate and failed Kotlin compilation at `BatteryColorControls.kt` because of an explicit `androidx.compose.foundation.layout.weight` import. In this Compose version that import resolves to an internal parent-data property, while `Modifier.weight()` is already available from the RowScope used by the existing project UI.

The correction:
- removes the explicit `weight` import only; layout behavior is unchanged;
- restores the later reviewed interaction where opening the Custom editor does not immediately write `CUSTOM`;
- writes `CUSTOM + color` only after a valid common-color / HSV / HEX / RGB edit;
- uses the Recommended color for the same semantic slot as the editor start when the selected source is dynamic, falling back to current foreground only where no semantic fixed color exists;
- keeps the per-mode atomic reset and opaque-only color contract;
- expands pure UI logic tests for HEX/RGB parsing, RGB round-trip, and editor initial-color priority.

The PR display title process was also verified: CI #2085 displayed `feat: add MIUIX custom battery color editor`, confirming that updating the PR title before the work-branch HEAD update makes the Actions list describe the concrete Build objective without changing workflow trigger/security semantics.

No SystemUI runtime, hook, transition, or rendering behavior changed in this correction.


### Build 562 CI correction 2 — remove duplicate editor tests

Runtime CI #2090 (run `36887864769`) compiled the production app successfully. Unit-test compilation then failed because iterative review had appended a second set of tests covering the same HEX/RGB parsing and editor initial-color priority, including a duplicate function named `editorInitialColorPrefersStoredThenPresetThenDynamicFallback`.

Correction:
- remove the later duplicate parser / initial-color / RGB round-trip block;
- keep the original seven focused tests;
- confirm there are no duplicate test function names;
- production code is unchanged.

The workflow/run-name cleanup is intentionally deferred until after the next Canary is delivered for device testing.


## 2026-10-02 — CI run-title clarity

**Type:** CI presentation only

GitHub PR-triggered workflow runs previously inherited the pull-request title because the workflows did not define `run-name`. This made unrelated commits appear under the same Actions title.

Change:
- Build PR runs now show run number + PR number + work branch + exact PR HEAD SHA.
- Build push runs show run number + branch + push head commit message.
- Manual Build runs show run number + branch.
- Comment-triggered Canary runs show Canary run number + PR number.
- Manual Canary runs show Canary run number + requested source branch.

Review:
- workflow names remain `Build` and `Work Branch Canary`;
- job ids/names remain `build` and `canary`;
- no permissions, triggers, validation scope, signing, artifact, concurrency, or required-check behavior changed.


### CI run-title correction — restore PR-title-driven Actions labels

The later `run-name` experiment did not satisfy the intended per-change label contract for pull-request builds. On `pull_request` events, the top-level `run-name` expression has the PR metadata and head SHA but not the checked-out head commit message, so the resulting titles repeated the PR number / branch / SHA pattern and obscured the actual change summary.

This also left `.github/workflows/build.yml` and `.github/workflows/work-branch-canary.yml` in the runtime PR diff, forcing Full / mixed-surface classification for unrelated runtime commits.

Correction:
- restore both workflow files exactly to the current `dev` versions, removing only the experimental `run-name` additions;
- preserve all triggers, permissions, job ids, validation routing, signing, artifact, and concurrency behavior;
- return to the already verified process: update PR #181 title to the concise commit/change summary before moving the work-branch HEAD, so the default PR-triggered Actions display title is the desired `feat:/fix:/test: short summary`;
- existing workflow runs keep their historical titles and are not renamed retroactively.

This correction is CI presentation/branch hygiene only and does not affect the APK or runtime behavior.
