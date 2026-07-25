/*******************************************************************************
 * Copyright 2021 Danny Kunz
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.service.internal.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.omnaest.react4j.component.listview.internal.ListViewImpl;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.component.AccordionImpl;
import org.omnaest.react4j.service.internal.component.AlertImpl;
import org.omnaest.react4j.service.internal.component.BadgeImpl;
import org.omnaest.react4j.service.internal.component.BlockQuoteImpl;
import org.omnaest.react4j.service.internal.component.BreadcrumbImpl;
import org.omnaest.react4j.service.internal.component.ButtonImpl;
import org.omnaest.react4j.service.internal.component.CardImpl;
import org.omnaest.react4j.service.internal.component.CarouselImpl;
import org.omnaest.react4j.service.internal.component.CollapseImpl;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.react4j.service.internal.component.CompositeImpl;
import org.omnaest.react4j.service.internal.component.DropdownImpl;
import org.omnaest.react4j.service.internal.component.GridContainerImpl;
import org.omnaest.react4j.service.internal.component.HeadingImpl;
import org.omnaest.react4j.service.internal.component.IFrameImpl;
import org.omnaest.react4j.service.internal.component.IconImpl;
import org.omnaest.react4j.service.internal.component.ImageImpl;
import org.omnaest.react4j.service.internal.component.ImageIndexImpl;
import org.omnaest.react4j.service.internal.component.IntervalRerenderingContainerImpl;
import org.omnaest.react4j.service.internal.component.JumbotronImpl;
import org.omnaest.react4j.service.internal.component.LineBreakImpl;
import org.omnaest.react4j.service.internal.component.ModalImpl;
import org.omnaest.react4j.service.internal.component.NativeHtmlImpl;
import org.omnaest.react4j.service.internal.component.NavigationBarImpl;
import org.omnaest.react4j.service.internal.component.OffcanvasImpl;
import org.omnaest.react4j.service.internal.component.PaddingContainerImpl;
import org.omnaest.react4j.service.internal.component.PaginationImpl;
import org.omnaest.react4j.service.internal.component.ParagraphImpl;
import org.omnaest.react4j.service.internal.component.PlaceholderImpl;
import org.omnaest.react4j.service.internal.component.PopoverImpl;
import org.omnaest.react4j.service.internal.component.RatioContainerImpl;
import org.omnaest.react4j.service.internal.component.RerenderingContainerImpl;
import org.omnaest.react4j.service.internal.component.SVGContainerImpl;
import org.omnaest.react4j.service.internal.component.ScrollbarContainerImpl;
import org.omnaest.react4j.service.internal.component.SizedContainerImpl;
import org.omnaest.react4j.service.internal.component.SpinnerImpl;
import org.omnaest.react4j.service.internal.component.SplitButtonImpl;
import org.omnaest.react4j.service.internal.component.StackImpl;
import org.omnaest.react4j.service.internal.component.TabsImpl;
import org.omnaest.react4j.service.internal.component.TextImpl;
import org.omnaest.react4j.service.internal.component.ToasterImpl;
import org.omnaest.react4j.service.internal.component.ToggleButtonImpl;
import org.omnaest.react4j.service.internal.component.TooltipImpl;
import org.omnaest.react4j.service.internal.component.UnsortedListImpl;
import org.omnaest.react4j.service.internal.component.VerticalContentSwitcherImpl;
import org.omnaest.react4j.service.internal.service.NodeHierarchyStaticRenderer.NodeHierarchyRenderingProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * Goal-4 completeness meta-test (plan-74 Cliff C4 / G4-meta): enumerates every currently-registered
 * {@code UIComponentFactory} component that can be built and rendered bare (zero builder configuration, matching
 * each component's own {@code *ImplTest} convention - a mocked {@link ComponentContext}/{@link RenderingProcessor}/
 * {@link Location}, no mocks of the real rendering pipeline itself) and asserts every emitted node type has a
 * registered, NON-default HTML {@code NodeRenderer} via the new
 * {@link NodeHierarchyRenderingProcessor#registeredNodeTypes(NodeRenderType)} introspection hook.
 * <p>
 * <b>Ratchet, not a strict completeness gate (plan-74 F2 burn-down).</b> 16 of the 45 enumerated components
 * legitimately lack a registered HTML renderer today (see Finding 2c and {@link #KNOWN_MISSING_HTML_RENDERER});
 * fixing that drift is explicit fan-out follow-up (F2), not this pilot. This test therefore only fails the build
 * for a node type that is BOTH unregistered AND not present in {@link #KNOWN_MISSING_HTML_RENDERER} - i.e. any NEW
 * gap, never the pre-existing, tracked backlog. The full current drift set (allowlisted + any new regressions +
 * any allowlist entries that are now stale because their component gained a renderer) is logged once per run so
 * the backlog stays discoverable without failing the default build.
 * <p>
 * <b>F2 mechanical burn-down (this increment).</b> The STRUCTURAL/CONTENT/LAYOUT (non-interactive) subset - 18 of
 * the original 34 distinct drifting node types across 19 components ({@code Composite}, {@code PaddingContainer},
 * {@code RatioContainer}, {@code SizedContainer}, {@code ScrollbarContainer}, {@code Stack}, {@code LineBreak},
 * {@code Icon}, {@code Badge}, {@code Spinner}, {@code Placeholder}, {@code Alert}, {@code Breadcrumb},
 * {@code Pagination}, {@code ListView}, {@code IFrame}, {@code ImageIndex}, {@code NativeHtml}/{@code SVGContainer}
 * (raw-HTML passthrough, both emit {@code NATIVEHTML})) has now gained real registered HTML {@code NodeRenderer}s
 * and been REMOVED from {@link #KNOWN_MISSING_HTML_RENDERER}. The remaining 16 entries are all INTERACTIVE / stateful
 * client-behavior components ({@code Button}, {@code ToggleButton}, {@code Dropdown}, {@code SplitButton},
 * {@code Modal}, {@code Offcanvas}, {@code Tabs}, {@code Accordion}, {@code Carousel}, {@code Tooltip},
 * {@code Popover}, {@code Collapse}, {@code RerenderingContainer}, {@code IntervalRerenderingContainer},
 * {@code Toaster}, {@code VerticalContentSwitcher}) - deliberately DEFERRED, not forgotten, pending a design
 * decision on what a static/non-interactive rendering of client-side interaction should even mean (plan-74 F2
 * follow-up).
 * <p>
 * <b>Known enumeration gaps (excluded, not silently dropped):</b>
 * <ul>
 * <li>{@code Anker}, {@code AnkerButton}, {@code Table}, {@code Form} are constructed via
 * {@code CustomUIComponentFactoryManager} (Spring-wired), not a bare 1-arg constructor - out of reach of this plain
 * unit-test fixture.</li>
 * <li>{@code Figure}, {@code TextAlignmentContainer}, {@code ProgressBar} dereference an unguarded
 * {@code Optional.get()} on a child field that is only populated by a builder call - they throw
 * {@code NoSuchElementException} when rendered bare, unrelated to renderer registration. Excluded here; each would
 * need a minimally-configured (not bare) fixture to participate in this enumeration.</li>
 * <li>{@code MasterDetails}'s {@code asRenderer()} itself unconditionally builds a real child
 * {@code GridContainer} via {@code context.getUiComponentFactory().newGridContainer()} - it needs a working
 * {@code UIComponentFactory}, not just a mocked {@code ComponentContext}. Excluded here for the same reason.</li>
 * <li>Deprecated markdown factory methods return a {@code List} of components sourced from a file, not one
 * component - out of scope for a per-component node-type enumeration.</li>
 * </ul>
 *
 * @see NodeHierarchyStaticRenderer
 */
