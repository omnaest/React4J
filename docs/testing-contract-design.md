# React4J Testing / Rendering-Contract Design

Concise test-plan for the four-goal initiative. Full Charter, cliff option-evaluations, and
slice backlog: `.claude/plans/plan-74-react4j-testing-rendering-contract.md`.

## Premise
The node hierarchy (component builder → JSON node tree) is the single source of truth. These
four goals are completeness/fidelity checks on the edges of that contract: the builder API
that produces it, the client DOM renderer, the static renderer, and the DOM's testability.

## Two verification findings (they shape everything)
1. **`Location` does NOT reach the DOM as a stable attribute, and no settle signal exists.**
   `node.target` (the Location path) is on every node's JSON but is used only for client-side
   event routing — never emitted to the DOM. The only DOM id attribute is `id={locator}` on
   `Card`/`Container`, and `locator` is a sparse opt-in **link anchor** (`withLinkLocator`),
   not the Location path. → Goal 3(a) `data-location` and Goal 3(b) settle signal must be
   **built**.
2. **The static renderer is NOT Freemarker.** `NodeHierarchyStaticRenderer` uses per-node-type
   `NodeRenderer` HTML lambdas registered via `UIComponentRenderer.manageNodeRenderers`
   (`NodeRenderType = {HTML, SVG}`). The only `.ftl` is `sitemap.xml.ftl`. Node types with no
   registered renderer fall through to a **lossy** `DefaultNodeRenderer` (no wrapper/attrs). →
   Goal 4 = "guard what exists AND surface drift"; vocabulary is "registered HTML NodeRenderer",
   not "Freemarker template".

Bonus: Goal-1 `*ImplTest` already exist for ~23 newer components — the Goal-1 gap is the ~20
older core components. Goal-2's server round-trip exists (`RerenderingSiblingButtonClickEndToEndTest`);
the gap is a **real-browser** end-to-end.

## Goals, tiering, and homes
| Goal | What | Tier | Default `mvn test` | Home |
|---|---|---|---|---|
| 1 Contract fidelity | per component: builder API → node hierarchy (properties, nesting, i18n, handler wiring, rerender config); no internal mocks | 1 (JVM/fast) | yes | react4j-core-components (`*ImplTest`) |
| 4 Static-render completeness | per component: correct static HTML (headings→`<h*>`, links→`<a href>`, i18n resolved) + ONE parametrized meta-test failing the build if any node type lacks an HTML renderer | 1b (JVM/deterministic) | yes | react4j-core |
| 2 Pipeline liveness | per **archetype** (~6): node JSON → real DOM, click → `ServerHandler` → re-render, against booted `MockApplication` | 2 (browser) | **no** (`*IT` / `@Tag("browser")`) | react4j-ui-test (+ `react4j-test`) |
| 3 Testability contract | stable `data-location` selectors + `data-inflight-count`/`data-rerender-pending` settle signal + thin publishable `react4j-test` harness | framework-wide, proven via Tier 2 | frontend Jest: yes | react4j-core-ui + `react4j-test` |

## Archetype → representative component (Goal 2)
leaf text/heading → Heading · container/composition → Composite/Card · **interactive+server-handler
(pivotal) → ToggleButton/Button** · rerendering/interval → IntervalRerenderingContainer ·
form+file upload → Form+FileUpload · navigation/routing+i18n locale switch → NavigationBar+locale switch.
Do NOT write one browser suite per component — the renderer is shared.

## Frozen decisions (cliffs)
- **C1** `data-location = node.target.join(".")`, emitted once at the render dispatch point (not per-render hashes, not the opt-in `locator`).
- **C2** global in-flight counter → `data-inflight-count` / `data-rerender-pending` on a stable root; decrement in `finally`.
- **C3** `react4j-test` harness as a new published module (boot MockApplication, find-by-location, click-and-wait-settle, read node state) — **built only if the first inline Tier-2 tests prove painful**.
- **C4** a small `registeredNodeTypes(NodeRenderType)` introspection hook on the render processor drives the Goal-4 meta-test (needs `junit-jupiter-params`).
- **C5** Playwright-Java `*IT`, excluded from default `mvn test` via a POM `<excludedGroups>` property (NOT a surefire `<configuration>` literal). Mandatory feasibility spike first (does the reactor build+serve the JS bundle?).

## Non-goals
No pixel/visual/CSS regression. Do not re-test React itself or Spring bean wiring. No
per-component browser tests.

## Pilot
One vertical through all four goals: Heading (Goal 1 + Goal 4) + `data-location`/settle hooks
(Goal 3) + one ToggleButton browser round-trip (Goal 2). Report pilot results before fanning
out to the ~20 older components (Goal 1), per-component static assertions (Goal 4), and the
remaining 5 archetypes (Goal 2).
