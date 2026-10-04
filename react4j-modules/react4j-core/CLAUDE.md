# react4j-core

The runtime service layer of React4J (`org.omnaest.react4j`): the `/ui` and `/ui/event` endpoints, the
`ReactUIService` rendering pipeline, and the file-upload endpoints (`FileUploadController`,
`FileUploadService`). Depends on `react4j-core-components` (the component/UploadChannel model) and
`react4j-core-ui` (the bundled frontend).

## Build

```cmd
mvn clean install
```

No embedded servlet container is on this module's classpath (it parents `CommonsParent`, not
`CommonsSpringBootParent`) - it is a library, consumed by Spring Boot apps that bring their own
`spring-boot-starter-web`. `@SpringBootTest(webEnvironment = MOCK)` works here (`FileUploadEndToEndTest`);
`webEnvironment = RANDOM_PORT` does not, for want of a real embedded container - see "Real-container upload
tests live in `react4j-ui-test`" below before adding one.

## Theme: server-selected Bootstrap stylesheet (plan-274)

Bootstrap CSS is no longer part of the `react4j-core-ui` JS bundle. `IndexHtmlController` links it into
`index.html` itself, so the server decides between the **modern** theme (a Bootstrap 5.3 build compiled from Sass,
accent `#4f46e5`, radius 8px) and **stock** Bootstrap.

- **API** (`org.omnaest.react4j.domain.configuration.ThemeConfiguration`, reached via
  `ReactUI#configureTheme(Consumer)` or `ReactUIService#configureTheme(Consumer)`, a null consumer is ignored):
  `useDefault()`, `disable()`, `preset(ThemePreset)`, `colorMode(ColorMode.LIGHT|DARK|AUTO)`, `addStylesheet(url)` (null/blank rejected).
  The configuration is application-global, not per root.
- **Default is ON** and needs no call: modern, `LIGHT`, no added sheets. `useDefault()` re-enables after a `disable()`
  and keeps the colour mode and added sheets.
- **Preset** (plan-277 T2): `preset(ThemePreset.MODERN|TABLER)`, `ThemePreset` nested in `ThemeConfiguration` like `ColorMode`,
  null throws `IllegalArgumentException` and leaves the state unchanged. `TABLER` links `/css/theme/react4j-tabler.css`
  (same cache-buster query) INSTEAD of `react4j-modern.css`, so a MODERN-only app is byte-identical. Semantics: `preset(X)`
  never changes `enabled`; `disable()` keeps the preset (like colour mode, sheets and tokens) but while disabled the output is the
  stock link only; `useDefault()` sets `enabled=true` AND `preset=MODERN`, so Tabler after a `disable()` is
  `useDefault().preset(ThemePreset.TABLER)`. `colorMode()` and `addStylesheet()` behave the same on TABLER (same attribute and
  AUTO script, added sheets after the theme link). The design tokens on TABLER are written by `TablerTokenCss`, never by
  `ThemeTokenCss` (see "Design tokens on TABLER" below). **Browser floor of TABLER: Chrome 123+, Firefox 128+, Safari 17.5+**: Tabler derives
  its colour shades in the browser with `color-mix()`, `light-dark()` and `oklch()`, and its `-darken` hover shade with relative colour
  syntax, which Firefox supports from 128. See `.claude/plans/plan-277-react4j-tabler-theme-preset.md`.
- **`disable()` means stock Bootstrap only**, as before the theme existed: the single link
  `/css/theme/bootstrap.min.css`, no `data-bs-theme` attribute (the colour mode is ignored), no added sheets, no
  `<style>` block. `ColorMode.AUTO` emits no server attribute but a tiny inline head script (before the stylesheet
  links) that sets `light`/`dark` on `<html>` from `prefers-color-scheme`.
- **Template placeholders** in the core-ui `public/index.html`, verified in the BUILT copy (CRA's minifier strips
  comments but keeps these): the empty element `<react4j-theme/>` right after `<title>`, and the attribute text
  `data-bs-theme="%BS_THEME%"` on `<html>`. Both are replaced by `IndexHtmlController` (cached 5 s like every other
  placeholder, so a `configureTheme` change shows within that window). A new placeholder must be written exactly as
  the minifier emits it.
