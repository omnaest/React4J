# react4j-core-components

The component and form model of React4J (`org.omnaest.react4j.component`): `Form` and its field elements,
the `UploadChannel`/`UploadContent`/`UploadReceipt` upload extension point, and the node types
(`org.omnaest.react4j.component.form.internal.renderer.node`) serialized to the client.

## Build

```cmd
mvn clean install
```

## The upload extension point

`UploadChannel` is the pluggable sink an application implements to receive an uploaded file; ships with
`ByteArrayChannel` (in memory) and `FileChannel` (streamed to a server-controlled path). Neither the
`UploadChannel` interface nor `UploadContent` know anything about which HTTP transport delivered the bytes -
that is `react4j-core`'s (`FileUploadController`'s) concern, not this module's.

### `Form.FileUploadFormElement#withUnbufferedTransport()`

Opt-in per upload element. See `react4j-core`'s `CLAUDE.md` for the full mechanism and the finding that
motivated it (the default multipart transport writes a plaintext temporary file per part, via the servlet
container, before any `UploadChannel` ever runs). What lives in *this* module:

- `Form.FileUploadFormElement#withUnbufferedTransport()` - the builder method. Default is `false`; calling it
  is the only way an element's behaviour changes.
- `FormFileUploadNode#isUnbufferedTransport()` - the rendered flag the client branches on to decide which
  request shape to send. `FormFileUploadNode#getUploadUrl()` already reflects the choice too
  (`ui/upload` vs `ui/upload/raw`), computed in `FileUploadFormElementImpl#renderNode`.

Both are additive fields with a default that reproduces today's behaviour exactly, so no existing consumer's
rendered node - or wire contract - changes by not calling the new method.

## Standing policy: additive public API on a published React4J component (plan-261 Cliff C5)

Generalises the reasoning above into a four-part admissibility test, so the same question is not
re-litigated per card. A new method on a published React4J component interface is **admissible without a
design cliff** when all four hold:

1. **Purely additive** - no existing method's signature or semantics change.
2. **Behaviour-preserving default**, in two limbs that are deliberately *not* the same claim. For a
   consumer that never calls the new method:
   - **(2a) the rendered OUTPUT is byte-for-byte identical, on *every* renderer the component registers.**
     This is the limb that bites and the limb that is mechanically assertable. The "on every renderer"
     clause is what catches a field added to one renderer and left out of a second - a component with more
     than one render surface (a client `.tsx` **and** a `NodeRenderType.HTML` static renderer) must have the
     new field reach both, or the default case is unpinned on the one left out.
   - **(2b) the serialized NODE may gain a field, provided its default is one every renderer already
     ignores.** It need not be byte-identical, and requiring that would be wrong here.

   **Corrected 2026-09-26, during the very slice that introduced this policy - recorded rather than quietly
   fixed, because the error was in the criterion and not in the code it judged.** This criterion first read
   "the rendered **node** is byte-for-byte identical", full stop. That is violated by
   `Modal.withFullscreen(boolean)` itself: `ModalNode.fullscreen` is a primitive `boolean` `@JsonProperty`
   alongside `visible` and `centered`, so it is *always* serialized and **every** `MODAL` node in every
   React4J application's `/ui` JSON now carries `"fullscreen":false`. As originally worded the criterion
   would have rejected the conventional shape and pushed the next author toward a boxed `Boolean` or an
   optional field purely to satisfy a sentence - inconsistent with every sibling field on the same node, and
   for no behavioural gain, since a client that does not read the field cannot be affected by it. Note that
   the two fields this policy shipped with are legitimately *different* on this point and both are correct:
   `DiagramViewerNode.height` is a `String` that stays absent when unset (so its node genuinely is
   byte-identical), while `ModalNode.fullscreen` follows `centered`'s primitive-boolean convention (so its
   node is not). Splitting the criterion is what makes both readable as conformant.
3. **No partial vocabulary widening** - no new enum member whose token mapping is non-total (cf.
   `Modal.Size.toBootstrapToken()`, which would have to return `null` for a hypothetical `FULLSCREEN`
   member).
4. **No foreign vocabulary exported as API** - no raw CSS class / Bootstrap token escape hatch.

