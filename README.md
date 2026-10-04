# React4J
Java based UI component framework that integrates and abstracts React as UI Layer and the REST intercommunication between UI and backend

## Themes

React4J ships two built-in looks, called presets. Both are Bootstrap based and are selected from Java; the default is `MODERN`.

| Preset | Look | Stylesheet (served from the `react4j-core-ui` jar) |
|---|---|---|
| `MODERN` (default) | React4J's own light and dark theme | `/css/theme/react4j-modern.css` |
| `TABLER` | [Tabler](https://tabler.io) 1.6.1 (MIT), built from its Sass sources | `/css/theme/react4j-tabler.css` |

### Choosing a preset

```java
reactUI.configureTheme(theme -> theme.preset(ThemeConfiguration.ThemePreset.TABLER)
                                     .colorMode(ThemeConfiguration.ColorMode.DARK)   // optional, LIGHT is the default
                                     .primaryColor("#0f766e"));                      // optional design tokens, see below
```

An application that makes no `preset` call gets `MODERN`, and its output is unchanged.

- `disable()` switches React4J's theming off and links the stock Bootstrap stylesheet instead. The chosen preset is kept but ignored while disabled.
- `useDefault()` switches theming on again **and selects `MODERN`**. To get Tabler back after `disable()`, call `useDefault().preset(TABLER)`.
- `preset(...)` never changes whether theming is enabled. `colorMode(...)` and `addStylesheet(...)` work on both presets.

### Design tokens per preset

The token methods (`primaryColor`, `secondaryColor`, `successColor`, `infoColor`, `warningColor`, `dangerColor`, `fontFamily`, `baseFontSize`, `borderRadius`, `shadowStrength`) are accepted by both presets, and each preset applies them its own way:

| | `MODERN` | `TABLER` |
|---|---|---|
| What is written | Java-derived values for the component variables (hover, active and subtle shades are computed in Java and re-emitted as component rules) | Base custom properties only (`--bs-<role>`, `--bs-<role>-rgb`, radius, font, shadow colour), in one rule after the Tabler sheet; never a component rule |
| Shades (hover, active, subtle, focus ring) | Derived in Java | Derived in the browser by Tabler itself (`color-mix()`, relative colours) |
| `-fg` (text on a solid role colour) | Derived | **Not derived**: stays Tabler's light text, so choose role colours dark enough for light text |
| `borderRadius` | Scaled with the modern ratios | Becomes Tabler's base radius, the other sizes scale with Tabler's own ratios |
| `shadowStrength` | The modern shadows | Scales the alpha of Tabler's one shadow colour; `STRONG` equals `DEFAULT` in dark mode, `DEFAULT` alone writes nothing |

The full per-token contract is the Javadoc of `ThemeConfiguration`; the internals are in `react4j-modules/react4j-core/CLAUDE.md` (section "Theme").

### Browser floor and size

| | Browser floor | Size (loaded only when the preset is chosen) |
|---|---|---|
| `MODERN` | the `browserslist` of `react4j-core-ui` (`>0.2%, not dead, not op_mini all`) | about 234 KB |
| `TABLER` | Chrome 123+, Firefox 128+, Safari 17.5+ (Tabler uses `oklch()`, `color-mix()` and relative colour syntax) | about 633 KB, about 80 KB gzipped |

### What of Tabler is deliberately not used

Only Tabler's Sass sources are used. Nothing from `@tabler/core/dist` (including `dist/libs`, whose bundled plugins such as ApexCharts have their own licences) is used or shipped, no Tabler JavaScript is shipped, and no font is loaded from Google Fonts. The theme build in `react4j-modules/react4j-core-ui/src/main/react/scripts/build-theme.js` fails when a Sass source outside `@tabler/core/scss` is loaded, when the output mentions Google Fonts, ApexCharts or `dist/libs`, or when a custom property lies outside the `--bs-` namespace. The licences of Bootstrap and Tabler are reproduced in [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md), which is also inside the `react4j-core-ui` jar as `META-INF/THIRD-PARTY-NOTICES.md`.

### Screenshots

`docs/theme-screenshots/` holds the showcase under each theme (`showcase-modern.png`, `showcase-modern-dark.png`, `showcase-disabled.png`). `docs/theme-screenshots/tabler/` holds MODERN and TABLER side by side (`showcase-modern-vs-tabler.png`), the same showcase on `TABLER` in light and dark mode, and the MODERN capture the comparison was made from. They are written by the `ShowcaseScreenshot*IT` tests of `react4j-ui-test`.

### How to update Tabler

1. Bump the exact pin of `@tabler/core` in `react4j-modules/react4j-core-ui/src/main/react/package.json`, then run `npm install` there.
2. Rebuild `react4j-core-ui` (`mvn install`, or `npm run build`). The build guards above must pass; a failing guard names the offending source, string or custom property.
3. Run `TablerTokenCssSheetGuardTest` in `react4j-core` (every variable the Java token writer can emit is still read by the sheet, the selector and the radius, shadow and colour constants still match the compiled sheet).
4. Run, in `react4j-ui-test`, `TablerThemeIT` and `TablerTokenThemeIT`, and the whole suite on Tabler: `mvn verify "-DexcludedGroups=" "-Dspring.profiles.active=theme-tabler" "-Dit.test=*IT,!ShowcaseScreenshotModernIT"`. `ModernThemeIT` is expected to fail in that run (it asserts the modern sheet); `ShowcaseScreenshotModernIT` is excluded because it has no profile of its own and would write a Tabler page over the committed MODERN screenshot. Then run `mvn verify "-DexcludedGroups="` without a profile; it must be green.
5. Regenerate and review `docs/theme-screenshots/tabler/` (written by the default run), and `git checkout` any MODERN screenshot outside `tabler/` that the run rewrote without a real change.
6. Update the Tabler version in `THIRD-PARTY-NOTICES.md` (and its copyright line, should upstream change it).