- **Design tokens** (S4, additive on `ThemeConfiguration`, each returns the configuration): `primaryColor`,
  `secondaryColor`, `successColor`, `infoColor`, `warningColor`, `dangerColor` (`#rgb` / `#rrggbb` only),
  `fontFamily` (names or quoted names, at most 20 entries and 300 characters), `baseFontSize` and `borderRadius`
  (`<number><px|rem>`, at most four digits before and after the point), `shadowStrength(ShadowStrength.NONE|SUBTLE|DEFAULT|STRONG)`.
  **Validation is the only injection defence and it is by construction**: the setter parses into a typed value
  (`RgbColor`, `CssLength`, `FontFamily`) before touching state, so an invalid value throws `IllegalArgumentException`
  and leaves the configuration unchanged; the renderer only ever prints typed values and constants, never raw text.
  `ThemeHeadRenderer.styleBlock` additionally throws `IllegalStateException` if the CSS contains `<`. Tokens are kept
  by `disable()` and `useDefault()` but only take effect while the theme is enabled. No token set means no `<style>`
  and a head byte-identical to the S3 head.
- **Why per-variant rules**: Bootstrap 5.3.2 compiles component variables as literals, so a root `--bs-primary` does not
  recolour `.btn-primary` or the form focus rings. `ThemeTokenCss` therefore emits, for each set colour, the root and
  dark-mode variables plus the rules of every component that bakes the colour in (buttons, outline buttons, `text-bg`,
  link helpers, table variants, and for the primary the form controls, switches, range, dropdown, nav pills,
  accordion, pagination, progress, list group, close button).
  The Sass colour functions are mirrored in pure Java (`BootstrapColorFunctions` and `BootstrapColorDeriver`, no
  runtime Sass): `mix`/`tint`/`shade`/`shift`, the 10-digit rounded luminance, and `color-contrast` (first of white,
  black above 4.5). Output is Sass-formatted (`CssText`: short hex for integer channels, otherwise `rgb(P%, ...)`).
  **The Java mirrors theme settings that live in `react4j-modern.scss`** (focus ring alpha 0.4 and width 0.25rem, body
  background `#f7f8fa`, the shadow shapes and default alphas, radius ratios 0.75 / 1.5 / 2). Change one in the scss and
  the compiled-sheet equality test goes red until `ThemeTokenCss` follows.
