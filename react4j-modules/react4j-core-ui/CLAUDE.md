# react4j-core-ui

The browser client of React4J (`org.omnaest.react4j`): the TypeScript/React renderer
(`src/main/react/src/renderer`) and the `Backend`/`InFlightTracker` wrapper around Axios
(`src/main/react/src/backend`) that talks to `react4j-core`'s `/ui`, `/ui/event` and `/ui/upload*`
endpoints. Built as a CRA bundle and packaged into `react4j-core`'s static resources by the Maven
build.

## Build / test

From `src/main/react`:

```cmd
npm test    -- react-scripts test (Jest + React Testing Library)
npm run build -- react-scripts build (also the only real TypeScript compile/type-check in this module)
```

`npm test` in CI mode (`CI=true` env var, or `--watchAll=false`) runs once and exits; interactively it
watches. See the in-repo comment on `react4j-core-ui-jest-use-npm-test-and-run-build-for-tsc` (Jest
memory) for why automocking `Backend`/axios needs an explicit factory mock rather than
`jest.mock(path)` with no factory - axios's real ESM build is not transformable by CRA's default Jest
config.

The Bootstrap stylesheets are **not** in the bundle any more: the `prebuild` npm script (`scripts/build-theme.js`, Sass
1.105) compiles `src/theme/react4j-modern.scss` into `public/css/theme/` (modern plus a stock `bootstrap.min.css`), and
`react4j-core`'s `IndexHtmlController` links one of them (see "Theme" in its CLAUDE.md). `src/main/react/.npmrc` sets
`legacy-peer-deps=true` because `npm install` otherwise aborts with ERESOLVE (react-scripts 5 peers TypeScript 3/4, the
project uses 5); the Maven `npm install` execution relies on it.