Failing any one makes it a cliff requiring an option evaluation (R15) before proceeding.

**Worked applications** (all four hold, no cliff needed):

- `Form.FileUploadFormElement#withUnbufferedTransport()` - see above.
- `Modal.withFullscreen(boolean)` - additive; the node gains an always-serialized `"fullscreen":false`
  (criterion 2b, following `centered`'s primitive-boolean convention) while the rendered output is unchanged
  because `Modal.tsx` passes `undefined` rather than `false` to react-bootstrap, so no class is emitted
  (criterion 2a). `Modal` registers no `NodeRenderType.HTML` renderer at all
  (`ModalImpl.manageNodeRenderers` has an empty body), so 2a has only one render surface to satisfy here.
  Independent of `Modal.Size`, so criterion 3 is untouched.
- `DiagramViewer.withHeight(String)` - additive; default (unset/`null`) reproduces today's
  `DiagramViewerNode` exactly, and criterion 2's "on every renderer" clause is exactly what is doing real
  work here: `DiagramViewerImpl` registers **two** render surfaces (the client component and a
  `NodeRenderType.HTML` renderer emitting `width`/`max-height` inline), so the new field had to reach both,
  and the HTML renderer must omit the `height:` declaration entirely when unset rather than emit an empty
  or null one.
- `Breadcrumb.BreadcrumbEntry#onClick(EventHandler)` / `Breadcrumb.withLinkLocator(String)` (card `ad16d6a9`,
  plan-262 S1) - shipped. `onClick` lives on `BreadcrumbEntry`, not on `Breadcrumb` itself (C3): a trail needs
  one handler per entry, so `BreadcrumbEntry` was promoted out of `BreadcrumbImpl` into its own
  `BreadcrumbEntryImpl` (`extends AbstractUIComponent<BreadcrumbEntry>`, own `Location`/`Target`, own
  `manageEventHandler`), mirroring `PaginationItem`/`PaginationItemImpl` exactly (C1) - forced, not chosen:
  `AbstractUIComponent<UIC>` is bounded `UIC extends UIComponent<?>`, so a bare `BreadcrumbEntry` interface
  could not be the type argument. Criterion 2a's "on every renderer" clause bit exactly as flagged:
  `BreadcrumbImpl` registers both a `NodeRenderType.HTML` renderer and the client `.tsx`, so `withLinkLocator`
  had to reach both (the HTML renderer emits the locator as the `<nav>`'s `id` attribute, omitted entirely
  when unset - the `DiagramViewer.height` shape) while `onClick` deliberately reaches the interactive client
  renderer only, written down on the method's own javadoc: a `NodeRenderType.HTML` static render has no
  channel back to `POST /ui/event`. `BreadcrumbStaticRenderTest`'s pre-existing assertions pass unchanged
  (verified: temporarily emitting the `id` attribute unconditionally turned the test RED naming the attribute,
  reverting restored GREEN), which is the criterion-2a evidence for this application.

- `Text.withStyle(Text.Style)` with `enum Style { MUTED }` (card `4eee2913`, plan-283 S1) - shipped (Java side; the client `Text.tsx`
  is plan-283 S2). Additive. Criterion 2b: `TextNode` gains a nullable `style` field, `null` when unset and the enum name (`"MUTED"`)
  when set - the enum name verbatim because the client compares it exactly. Criterion 2a: `TextImpl` registers ONE render surface here, the
  `NodeRenderType.HTML` renderer, and it is byte-identical for an unstyled text (pinned BEFORE the change by
  `TextStaticRenderTest.testUnstyledText...`, which compares the complete output); a styled text wraps the same escaped texts in exactly one
  `<span class="...">`. Criterion 3: the one enum member maps totally through the single method `Style.toCssClass()` (`MUTED` -> Bootstrap
  5.3's `text-body-secondary`, a colour utility defined in all three served sheets: modern, stock and Tabler), and
  `TextStyleThemeClassSheetGuardTest` in `react4j-core` fails if a member's class sets no colour in any of them, so a new member cannot be
  a silent no-op on one preset. Criterion 4: no consumer-chosen class; the vocabulary is React4J's own and the mapping is the enum's.
  `withStyle(null)` stores `null` like every sibling `withStyle` and means "unset". Before this method `Text` rendered no element on the
  client and so offered no hook for a muted look (the plan-282 K4 accepted deviation).
- `OrderedList` / `UIComponentFactory#newOrderedList()` (card `d4269b7a`, plan-283 S1) - a new component beside `UnsortedList`, so
  criterion 1 holds trivially (a new type and one new factory method, no existing method or output touched) and criterion 2 has no prior
  output to preserve. Node `ORDEREDLIST` (`{"type":"ORDEREDLIST","elements":[...],"startNumber":N}`, `OrderedListNode` has the no-arg
  constructor and non-empty type `NodeRendererRegistrationKeyGuardTest` demands); HTML renderer `<ol>` with `start="N"` only for N != 1
  (default 1, `withStartNumber(int)` validates nothing: HTML accepts any integer). It exists because `react4j-core`'s
  `MarkdownServiceImpl` dropped a markdown ordered list silently; see that module's "Known defect classes".
- `Text.withEmphasis(Text.Emphasis...)` with `enum Emphasis { BOLD, ITALIC, STRIKETHROUGH }` (card `79ce393f`, plan-284 S2; the client
  `Text.tsx` is plan-284 S3) - additive and cumulative (idempotent per member, `null` ignored). Criterion 2b: `TextNode` gains a nullable
  `emphasis` list, `null` when unset (exactly like `style`) and otherwise the member names in enum order whatever the order of the calls.
  Criterion 2a: `TextImpl` registers ONE render surface, the `NodeRenderType.HTML` renderer, and it is byte-identical for a text without
  emphasis (pinned BEFORE the change by `TextStaticRenderTest.testUnstyledText...`); with emphasis it wraps the same escaped texts in
  `<strong>` / `<em>` / `<del>`, the first enum member outermost, inside the `Style` span when both are set. Criterion 3: the three members map
  totally through `Emphasis.toElementName()`. The mapping is to SEMANTIC ELEMENTS, not to classes, so unlike `Style.toCssClass()` it needs no
  per-stylesheet guard: every sheet's reboot styles `strong`/`em`/`del`. Criterion 4: no consumer-chosen markup. It exists because
  `react4j-core`'s `MarkdownServiceImpl` parsed bold/italic/strikethrough and then dropped them (bold was mapped to a paragraph-wide class that no
  served sheet defines).
- `BlockQuote.addComponent(UIComponent)` / `addComponents(List)` and **the footer element only when a footer was set** (card `79ce393f`,
  plan-284 S2). The children are what lets a quote hold paragraphs with links and emphasis, lists and nested quotes; `BlockQuoteImpl` returns
  them from `getSubComponents` like `ParagraphImpl`/`CompositeImpl`, so traversals (event registration) reach them. Criterion 2b: `BlockQuoteNode`
  gains a nullable `elements`, `null` when no component was added. Criterion 2a: with texts and a footer and no children the HTML render is
  byte-identical (pinned BEFORE the change by `BlockQuoteStaticRenderTest.testQuoteWithTextsAndAFooterRendersAsItAlwaysDid`, compared with the
  line endings normalised because the template is checked in with LF); children render after the texts and before the footer.
  **A deliberate defect fix, recorded as such: the footer is omitted when none was set.** Before, the footer element was always emitted and
  Bootstrap's `.blockquote-footer::before` printed a lone em dash for an empty one. `BlockQuoteNode.footer` is now `null` (it used to be an empty
  `I18nTextValue`, because `LocalizedTextResolverService` turns a null text into an empty one) and the template emits the element only when the
  `footer` key is present. Both known callers (IMBSApplication's `SubHeaderComponent`, MockUI) set a footer, so neither changes; an unknown
  caller without a footer loses the stray dash. A client that dereferences `footer` unconditionally must be updated first (plan-284 S3).
- `Anker.addComponent(UIComponent)` / `addComponents(List)` (card `698a6daa`, plan-286 S1; the client `Anker.tsx` is plan-286 S2) - a formatted
  label, built exactly like `BlockQuote`'s children (cliff C1 = A1, option-evaluated in plan-286 section 2.2: it is the only option that is purely
  additive, follows the precedent, and lets the markdown mapper route a link label through its ONE dispatch). Criterion 2b: `AnkerNode` gains a
  nullable `elements`, `null` when no component was added. Criterion 2a: `AnkerRenderer` registers ONE render surface (the HTML renderer plus the
  client `.tsx`), and the HTML of an anker without children is byte-identical (pinned BEFORE the change by `AnkerStaticRenderTest` in
  `react4j-core`, `AnkerHtmlRenderTest` and `AnkerImplTest` here); the label is the text followed by each rendered child, no added whitespace.
  Children are located with `ChildLocationSupport.indexedChildLocation` in `render` and `getSubComponents` alike (that helper became `public` for
  it: `Anker` lives in another package than the other containers), a `null` list or member is ignored, and `asTemplateProvider` copies the list (the
  anker's text builder is shared with the template, the children must not be). Criterion 3: no enum. Criterion 4: no CSS vocabulary. **Caller
  obligation, written on the method: phrasing content only; never nest a link or button in an anchor** (React4J does not check it; CommonMark cannot
  produce one).
- **Accepted deviation from criterion 2a, user decision 2026-10-06 (card `ee0e2d09`): `Paragraph.withBoldStyle()` now renders bold in the static
  HTML.** `paragraph.html` honours the flag as `<p class="fw-bold">`, where it used to ignore it (and the client emitted the Bootstrap 4 class
  `font-weight-bold`, defined in none of the three served sheets, so the flag was visible nowhere). A paragraph that is not bold is byte-identical
  (pinned BEFORE the change by `ParagraphStaticRenderTest` in `react4j-core`); a caller of `withBoldStyle()` gets what it asked for for the first
  time. The workspace's one app caller is MarkdownServer (`MarkdownDocumentRenderer`, which bolds a whole paragraph for any bold run - finding
  card `3fe56d1c`).