@Slf4j
public class NodeRendererCompletenessMetaTest
{
    /**
     * Burn-down backlog for plan-74 F2 (Goal-4 fan-out): the exact set of currently-drifting node-type STRINGS
     * (as emitted by {@link Node#getType()}, not component display names) that fall through to the lossy
     * {@code DefaultNodeRenderer} today (Finding 2c). This is a RATCHET, not a permanent exemption - entries here
     * must only ever be REMOVED as their component gains a real registered HTML {@code NodeRenderer}, never ADDED.
     * Any NEW node type missing a renderer (i.e. not present in this set) fails the build via
     * {@link #everyEmittedNodeTypeHasARegisteredHtmlRenderer(String, Supplier)}.
     * <p>
     * <b>Shrunk in this increment (34 -&gt; 16 distinct entries)</b> by the F2 mechanical (non-interactive)
     * burn-down: {@code BUTTON}, {@code COMPOSITE}, {@code IMAGEINDEX}, {@code SCROLLBARCONTAINER},
     * {@code LINEBREAK}, {@code ICON}, {@code PADDINGCONTAINER}, {@code IFRAMECONTAINER}, {@code NATIVEHTML},
     * {@code RATIOCONTAINER}, {@code SIZEDCONTAINER}, {@code LISTVIEW}, {@code BADGE}, {@code SPINNER},
     * {@code PLACEHOLDER}, {@code ALERT}, {@code BREADCRUMB}, {@code PAGINATION}, {@code STACK} were removed
     * (note {@code BUTTON} stays - it is interactive, not mechanical; only the 18 mechanical types were removed).
     * The remaining 16 entries are all INTERACTIVE / stateful-client-behavior components, deliberately DEFERRED
     * pending a design decision on what a static/non-interactive rendering of client-side interaction should mean
     * (plan-74 F2 follow-up) - NOT forgotten.
     */
    private static final Set<String>               KNOWN_MISSING_HTML_RENDERER = Set.of("BUTTON", "VERTICALCONTENTSWITCHER", "TOASTER",
                                                                                        "RERENDERINGCONTAINER", "INTERVALRERENDERINGCONTAINER", "TABS",
                                                                                        "ACCORDION", "MODAL", "OFFCANVAS", "TOOLTIP", "POPOVER", "COLLAPSE",
                                                                                        "TOGGLEBUTTON", "DROPDOWN", "SPLITBUTTON", "CAROUSEL");