- **Guards, in the order they bite**: (1) `BootstrapColorFunctionsTest` and `BootstrapColorDeriverTest` against
  `src/test/resources/theme/golden/bootstrap-function-golden.json` (21 colours incl. extremes, a yellow contrast flip,
  `#4f46e5`, plus a 256-step grey luminance ramp, produced by Bootstrap's own Sass); (2) `ThemeTokenCssSheetGoldenTest`
  against `theme-sheet-golden.json`, the FULL modern theme compiled with swapped Sass variables for 20 token sets (every
  declaration Sass changed must be emitted with the same value, every emitted declaration must equal what Sass
  compiled); (3) `ThemeTokenCssCompiledSheetTest`: at the theme's own defaults the renderer must equal the shipped
  `react4j-modern.css`, and a **coverage guard** feeds a marker colour in place of each default and fails if the shipped
  sheet still holds a declaration with the default colour that the renderer does not emit. That guard keeps an
  `ALLOWLIST` of four gray-600 coincidences for `secondary` (`.blockquote-footer`, `.btn-link`, `.dropdown-menu`,
  `.form-floating` disabled label), whose default `#6c757d` equals Bootstrap's gray-600 by palette coincidence.
  **Regenerate both goldens with `node regenerate-golden.js`** in `src/test/resources/theme/golden/` (it requires `sass`
  and `postcss` from `react4j-core-ui/src/main/react/node_modules`, so run `npm install` there first); commit the
  result. Never edit a golden by hand.
- **Known limits**: `baseFontSize` does not rescale headings (Bootstrap sizes them in rem from the root); a re-emitted rule
  sits after the whole sheet, so `.text-bg-<role>` beats a `.text-*` utility on the same element (the stock sheet orders
  them the other way); the inline `<style>` needs `style-src 'unsafe-inline'` where an app sets a Content-Security-Policy.
- **Design tokens on TABLER** (plan-277 T3): the same setters, but `TablerTokenCss` (`service.internal.service.internal.theme`, a pure function
  `ThemeTokens -> String`, selected by `ThemeHeadRenderer` per preset) writes **base custom properties only**, in ONE rule with the selector
  `:root,[data-bs-theme=light],[data-theme=light]` after the Tabler link, and never a component rule (no `.btn-primary`, no
  `BootstrapColorDeriver`): Tabler derives hover, active and subtle shades in the browser. The rule ties the sheet's highest-specificity declaring
  selector and comes later, so it wins in light AND dark mode (the sheet's dark block redefines none of these variables). Per token: a colour
  writes `--bs-<role>` (hex) and `--bs-<role>-rgb` (`r, g, b`) and nothing else; **`--bs-<role>-fg` is NOT derived** (Tabler's
  `var(--bs-light)`), so choose role colours dark enough for light text; `fontFamily` writes `--bs-font-sans-serif` (the body family follows),
  `baseFontSize` writes `--bs-body-font-size`; `borderRadius(r)` writes `--bs-border-radius-md` = r (`--bs-border-radius` follows) and
  sm/lg/xl/xxl as `calc(r * 2 / 3)`, `4 / 3`, `8 / 3`, `16 / 3` (Tabler's own ratios to its 6px base, exact and unit preserving for px and
  rem; `-xs` is unread and not written); `shadowStrength` scales the alpha of `--bs-shadow-color` (`light-dark(rgba(18, 18, 23, 0.4), #000)`)
  by 0 / 0.5 / 1 / 2 in both arms (the opaque dark arm is capped at 1, so STRONG equals DEFAULT in dark mode), DEFAULT writes nothing, and every
  non-default strength pins `--bs-shadow-border` (the 1px ring of mentions and avatars, a border drawn as a shadow) to its default so a strength
  never changes a border; focus rings are outlines and are untouched. A block with no declaration (no token, or only `DEFAULT` strength) is not
  emitted at all.
- **Tabler guards** (`TablerTokenCssSheetGuardTest`, against the SHIPPED `/public/css/theme/react4j-tabler.css` of the installed core-ui jar, so
  install `react4j-core-ui` before running `react4j-core` tests): (1) every custom property the writer can emit is READ in the sheet
  (`var(--name` followed by `)` or `,`; a name that is only declared fails, e.g. Tabler declares `--bs-<role>-lt-rgb` and never reads it),
  collected by rendering every token combination and pinned to an explicit list; (2) every declaration of an emitted name sits in a selector the
  writer's rule ties, so a Tabler update declaring one in the dark block or in a more specific selector fails; (3) the mirrored Tabler constants
  (the radius ratios against the base size, the default shadow colour and alpha, the shape of `--bs-shadow-border`) equal the sheet. Each guard
  has a negative control against a doctored sheet. A red guard means Tabler changed: update `TablerTokenCss` deliberately, never the guard.
- **Cascade order**: theme link, then the token `<style>` block (only when a token is set), then added sheets, then the app's
  `/css/color.css`, `/css/print.css`, `/css/custom.css`, then the bundle CSS. App CSS therefore wins equal-specificity
  ties against Bootstrap in **both** modes, including disabled.
- **URL namespace**: the stylesheets are served from the core-ui jar at `/css/theme/react4j-modern.css` and
  `/css/theme/bootstrap.min.css`. They stay under `/css/**` on purpose: the security chains of the consuming apps
  (e.g. Deployer, SecureVault) permit only `/css/**` unauthenticated, any other path would render the page unstyled.
- **Internals** (not public, no new public bean): `ThemeConfigurationService` (read view `getSettings()` returning the
  immutable `ThemeSettings` snapshot) is implemented by `ThemeConfigurationServiceImpl`; `ThemeHeadRenderer` is a pure
  function snapshot to head markup and attribute (`service.internal.service.internal.theme`).
- **Tests**: `ThemeHeadRendererTest`, `ThemeHeadRendererTokenTest`, `ThemeHeadRendererPresetTest` and `ThemeHeadRendererTablerTokenTest` (no
  mocks), `TablerTokenCssTest`, the token suites listed under
  "Guards", `ThemeConfigurationServiceImplTokenTest` (injection list, state unchanged on rejection), and one MockMvc
  class per mode (`IndexHtmlController*ThemeTest`, `*TokenThemeTest`) against the real core-ui template. Each mode needs
  its own Spring context because the rendered page is cached. The browser side is `react4j-ui-test`
  `browser.theme.ModernThemeIT` / `DisabledThemeIT` (the disabled one activates the `theme-disabled` profile and must
  equal the stock baseline fixture with zero deltas) and `TokenThemeIT` (profile `theme-tokens`: primary `#0f766e`,
  radius `12px`; asserts the served head carries the block, then the computed background, hover background and radius
  of the showcase's "Open modal" button). The hover expectation is read through the same route a button takes
  (stylesheet rule reading a custom property): a browser rounds a channel sitting on x.5 differently for an inline
  `style.color` than for that route.
  `TablerTokenThemeIT` (profile `theme-tabler-tokens`: TABLER, primary `#0f766e`, radius `12px`) proves the TABLER tokens in a real
  browser: the served head and the served sheet first (freshness), then the "Open modal" background and radius, a hover shade that is
  darker and differs from Tabler's OWN hover (measured in a second same-origin document, an iframe loading the same sheet without the
  token block), the breadcrumb "Home" link colour (Tabler draws it from the primary), and the primary again with `data-bs-theme=dark`
  set in the page (with a control that the dark scope really took effect). Tabler's primary button has a TRANSPARENT border, so the
  modern test's border-colour assertion does not transfer. `getComputedStyle` serialises `oklch()`/`color-mix()` results in their own
  space, so colours are compared as sRGB bytes read through a canvas, not as `rgb()` text.
  **ui-test hazard, not theme related**: surefire redirects `java.io.tmpdir` to `target/surefire-java-io-tmpdir`. After
  many runs without `mvn clean` the accumulated `tomcat.*` directories slow the 1 ms spill sampler of
  `UnbufferedUploadTransportEndToEndTest` until its positive control fails; after a `clean` that directory does not
  exist and Mockito's agent cannot start (`IOException ... path not found`) in the first Mockito test class. Fix the first by
  deleting `target/surefire-java-io-tmpdir`'s contents, the second by creating the directory, not by touching either test.
  **Tabler ITs (plan-277 T4)**: `TablerThemeIT` (profile `theme-tabler`: served head, sheet loaded once, primary 6,111,209 from the served sheet's own
  `--bs-primary-rgb`, DiagramViewer band 40px/controls 31px, dropdown menu reachable, toast not clipped, badge readable, modal), the screenshot ITs
  `ShowcaseScreenshotTablerDarkIT` and `ShowcaseScreenshotTablerComparisonIT` (pins its own profile and starts a second in-process Tabler app, so one
  browser session captures both themes into `docs/theme-screenshots/tabler/`). Whole suite on Tabler: `mvn verify "-DexcludedGroups="
  "-Dspring.profiles.active=theme-tabler" "-Dit.test=*IT,!ShowcaseScreenshotModernIT"` (the `*IT` include is required: an exclusion-only `it.test`
  makes failsafe run every `*Test` class too, without surefire's tmpdir argLine); `ModernThemeIT` is expected red there. Server-side showcase state
  (an open modal or offcanvas) is shared by every IT of one Spring context, so an IT that opens one must close it in a `finally`.

## File upload: two transports

`FileUploadController` exposes two upload endpoints. Both funnel through the same
`FileUploadService`/`UploadChannelRegistry` path into the application's `UploadChannel`
(`react4j-core-components`), so a consuming app writes one `UploadChannel` implementation regardless of
which transport delivers the bytes.

### `/ui/upload` (multipart, the default) - buffers a plaintext temp file per part

This is what every existing consumer uses today, unchanged. **It is important to understand what it
actually does before reaching for it in a confidentiality-sensitive application**: Spring's
`StandardServletMultipartResolver` detects the `multipart/*` content type and hands the request to the
servlet container's own multipart parser *before* `FileUploadController` ever runs.
`FileUploadConfiguration`'s `MultipartConfigElement` leaves `fileSizeThreshold` at Tomcat's default of `0`,
so **every part of every multipart upload - including a plain form field, not only the file part - is
written to a plaintext temporary file on disk before `UploadChannel.consume(...)` is invoked**, and deleted
only at the end of the request. Measured (plan-154 Spike S3-alpha): the spill file appears at
`<java.io.tmpdir>\tomcat.<port>.<random>\work\Tomcat\localhost\ROOT\upload_<uuid>_<seq>.tmp`, its SHA-256
equals the payload's, and it survives for the whole request. `spring.servlet.multipart.*` size properties
are inert in any React4J app, because `FileUploadConfiguration` already supplies the only
`MultipartConfigElement` and Spring Boot's `MultipartAutoConfiguration` backs off (`@ConditionalOnMissingBean`)
once that bean exists - the operative size knobs are `react4j.upload.max-file-size` /
`react4j.upload.max-request-size` (default 25 MB each).

**So: an encrypting (or otherwise confidentiality-sensitive) `UploadChannel` sitting downstream of this
transport does not, by itself, mean "no plaintext copy of the upload exists anywhere" - the container already
made one, one layer below the channel.** This is a property of the transport, not something an
`UploadChannel` implementation can fix from inside `consume(...)`.

### `/ui/upload/raw` (opt-in, unbuffered) - no temp file, no size-proportional heap buffer

An element opts in via `Form.FileUploadFormElement#withUnbufferedTransport()`; an element that does not is
completely unaffected - same multipart transport, same behaviour, same defaults, as today. Reach for it when
a channel's contract genuinely requires that no plaintext copy of the upload ever touches disk (e.g. an
encrypting sink), or when uploads are large enough that the multipart transport's disk round trip is a real
cost.

**Mechanism.** A request whose `Content-Type` is not `multipart/*` is never wrapped by Spring's multipart
resolver at all - `DispatcherServlet` never calls into `MultipartResolver`, so nothing parses or spills it.
`FileUploadController#uploadFileRaw` reads `HttpServletRequest.getInputStream()` directly and never calls
`getPart`/`getParts`/`getParameter` or anything else that would trigger container-side multipart parsing.
Measured (plan-154 Spike S3-alpha): 500 MB round-tripped with **zero** temp files and a flat **~27-30 MB**
peak heap under a 256 MB heap cap; 200 MB passed under a 96 MB cap.

**Wire shape.** The uploadId travels as the `X-Upload-Id` request header - deliberately a header, not a
query parameter, so it is not the kind of thing a default access log records. The client filename travels
as the `X-Filename` header, **percent-encoded (UTF-8, `URLEncoder`/`URLDecoder`)** - a raw HTTP header value
is not UTF-8 by default, so an un-encoded non-ASCII filename would be mangled; percent-encoding keeps the
header value itself pure ASCII regardless of how the container handles header charsets. The declared content
type travels on the request's own `Content-Type` header - any value that does not start with `multipart/` is
safe on this transport by construction, so no separate header is needed for it.

**The global size ceiling still applies, and is enforced by the controller itself.**
`FileUploadConfiguration`'s `MultipartConfigElement` is the coarse global ceiling on the multipart transport,
enforced by the container's own multipart parsing - which never runs on this transport, so that enforcement
would otherwise silently vanish. `FileUploadController#uploadFileRaw` reads the same
`react4j.upload.max-file-size` / `react4j.upload.max-request-size` properties, rejects a request whose
declared `Content-Length` already exceeds the ceiling before touching the stream, and additionally wraps the
request body in a `BoundedInputStream` at that ceiling so an unknown-length (chunked) body is bounded too -
both map to the same `413` the multipart transport produces. A channel implementing `UploadChannel` directly
(not via `AbstractUploadChannel`) still gets this global bound automatically, because it is baked into the
`InputStream` the channel reads, not layered on top by the channel.

**Real-container upload tests live in `react4j-ui-test`, not here.** Proving "no temp file" needs a real
embedded servlet container (`@SpringBootTest(webEnvironment = RANDOM_PORT)`); `MockMvc` runs no container and
so cannot produce, or fail to produce, a spill - a green result there would be vacuous. This module has no
embedded container on its classpath (see "Build" above), and none should be added here just for a test -
`react4j-ui-test` already carries one transitively via `CommonsSpringBootParent`, so
`UnbufferedUploadTransportEndToEndTest`/`UnbufferedUploadTransportGlobalCeilingTest` live there instead.

### A related finding, not used by this transport but worth knowing

`tomcat-embed-core` - already on every Spring Boot 3 web app's classpath, React4J's included - ships a
complete **streaming** multipart parser at `org.apache.tomcat.util.http.fileupload.*`, with a public
`getItemIterator`, that never spills to disk either. It keeps the multipart wire format (unlike the raw
transport above) at the cost of a hand-rolled parsing loop. Anyone who later needs a non-buffering transport
that speaks multipart needs no new dependency for it. Two things worth recording alongside it, so nobody
re-derives them: `commons-fileupload2-jakarta-servlet` **does not exist** on Maven Central (the Servlet-6
artifact is `commons-fileupload2-jakarta-servlet6`, and its newest release is `2.0.0-M5`, a milestone with no
GA); and `spring.servlet.multipart.resolve-lazily=true` does make a controller reading the raw body possible
even though React4J supplies the `MultipartConfigElement`, but its measured failure mode with the flag left
at its default (`false`) is silent: **HTTP 200 with an empty item list, while still writing plaintext spill
files** - which is why it is not the mechanism `/ui/upload/raw` uses.

## `UploadChannelRegistry` is bounded, not just idempotent

`UploadChannelRegistryImpl` (`service.internal.upload`) is self-bounding: an entry not registered or looked
up for longer than a configured time-to-live is removed from both its internal maps (not merely hidden from
`lookup`), and the total entry count is additionally capped with least-recently-used eviction. Both bounds
are independent - a time-to-live bounds growth per unit time, a capacity cap bounds a burst inside one
window - and neither substitutes for the other. Configurable via `react4j.upload.registry.time-to-live-minutes`
(default `30`) and `react4j.upload.registry.max-entries` (default `1000`). Do not reach for Commons
`DurationLimitedCache` here: it is a lazy read-side expiry gate that never removes entries, so it fixes the
symptom and leaves the defect.

The impl has two public constructors: `UploadChannelRegistryImpl(long timeToLiveMinutes, int maxEntries)`,
annotated `@Autowired` and the one Spring actually wires, which supplies `Clock.systemUTC()` internally; and
`UploadChannelRegistryImpl(Clock clock, long timeToLiveMinutes, int maxEntries)`, used only by tests
(`UploadChannelRegistryImplTest`, `FileUploadServiceImplTest`, `FileUploadControllerTest`) to advance time
deterministically without sleeping. React4J publishes no `Clock` bean - a `@ConditionalOnMissingBean(Clock.class)`
bean was tried and reverted: it widened this library's published Spring surface for all eleven consuming
applications (plan-156 cliff X8), and - independent of that - the "always wins" guarantee it advertised did
not hold, because `@ConditionalOnMissingBean` is only order-safe on auto-configuration classes processed after
user configuration, and `FileUploadConfiguration` is a plain `@Configuration` class with no defined ordering
relative to an embedding app's own `@Configuration` classes; an app that declares its own `Clock` bean could
have collided with this one at startup (`BeanDefinitionOverrideException`) depending on registration order. A
periodic `@Scheduled` sweep (piggybacking on
`ReactUIAutoConfiguration`'s existing `@EnableScheduling`) plus an opportunistic sweep on every `register`/
`lookup` call bound staleness even if nothing else happens. `UploadChannelRegistry` the interface is
unchanged - eviction is entirely internal to the impl.