- **Accepted deviation from criterion 2a, user decision 2026-10-19 (card `5f4d3f62`, plan-320 S16): the static HTML `Card` now places its
  title and locator as `Card.tsx` does.** Old default output: every card got `<h5 id="LOCATOR" className="card-header">TITLE</h5>` as its
  first child, so an untitled card showed an empty header bar (`id=""` without a locator) and the locator id lived on that bar. New default
  output: no `h5` and no header bar unless `withHeader` is set (the `div.card-header` of `header` is unchanged); the title, when it renders
  non-empty, is `<h4 class="card-title">TITLE</h4>` in the body, ordered image, featured title, title, sub title, inner body div; the locator
  is an `id` attribute on the root card div, after the class attribute and before `role`/`aria-label`, only when it is non-null and non-empty
  (otherwise no `id` attribute anywhere). The title is the `title` of the node, which `withTitle` sets on a card WITH an image (without an
  image it sets the featured title, which was never affected). Why: React is the reference renderer and the static output is what a visitor
  sees before React mounts, so the two should agree; criterion 2a would otherwise freeze a default that is wrong on the one renderer that
  matters. What callers see: an untitled card loses its empty header bar (a visible change, a few pixels of border and padding), a titled
  image card shows its title inside the body instead of in a bar above it, and a linked card's anchor (`href="#LOCATOR"`) moves from the
  header bar to the card's root, which is the same place on screen for a card whose bar was its first element. Pinned by
  `CardStaticRenderTest` in `react4j-core` (the exact default, the exact all-fields output with and without an image, the locator and title
  placement, escaping of title and locator), shown red against the old template first. The root and body `className=` spelling and the
  classless inner body `div` were left for the next entry (plan-320 S19), which changes the default output once more.