    /**
     * The components that currently DO have a registered HTML renderer - kept as an explicit, independently
     * checked regression guard (see {@link #knownCoveredComponentsRemainCovered()}) so a future change that
     * silently drops one of these registrations is caught even though it would also be caught by the general
     * ratchet assertion. Extended in this increment with the 19 components covered by the F2 mechanical
     * (non-interactive) burn-down.
     */
    private static final Set<String>               KNOWN_COVERED_COMPONENTS    = Set.of("Paragraph", "BlockQuote", "Card", "GridContainer", "NavigationBar", "Image",
                                                                                        "Heading", "Jumbotron", "UnsortedList", "Text", "Composite",
                                                                                        "PaddingContainer", "RatioContainer", "SizedContainer",
                                                                                        "ScrollbarContainer", "Stack", "LineBreak", "Icon", "Badge", "Spinner",
                                                                                        "Placeholder", "Alert", "Breadcrumb", "Pagination", "ListView", "IFrame",
                                                                                        "ImageIndex", "NativeHtml", "SVGContainer");

    private static NodeHierarchyRenderingProcessor registeredRenderers;

    private static ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(org.omnaest.react4j.service.internal.service.LocalizedTextResolverService.class));
        return context;
    }

    @SuppressWarnings("unchecked")
    static Stream<Arguments> components()
    {
        return Stream.of(Arguments.of("Paragraph", (Supplier<RenderableUIComponent<?>>) () -> new ParagraphImpl(newContext())),
                         Arguments.of("Button", (Supplier<RenderableUIComponent<?>>) () -> new ButtonImpl(newContext())),
                         Arguments.of("BlockQuote", (Supplier<RenderableUIComponent<?>>) () -> new BlockQuoteImpl(newContext())),
                         Arguments.of("Card", (Supplier<RenderableUIComponent<?>>) () -> new CardImpl(newContext())),
                         Arguments.of("Composite", (Supplier<RenderableUIComponent<?>>) () -> new CompositeImpl(newContext())),
                         Arguments.of("GridContainer", (Supplier<RenderableUIComponent<?>>) () -> new GridContainerImpl(newContext())),
                         Arguments.of("NavigationBar", (Supplier<RenderableUIComponent<?>>) () -> new NavigationBarImpl(newContext())),
                         Arguments.of("Image", (Supplier<RenderableUIComponent<?>>) () -> new ImageImpl(newContext())),
                         Arguments.of("Heading", (Supplier<RenderableUIComponent<?>>) () -> new HeadingImpl(newContext())),
                         Arguments.of("Jumbotron", (Supplier<RenderableUIComponent<?>>) () -> new JumbotronImpl(newContext())),
                         Arguments.of("UnsortedList", (Supplier<RenderableUIComponent<?>>) () -> new UnsortedListImpl(newContext())),
                         Arguments.of("ImageIndex", (Supplier<RenderableUIComponent<?>>) () -> new ImageIndexImpl(newContext())),
                         Arguments.of("VerticalContentSwitcher",
                                      (Supplier<RenderableUIComponent<?>>) () -> new VerticalContentSwitcherImpl(newContext())),
                         Arguments.of("ScrollbarContainer", (Supplier<RenderableUIComponent<?>>) () -> new ScrollbarContainerImpl(newContext())),
                         Arguments.of("Text", (Supplier<RenderableUIComponent<?>>) () -> new TextImpl(newContext())),
                         Arguments.of("LineBreak", (Supplier<RenderableUIComponent<?>>) () -> new LineBreakImpl(newContext())),
                         Arguments.of("Toaster", (Supplier<RenderableUIComponent<?>>) () -> new ToasterImpl(newContext())),
                         Arguments.of("Icon", (Supplier<RenderableUIComponent<?>>) () -> new IconImpl(newContext())),
                         Arguments.of("PaddingContainer", (Supplier<RenderableUIComponent<?>>) () -> new PaddingContainerImpl(newContext())),
                         Arguments.of("RerenderingContainer", (Supplier<RenderableUIComponent<?>>) () -> new RerenderingContainerImpl(newContext())),
                         Arguments.of("IntervalRerenderingContainer",
                                      (Supplier<RenderableUIComponent<?>>) () -> new IntervalRerenderingContainerImpl(newContext())),
                         Arguments.of("IFrame", (Supplier<RenderableUIComponent<?>>) () -> new IFrameImpl(newContext())),
                         Arguments.of("NativeHtml", (Supplier<RenderableUIComponent<?>>) () -> new NativeHtmlImpl(newContext())),
                         Arguments.of("SVGContainer", (Supplier<RenderableUIComponent<?>>) () -> new SVGContainerImpl(newContext())),
                         Arguments.of("RatioContainer", (Supplier<RenderableUIComponent<?>>) () -> new RatioContainerImpl(newContext())),
                         Arguments.of("SizedContainer", (Supplier<RenderableUIComponent<?>>) () -> new SizedContainerImpl(newContext())),
                         Arguments.of("ListView", (Supplier<RenderableUIComponent<?>>) () -> new ListViewImpl(newContext())),
                         Arguments.of("Badge", (Supplier<RenderableUIComponent<?>>) () -> new BadgeImpl(newContext())),
                         Arguments.of("Spinner", (Supplier<RenderableUIComponent<?>>) () -> new SpinnerImpl(newContext())),
                         Arguments.of("Placeholder", (Supplier<RenderableUIComponent<?>>) () -> new PlaceholderImpl(newContext())),
                         Arguments.of("Alert", (Supplier<RenderableUIComponent<?>>) () -> new AlertImpl(newContext())),
                         Arguments.of("Breadcrumb", (Supplier<RenderableUIComponent<?>>) () -> new BreadcrumbImpl(newContext())),
                         Arguments.of("Pagination", (Supplier<RenderableUIComponent<?>>) () -> new PaginationImpl(newContext())),
                         Arguments.of("Stack", (Supplier<RenderableUIComponent<?>>) () -> new StackImpl(newContext())),
                         Arguments.of("Tabs", (Supplier<RenderableUIComponent<?>>) () -> new TabsImpl(newContext())),
                         Arguments.of("Accordion", (Supplier<RenderableUIComponent<?>>) () -> new AccordionImpl(newContext())),
                         Arguments.of("Modal", (Supplier<RenderableUIComponent<?>>) () -> new ModalImpl(newContext())),
                         Arguments.of("Offcanvas", (Supplier<RenderableUIComponent<?>>) () -> new OffcanvasImpl(newContext())),
                         Arguments.of("Tooltip", (Supplier<RenderableUIComponent<?>>) () -> new TooltipImpl(newContext())),
                         Arguments.of("Popover", (Supplier<RenderableUIComponent<?>>) () -> new PopoverImpl(newContext())),
                         Arguments.of("Collapse", (Supplier<RenderableUIComponent<?>>) () -> new CollapseImpl(newContext())),
                         Arguments.of("ToggleButton", (Supplier<RenderableUIComponent<?>>) () -> new ToggleButtonImpl(newContext())),
                         Arguments.of("Dropdown", (Supplier<RenderableUIComponent<?>>) () -> new DropdownImpl(newContext())),
                         Arguments.of("SplitButton", (Supplier<RenderableUIComponent<?>>) () -> new SplitButtonImpl(newContext())),
                         Arguments.of("Carousel", (Supplier<RenderableUIComponent<?>>) () -> new CarouselImpl(newContext())));
    }

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void collectAllNodeRenderers()
    {
        registeredRenderers = new NodeHierarchyStaticRenderer().newNodeRenderingProcessor();
        components().forEach(arguments ->
        {
            Supplier<RenderableUIComponent<?>> supplier = (Supplier<RenderableUIComponent<?>>) arguments.get()[1];
            supplier.get()
                    .asRenderer()
                    .manageNodeRenderers(registeredRenderers);
        });

        logDriftSet();
    }

    private static String emittedNodeType(Supplier<RenderableUIComponent<?>> supplier)
    {
        Node node = supplier.get()
                            .asRenderer()
                            .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());
        return node.getType();
    }

    /**
     * Logs the complete drift picture ONCE per run (plan-74 F2 requirement: keep the full drift set observable
     * even though only NEW gaps fail the build):
     * <ul>
     * <li>node types still missing a renderer AND allowlisted (the tracked, non-failing backlog)</li>
     * <li>node types missing a renderer and NOT allowlisted (would fail the build as a new regression)</li>
     * <li>allowlisted node types that unexpectedly now DO have a renderer (stale allowlist entries, safe to
     * remove - the backlog can shrink)</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private static void logDriftSet()
    {
        Set<String> registeredHtmlTypes = registeredRenderers.registeredNodeTypes(NodeRenderType.HTML);

        List<String> stillDrifting = new ArrayList<>();
        List<String> newRegressions = new ArrayList<>();
        List<String> staleAllowlistEntries = new ArrayList<>();

        components().forEach(arguments ->
        {
            String componentName = (String) arguments.get()[0];
            Supplier<RenderableUIComponent<?>> supplier = (Supplier<RenderableUIComponent<?>>) arguments.get()[1];
            String nodeType = emittedNodeType(supplier);
            boolean isRegistered = registeredHtmlTypes.contains(nodeType);
            boolean isAllowlisted = KNOWN_MISSING_HTML_RENDERER.contains(nodeType);
            String entry = componentName + "='" + nodeType + "'";

            if (!isRegistered && isAllowlisted)
            {
                stillDrifting.add(entry);
            }
            else if (!isRegistered && !isAllowlisted)
            {
                newRegressions.add(entry);
            }
            else if (isRegistered && isAllowlisted)
            {
                staleAllowlistEntries.add(entry);
            }
        });

        log.info("[plan-74 F2 backlog] {} component(s) still drifting (allowlisted, non-failing): {}", stillDrifting.size(), stillDrifting);
        if (!staleAllowlistEntries.isEmpty())
        {
            log.info("[plan-74 F2 backlog] {} allowlisted component(s) now HAVE a registered HTML NodeRenderer - safe to remove from "
                     + "KNOWN_MISSING_HTML_RENDERER: {}", staleAllowlistEntries.size(), staleAllowlistEntries);
        }
        if (!newRegressions.isEmpty())
        {
            log.warn("[plan-74 F2 backlog] {} component(s) are missing a registered HTML NodeRenderer and are NOT allowlisted - these "
                     + "are NEW regressions and will fail the build: {}", newRegressions.size(), newRegressions);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("components")
    public void everyEmittedNodeTypeHasARegisteredHtmlRenderer(String componentName, Supplier<RenderableUIComponent<?>> supplier)
    {
        String nodeType = emittedNodeType(supplier);

        if (KNOWN_MISSING_HTML_RENDERER.contains(nodeType))
        {
            // Tracked plan-74 F2 backlog entry: known, pre-existing drift - not a regression, does not fail the
            // build. If this node type has meanwhile gained a real renderer, logDriftSet() surfaces that as a
            // "safe to remove from the allowlist" candidate instead of failing here.
            return;
        }

        assertTrue(registeredRenderers.registeredNodeTypes(NodeRenderType.HTML)
                                      .contains(nodeType),
                   () -> componentName + " emits node type '" + nodeType
                         + "' with NO registered HTML NodeRenderer and is NOT in KNOWN_MISSING_HTML_RENDERER - this is a NEW gap "
                         + "(plan-74 ratchet: existing drift is allowlisted, new drift fails the build)");
    }

    /**
     * Explicit regression guard (plan-74 ratchet requirement 2): the 10 components that are registered today must
     * never silently lose their registration. This is already implied by
     * {@link #everyEmittedNodeTypeHasARegisteredHtmlRenderer(String, Supplier)} (none of these 10 node types are
     * allowlisted, so a regression there already fails the build), but is asserted independently here so the
     * intent is unambiguous and traceable to its own test method.
     */
    @Test
    public void knownCoveredComponentsRemainCovered()
    {
        Set<String> registeredHtmlTypes = registeredRenderers.registeredNodeTypes(NodeRenderType.HTML);

        @SuppressWarnings("unchecked")
        List<String> regressed = components().filter(arguments -> KNOWN_COVERED_COMPONENTS.contains((String) arguments.get()[0]))
                                             .map(arguments ->
                                             {
                                                 String componentName = (String) arguments.get()[0];
                                                 Supplier<RenderableUIComponent<?>> supplier = (Supplier<RenderableUIComponent<?>>) arguments.get()[1];
                                                 String nodeType = emittedNodeType(supplier);
                                                 return registeredHtmlTypes.contains(nodeType) ? null
                                                         : componentName + " emits node type '" + nodeType + "' which is NO LONGER registered";
                                             })
                                             .filter(Objects::nonNull)
                                             .collect(Collectors.toList());

        assertTrue(regressed.isEmpty(), () -> "Previously-covered component(s) regressed to having NO registered HTML NodeRenderer: " + regressed);
    }
}