The same `prebuild` script also builds `public/css/theme/react4j-tabler.css` (plan-277): Tabler 1.6.1 (`@tabler/core`, exact pin in
`package.json`, MIT) compiled from its **Sass sources only** through the wrapper `src/theme/react4j-tabler.scss` (which also holds the few
React4J adjustments, each with the defect it fixes), then PostCSS renames the custom properties from `--tblr-*` to `--bs-*`
(`postcss-prefix-custom-properties`), autoprefixer runs, and a banner with the MIT notices is prepended. This pipeline is separate from the
modern one, whose output (`react4j-modern.css`, `bootstrap.min.css`) must stay byte-identical. The build **fails** (non-zero exit, no CSS) on:
a Sass source loaded from outside `@tabler/core/scss` and `src/theme` (`assertTablerSourcesOnly`, keeps `dist/libs` and its non-MIT plugins
out); forbidden text in the output (`fonts.googleapis`, `fonts.gstatic`, `apexcharts`, `dist/libs`) or a custom property outside `--bs-`
(`assertTablerOutput`); a Tabler package not declaring MIT. The licences are reproduced in the repository's `THIRD-PARTY-NOTICES.md`, which the
`copy-third-party-notices` execution of this module's pom copies into the jar as `META-INF/THIRD-PARTY-NOTICES.md`. To update Tabler see the
README of the repository ("How to update Tabler"). `public/css/custom.css` is loaded AFTER the theme sheet, so its `.card-body { position:
static }` (equal to Bootstrap's default, needed because Tabler makes `.card-body` `position: relative`) wins on every preset. The same sheet holds .card-body:has(.dropdown-menu.show) { overflow: visible } (plan-277 F1), which lets an open dropdown menu leave its card on every preset.

## Font Awesome Icon stylesheet (plan-277 section 8, kanban bcfd7e44)

`Icon.tsx` emits `<i class="fas fa-<key>">`, which only renders with Font Awesome's own stylesheet. `public/css/fontawesome-free-all.min.css` is the file
`css/all.min.css` of `@fortawesome/fontawesome-free@5.13.0`, byte-identical (SHA-256 `876D023D...D84`, licence and hashes in the repository's
`THIRD-PARTY-NOTICES.md`), linked from `public/index.html` ahead of `color.css`/`custom.css`. It sits directly in `public/css/` so that its
`url(../webfonts/...)` resolves to the shipped `public/webfonts/` (a sub-directory would not resolve). It defines class selectors only (`.fa*`, `.sr-only`,
`.sr-only-focusable`, `:root .fa-flip-*`/`.fa-rotate-*`), `@font-face` and the `fa-spin` keyframes: no element selector and no custom property. Do not edit
it; update it by replacing it with the file of the new package version. Guard: `ShowcaseCoverageChecks.testEveryStandardIconRendersARealGlyphFromTheFontAwesomeFace`
(the face is `loaded`, every `StandardIcon`'s `::before` is a private-use glyph in `Font Awesome 5 Free` with a non-zero rendered width); without the link
the page has no such face at all. Only the stylesheet and the 15 `public/webfonts/` files of Font Awesome ship: the package's unused `svgs/` icon files (1,591) were
removed in plan-277 section 9 (nothing referenced them; the three `fa-*-*.svg` files in `webfonts/` are SVG web fonts, kept).

## File upload: two client-side transports

`FileUpload.tsx` renders one `<input type=file>` regardless of which transport the server rendered for
this element - the element's `FileUploadFormNode.uploadUrl` and `.unbufferedTransport` (mirroring the
server's `FormFileUploadNode`, see `react4j-core-components/CLAUDE.md`) decide which `Backend` method
is called on file selection. See `react4j-core/CLAUDE.md` for the full server-side mechanism and
`react4j-core-components/CLAUDE.md` for the node/builder contract; this file records what the CLIENT
must get right, because both traps below are silent when missed - no compile error, no obvious runtime
error, just a corrupted or (worse) unprotected upload.

- **`unbufferedTransport` absent/false -> `Backend.uploadFile(...)`.** Unchanged: builds a `FormData`,
  posts `multipart/form-data` to `uploadUrl` (`ui/upload`).
- **`unbufferedTransport === true` -> `Backend.uploadFileRaw(...)`.** Posts the raw `File` itself as
  the request body - no `FormData`, no wrapper, no base64 - to `uploadUrl` (`ui/upload/raw`), with
  `X-Upload-Id` and `X-Filename` as headers and the file's own media type as `Content-Type`. Both
  methods increment/decrement the shared `InFlightTracker` around the request, in `finally`, so a
  failed round trip still settles the page-level busy indicator - preserve that if either method is
  ever touched again.

### Trap (a): `Content-Type` must never end up as a form content type

A `File.type` reported by the browser is routinely **empty** for an unrecognised extension. Left
unset, the HTTP client (or the server it talks to) is free to fall back to a default - and
`application/x-www-form-urlencoded` is exactly the one content type Tomcat parses into request
*parameters* even though it never runs its multipart resolver. Any filter or interceptor that reads a
request parameter would then consume the raw body before `FileUploadController#uploadFileRaw` ever
sees it, silently defeating the whole point of the unbuffered transport - the failure is invisible:
no exception, no log line, just an empty/corrupt upload. `Backend.uploadFileRaw` closes this by always
sending an explicit `Content-Type`: `file.type || "application/octet-stream"`, never left to a
default. Pinned by `Backend.uploadFileRaw.test.ts` (empty-`file.type` case).

### Trap (b): percent-encoding and `URLDecoder` disagree about `+`

`FileUploadController` decodes `X-Filename` with Java's `URLDecoder.decode(value, UTF_8)`, which turns
a **literal `+` into a space** - that is `application/x-www-form-urlencoded` decoding semantics, not
generic percent-decoding. `encodeURIComponent` is compatible: it escapes `+` to `%2B` and a space to
`%20`, so both round-trip correctly. **`encodeURI`, or sending the filename raw, is NOT compatible** -
either would leave a literal `+` in the header, and the server would silently turn it into a space,
corrupting any filename containing one. `Backend.uploadFileRaw` always encodes with
`encodeURIComponent(file.name)`. Pinned by `Backend.uploadFileRaw.test.ts` over two filenames: one
containing a space, one containing a literal `+`.

## Known defect classes

Recorded by the orchestrator, keyed by the property that was violated, not the symptom (full-stack-engineer step 13c). Look up a new
defect here by its property before diagnosing it.

| Class (property) | Occurrences | Guard | Status |
|---|---|---|---|
| An absolutely positioned overlay (dropdown menu, toast container) must not be clipped by an ancestor's `overflow`. React4J's global `custom.css` gives `.card-body` `overflow-x:auto` | plan-277 T4: dropdown menus clipped on MODERN (pre-existing) and TABLER; toast clipped on TABLER once Tabler made `.card-body` `position:relative` | Both presets, one source each: `custom.css` rule `.card-body:has(.dropdown-menu.show) { overflow: visible }` (plan-277 F1, replaces the Tabler-only copy in `react4j-tabler.scss`) and `custom.css` `.card-body { position: static }` (toast). Pinned by `ModernThemeIT` and `TablerThemeIT` dropdown tests (standalone and SplitButton, centre of the last item AND every edge midpoint of the menu), `TablerThemeIT.testToastIsNotClippedByItsCard` and `ModernShowcaseCoverageIT` / `TablerShowcaseCoverageIT.testNavDropdown...` (NAV presentation) and `...testNavigationBarDropdownOpensUnclippedAndItsItemNavigatesToItsLocator` (the dropdown inside the expanded NavigationBar, which sits in `.body-top` with `overflow-y: hidden`: `NavigationBar.tsx` gives its menu popper's `strategy: "fixed"`, and with that one prop removed both presets fail on the last item and the edge midpoints), all through `OverlayProbes`. Shown red: with the custom.css rule absent the MODERN and TABLER dropdown tests fail, and on committed code the MODERN ones failed | guarded on both presets (needs `:has()`, which a browser without it lacks and then keeps the old clipping) |
| A visibility/clipping assertion must sample the overlay's edges, not only its centre: a centre probe stays green while the edges are cut | plan-277 T4 gap G1 (`testToastIsNotClippedByItsCard` stayed green without its fix); workspace class testing P13 (tests unable to fail), also guarded in plan-274 | Lifted (plan-277 F1) into the shared `OverlayProbes.assertEveryEdgeMidpointReachable` (package `browser.theme` of `react4j-ui-test`); users: the toast check, every dropdown menu check (`ThemeBrowserSupport.assertShowcase...MenuIsVisibleAndUnclipped`, `ModernThemeIT`, `TablerThemeIT`) the NAV dropdown check and the NavigationBar dropdown check (`ShowcaseCoverageChecks.testNavigationBarDropdownOpensUnclippedAndItsItemNavigatesToItsLocator`, plan-277 section 8). Shown to bite alone: with the item-centre probe removed the MODERN dropdown tests still fail on the edge midpoints | guarded |
| An absolutely positioned overlay (the collapsed-menu hamburger, `.navbar-menu-icon-container`) must not cover in-flow content: the space reserved for it must be at least as large as the overlay. `HomePage.tsx` plus `custom.css` position it `absolute; top:10px; left:0` (56px tall under MODERN, 32px under TABLER) while the body starts after a 30px spacer, so the first element of every app with a NavigationBar and left-aligned content sits under it | plan-277 F3 measurement at viewport 1280x900, "Component Showcase" `h2` (0,30)-(1280,68.4) MODERN / (0,30)-(1280,58) TABLER: container (0,10)-(56,66) MODERN / (0,10)-(32,66) TABLER, overlap with the `h2` 56x36px MODERN / 32x28px TABLER (the `.navbar-toggler` alone: 56x28 / 32x16) | Fixed (plan-277 section 8, kanban 66fb08de): `HomePage.tsx` wraps the collapsed state in `.body-with-menu-icon` (a flex column) and `custom.css` puts `.navbar-menu-icon-container` IN FLOW above the scrolling `.body-full` (no absolute position, no fixed spacer), so the reserved space is the container's own box on every preset; the expanded state is wrapped in `.body-with-navbar { display: contents }` and lays out as before, and an app without a NavigationBar renders as before. Pinned by `ModernShowcaseCoverageIT` / `TablerShowcaseCoverageIT.testHamburgerDoesNotCoverTheFirstBodyElementAtDesktopWidth` and `...AtPhoneWidth` (box arithmetic on `getBoundingClientRect` at 1280x900 and 400x900: heading vs container and toggler). Shown red on the old CSS (heading (0,30)-(1280,68.4) vs container (0,10)-(56,66) MODERN, (0,30)-(1280,58) vs (0,10)-(32,66) TABLER), green after (heading starts at y=66, container (0,0)-(56,66) MODERN / (0,0)-(32,66) TABLER) | fixed on both presets |