- **Accepted deviation from criterion 2a, user instruction 2026-10-19 (cards `45da782b` and `21da417b`, plan-320 S19): the static HTML `Card`
  now emits the class attributes `Card.tsx` emits.** Old default output: `<div className="card">` as root and `<div className="card-body">` as
  body, both the JSX attribute name, which an HTML parser keeps as an inert `classname` attribute (so a default static card had neither `card`
  nor `card-body`, and a full-height one lacked `card-body`), and a bare `<div>` as inner body unless `adjust` was set. New default output:
  root `class="card"` (`class="card h-100"` with `fullHeight`, unchanged), body `class="card-body"`, inner body `class="card-inner-body"`
  (`class="card-inner-body width-max-content"` with `adjust`, unchanged); the root attribute order (class, id, role, aria-label) and everything
  else is byte-identical. Why: `Card.tsx` is the reference renderer and the static output is what a visitor sees before React mounts. Done as
  ONE template change so that the default output changes once. What callers see: `card` and `card-body` are Bootstrap's own classes, defined in
  all three served sheets, so a static card gains its border and body padding before React mounts, and it now matches the mounted card.
  **`card-inner-body` is not styled by any React4J sheet** (`Card.css` has one rule, `.card.h-100 > .card-body`): it is a structural hook that
  applications select on (KanbanBoardServer `color.css` `#kanban-board .column .card .card-inner-body`) and browser tests select on
  (`TablerLookIT`, react4j-ui-test), which is why the static side must carry it. Pinned by `CardStaticRenderTest` in `react4j-core` (the exact
  default, full-height, adjusted and both all-fields outputs, and `testNoRenderedCardContainsClassNameInAnySpelling`), shown red against the old
  template first.
