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

**Worked rejections** (fail at least one criterion, so each needed - and got - its own option evaluation):

- `Modal.Size.FULLSCREEN` (a new enum member) - fails criterion 3: `toBootstrapToken()` would become
  non-total, forced to return `null` for a member that isn't a size at all. Chosen instead:
  `Modal.withFullscreen(boolean)`, a separate additive boolean field mirroring react-bootstrap's own
  independent `size`/`fullscreen` props.
- `Modal.withDialogClassName(String)` (a raw CSS-class escape hatch) - fails criterion 4: it exports
  Bootstrap's class vocabulary as React4J's own published API, unbounded and reusable for anything, which is
  exactly the escape hatch this module's design has consistently avoided (see the typed-CSS-value reasoning
  on `DiagramViewer`'s own sizing methods).

## NavigationBar dropdowns (plan-277 section 8, kanban bcfd7e44)

`NavigationBar#addDropdown(Consumer<NavigationBarDropdown>)` adds a toggle with a menu of `NavigationBarEntry` items beside the plain
`addEntry` entries; entries and dropdowns share one list in insertion order, and a dropdown's items are the same implementation as
the bar's entries (text, link, linked locator, linked component, active, disabled behave identically). The node contract the client
codes against: `NavigationBarNode.Entry#dropdownEntries` is `null` on a plain entry and a (possibly empty) list on a dropdown, whose
`text`/`active`/`disabled` describe the toggle and whose `link`/`linkedId` are `null`; one level only (an item is a plain entry). The
static HTML template renders a dropdown as `li.nav-item.dropdown > a.nav-link.dropdown-toggle + ul.dropdown-menu > li > a.dropdown-item`;
plain entries render exactly as before. Tests: `NavigationBarImplTest` (API to node, JSON shape) and `NavigationBarStaticRenderTest`.

## Testing note

`FileUploadFormElementImplTest` covers the node-rendering contract (algorithmic, zero Spring context, zero
mocks - a fake `UploadChannelRegistry`/`EventHandlerRegistry` in place of Spring beans). The property that
the *unbuffered* transport actually avoids a temp file spill, against a real servlet container, is proven in
`react4j-ui-test` (`UnbufferedUploadTransportEndToEndTest`) rather than here - this module has no embedded
servlet container on its classpath and does not need one for what it owns (the node/builder contract, not the
HTTP transport).