**Worked rejections** (fail at least one criterion, so each needed - and got - its own option evaluation):

- `Modal.Size.FULLSCREEN` (a new enum member) - fails criterion 3: `toBootstrapToken()` would become
  non-total, forced to return `null` for a member that isn't a size at all. Chosen instead:
  `Modal.withFullscreen(boolean)`, a separate additive boolean field mirroring react-bootstrap's own
  independent `size`/`fullscreen` props.
- `Modal.withDialogClassName(String)` (a raw CSS-class escape hatch) - fails criterion 4: it exports
  Bootstrap's class vocabulary as React4J's own published API, unbounded and reusable for anything, which is
  exactly the escape hatch this module's design has consistently avoided (see the typed-CSS-value reasoning
  on `DiagramViewer`'s own sizing methods).

## Published API and `internal` types (plan-320 S8, kanban fd9f338f)

`EventHandler` moved from `org.omnaest.react4j.service.internal.handler.domain` to `org.omnaest.react4j.domain`, beside the
components that speak it (`Button`, `Modal`, `DropTarget.DropEvent`, ...). A package move of a type is breaking for an explicit
importer or implementor; it was accepted at 1.0.0-SNAPSHOT because the guideline gives `internal` importers no compatibility claim.
Lambda callers (every known consumer) are source-compatible. `PublicApiInternalTypeExposureTest` ratchets the rest: a public type
outside `internal` that mentions an `internal` type on a public or protected member fails the build unless that type is on the test's
allowlist, and a stale allowlist entry fails too. The remaining entries are tracked on kanban card `cdeb180d`.

## NavigationBar dropdowns (plan-277 section 8, kanban bcfd7e44)

`NavigationBar#addDropdown(Consumer<NavigationBarDropdown>)` adds a toggle with a menu of `NavigationBarEntry` items beside the plain
`addEntry` entries; entries and dropdowns share one list in insertion order, and a dropdown's items are the same implementation as
the bar's entries (text, link, linked locator, linked component, active, disabled behave identically). The node contract the client
codes against: `NavigationBarNode.Entry#dropdownEntries` is `null` on a plain entry and a (possibly empty) list on a dropdown, whose
`text`/`active`/`disabled` describe the toggle and whose `link`/`linkedId` are `null`; one level only (an item is a plain entry). The
static HTML template renders a dropdown as `li.nav-item.dropdown > a.nav-link.dropdown-toggle + ul.dropdown-menu > li > a.dropdown-item`;
plain entries render as before. Plan-320 S3a made the static render follow `NavigationBar.tsx` for entries, toggles and items alike: `active` /
`disabled` classes, `target="_blank"` only when the href is the external link (a linked locator wins the href and gives `_self`), and no `href`
attribute when neither is set (the rules are written once on `NavigationBar.NavigationBarEntry`). Tests: `NavigationBarImplTest` (API to node, JSON
shape) and `NavigationBarStaticRenderTest`; the React half is `NavigationBar.test.tsx` in `react4j-core-ui`.

## Testing note

`FileUploadFormElementImplTest` covers the node-rendering contract (algorithmic, zero Spring context, zero
mocks - a fake `UploadChannelRegistry`/`EventHandlerRegistry` in place of Spring beans). The property that
the *unbuffered* transport actually avoids a temp file spill, against a real servlet container, is proven in
`react4j-ui-test` (`UnbufferedUploadTransportEndToEndTest`) rather than here - this module has no embedded
servlet container on its classpath and does not need one for what it owns (the node/builder contract, not the
HTTP transport).

## Known defect classes

Recorded by the orchestrator, keyed by the property that was violated, not the symptom. Look up a new defect here by its property before diagnosing it.

| Class (property) | Occurrences | Guard | Status |
|---|---|---|---|
| A node field is honoured identically by every renderer the component registers (static HTML and React). The two renderers are independent hand-written implementations with nothing comparing them, so a field wired on one side only passes every test. | plan-262 Breadcrumb linkedId (React ignored it); df581d4c NavigationBar linkedId (fixed by plan-277); plan-320 S3 sweep 2026-10-19: Breadcrumb link/linkedId precedence and href="null", NavigationBar active/disabled/target (fixed here); 7 more components with 17 field-level gaps plus 4 undocumented structural ones (kanban card 3277cf97). **Resolved by plan-320 S13 (card 3277cf97), per item (A = static side now honours the field, pinned by a `*StaticRenderTest` in `react4j-core` shown red first and with a pin of the unset output taken before the change; B = written into the public javadoc as structural):** Card featuredTitle/subTitle/header/footer/image/adjust/fullHeight/ariaLabel A; Jumbotron fullWidth A; UnsortedList enableBulletPoints A (second template `unsorted_list_with_bullets.html`); GridContainer unlimitedColumns A (class only, it selects nothing statically) and Cell column span B; ScrollbarContainer announcedUpdates A and scrollToBottomOnUpdate B; Table size A and the wrapper of the DEFAULT options (responsive, no size) B; Anker title A; Pagination.onClick B, Alert.dismissible B, ListView (element rendered once, not per row) B. **Card title placement (card 5f4d3f62), resolved by plan-320 S16 as an accepted deviation:** the static Card put the title in an `h5.card-header` bar carrying the locator id on every card, where `Card.tsx` puts the title in an `h4.card-title` in the body and the locator on the root; the static default was changed to match the React renderer (see the "Accepted deviation from criterion 2a ... card `5f4d3f62`" entry under the standing policy). **Card class attributes (cards 45da782b and 21da417b card.html item), resolved by plan-320 S19 as a second accepted deviation:** the static Card wrote the JSX name `className=` on its root and body (inert in HTML) and no class on the inner body; it now emits `card`, `card-body` and `card-inner-body` as `Card.tsx` does (see the "Accepted deviation ... cards `45da782b` and `21da417b`" entry). The two departures from "all field items are A" are Cell column span and the Table default wrapper: both have no unset state, so honouring them would change the output of every existing grid or table (criterion 2a). | Per-component static render tests and jest only; no cross-renderer guard. A field-parity guard (rung ii) is proposed, unscheduled (plan-320, kanban card fb604efe). | third+ occurrence; fix-at-source proposed; the instances of card 3277cf97 are resolved (plan-320 S13) |

**Template trap (plan-320 S13).** The static HTML templates under `render/templates/html` go through FreeMarker with its default whitespace stripping,
and an FTL tag or `${}` before the FIRST text of a template changes how the indentation of the `#list` lines below it is stripped (the first list item loses
an extra tab it has with a text-first template). So adding a conditional to line 1 of `unsorted_list.html` or `table.html` changes the whitespace of every
existing list or table, which criterion 2a forbids; those two choose a second template or wrap the rendered string in Java instead. Pin the complete unset
output before touching a template. Also visible there and deliberately left alone: `container_grid.html` writes the glued class `container-fluidpage-content` (kanban 21da417b).
`card.html` obeys the trap: its line 1 still starts with the literal text `<div` (the class is built inside the tag with `<#if fullHeight>`), which is
how plan-320 S19 fixed its `className=` without shifting any whitespace.