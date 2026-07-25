package org.omnaest.react4j.component.treetable.internal.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.treetable.internal.data.TreeTableData;
import org.omnaest.react4j.component.treetable.internal.renderer.node.TreeTableNode;
import org.omnaest.react4j.component.treetable.internal.renderer.node.TreeTableNode.RowEntryNode;
import org.omnaest.react4j.component.treetable.provider.ColumnFilter;
import org.omnaest.react4j.component.treetable.provider.InMemoryTreeTableDataProvider;
import org.omnaest.react4j.component.treetable.provider.InMemoryTreeTableDataProvider.Node;
import org.omnaest.react4j.component.treetable.provider.SortColumn;
import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.component.treetable.provider.TreeTableDataProvider;
import org.omnaest.react4j.component.treetable.provider.TreeTableQuery;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.ParentLocationAndComponent;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.react4j.service.internal.component.RerenderingContainerImpl;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.nodes.RerenderingContainerNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Integration test for {@link TreeTableRendererImpl} (plan-76 Slice 2): real {@link InMemoryTreeTableDataProvider} + a
 * hand-rolled real (non-bare-mock) {@link RenderingProcessor} that replicates the essential Location-composition contract
 * of {@code RenderingProcessorImpl} (getLocation() first, then render() with that resolved Location) — react4j-core-components
 * cannot depend on react4j-core's {@code RenderingProcessorImpl}, and a bare {@code mock(RenderingProcessor.class)} only
 * captures the parent-side Location (see {@code react4j-bare-mock-renderingprocessor-captures-parent-location-only}), which
 * would hide the exact per-row Location bug this test exists to catch.
 *
 * @see TreeTableRendererImpl
 * @author omnaest
 */
public class TreeTableRendererImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    private UIComponentFactory newFactory(ComponentContext context)
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        when(factory.newRerenderingContainer()).thenAnswer(invocation -> new RerenderingContainerImpl(context));
        return factory;
    }

    /**
     * Real (non-bare-mock) recursive processor: replicates {@code RenderingProcessorImpl.createComponentRenderer}'s
     * contract of resolving {@code getLocation()} first and passing that SAME Location into {@code render(...)}.
     */
    private RenderingProcessor realRenderingProcessor()
    {
        return new RenderingProcessor() {
            @Override
            public org.omnaest.react4j.domain.raw.Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data)
            {
                RenderableUIComponent<?> renderable = (RenderableUIComponent<?>) component;
                UIComponentRenderer renderer = renderable.asRenderer();
                LocationSupport locationSupport = new LocationSupport() {
                    @Override
                    public Location getParentLocation()
                    {
                        return parentLocation;
                    }

                    @Override
                    public Location createLocation(String id)
                    {
                        return Optional.ofNullable(parentLocation)
                                       .orElse(Location.empty())
                                       .and(id);
                    }
                };
                Location location = renderer.getLocation(locationSupport);
                return renderer.render(this, location, data);
            }
        };
    }

    private LocationSupport rootLocationSupport()
    {
        return new LocationSupport() {
            @Override
            public Location getParentLocation()
            {
                return null;
            }

            @Override
            public Location createLocation(String id)
            {
                return Location.of(id);
            }
        };
    }

    private RerenderingContainerNode render(TreeTableData data)
    {
        return this.render(data, null);
    }

    /**
     * @param submittedData
     *            simulates the Data a {@code POST /ui/event} round trip would carry (Cliff C1a mechanism (a)) - the
     *            SAME parameter the real event round trip threads through {@code EventHandlerServiceImpl}'s
     *            post-handler render pass.
     */
    private RerenderingContainerNode render(TreeTableData data, Data submittedData)
    {
        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);

        Location location = renderer.getLocation(this.rootLocationSupport());
        org.omnaest.react4j.domain.raw.Node node = renderer.render(this.realRenderingProcessor(), location, Optional.ofNullable(submittedData));
        return (RerenderingContainerNode) node;
    }

    private TreeTableNode content(RerenderingContainerNode containerNode)
    {
        return (TreeTableNode) containerNode.getContent();
    }

    private LocationSupport locationSupportFor(Location parentLocation)
    {
        return new LocationSupport() {
            @Override
            public Location getParentLocation()
            {
                return parentLocation;
            }

            @Override
            public Location createLocation(String id)
            {
                return Optional.ofNullable(parentLocation)
                               .orElse(Location.empty())
                               .and(id);
            }
        };
    }

    @Test
    public void testRootFetchProducesFlattenedRowsInOrderWithColumnTitles()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(
                                                                                                 new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"),
                                                                                                          Arrays.asList(new Node("b1", Map.of("name", "Beta Child"), Collections.emptyList())))),
                                                                                   true);

        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        assertEquals("TREETABLE", treeTableNode.getType());
        assertEquals(1, treeTableNode.getColumns()
                                     .size());
        assertEquals("name", treeTableNode.getColumns()
                                          .get(0)
                                          .getKey());
        assertEquals("Name", treeTableNode.getColumns()
                                          .get(0)
                                          .getTitle());

        List<RowEntryNode> rows = treeTableNode.getRows();
        assertEquals(2, rows.size());

        RowEntryNode rowA = rows.get(0);
        assertEquals("a", rowA.getNodeId());
        assertEquals("Alpha", rowA.getCells()
                                  .get("name"));
        assertFalse(rowA.isExpandable());
        assertFalse(rowA.isExpanded());

        RowEntryNode rowB = rows.get(1);
        assertEquals("b", rowB.getNodeId());
        assertEquals("Beta", rowB.getCells()
                                 .get("name"));
        assertTrue(rowB.isExpandable(), "node with children must be marked expandable");
        assertFalse(rowB.isExpanded(), "expanded must default to false and be present on every row (Cliff C5)");
    }

    @Test
    public void testPerRowPositionalLocationIsDistinctAndDeterministic()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"), Collections.emptyList())),
                                                                                   true);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        List<RowEntryNode> firstRenderRows = this.content(this.render(data))
                                                 .getRows();
        List<RowEntryNode> secondRenderRows = this.content(this.render(data))
                                                  .getRows();

        List<String> row0TargetFirst = firstRenderRows.get(0)
                                                      .getTarget()
                                                      .get();
        List<String> row1TargetFirst = firstRenderRows.get(1)
                                                      .getTarget()
                                                      .get();
        assertNotEquals(row0TargetFirst, row1TargetFirst, "row0 and row1 must resolve to distinct positional Locations");
        assertEquals("row0", row0TargetFirst.get(row0TargetFirst.size() - 1));
        assertEquals("row1", row1TargetFirst.get(row1TargetFirst.size() - 1));

        List<String> row0TargetSecond = secondRenderRows.get(0)
                                                        .getTarget()
                                                        .get();
        List<String> row1TargetSecond = secondRenderRows.get(1)
                                                        .getTarget()
                                                        .get();
        assertEquals(row0TargetFirst, row0TargetSecond, "row0's Location must be deterministic across renders");
        assertEquals(row1TargetFirst, row1TargetSecond, "row1's Location must be deterministic across renders");
    }

    @Test
    public void testLoadMoreOfferedWhenFullWindowReturnedAndTotalCountAbsent()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(1)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        assertEquals(1, treeTableNode.getRows()
                                     .size(),
                     "windowSize=1 must cap the returned rows to a single row");
        assertTrue(treeTableNode.getLoadMore()
                                .isAvailable(),
                   "a full window (rows.size() == limit) with no totalChildCount must offer load-more");
        assertEquals(1, treeTableNode.getLoadMore()
                                     .getNextOffset());
        assertNull(treeTableNode.getLoadMore()
                                .getTotalCount());
        assertFalse(treeTableNode.getLoadMore()
                                 .getTarget()
                                 .isEmpty(),
                    "an available load-more must carry a routable, non-empty Target (Slice 4)");
        assertEquals("loadmore",
                     treeTableNode.getLoadMore()
                                  .getTarget()
                                  .get()
                                  .get(treeTableNode.getLoadMore()
                                                    .getTarget()
                                                    .get()
                                                    .size()
                                       - 1));
    }

    @Test
    public void testLoadMoreDisabledWhenTotalCountReachedByWindow()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"), Collections.emptyList())),
                                                                                   true);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        assertFalse(treeTableNode.getLoadMore()
                                 .isAvailable(),
                    "totalChildCount reached by the window must disable load-more even though the provider supports counting");
        assertEquals(2L, treeTableNode.getLoadMore()
                                      .getTotalCount());
        assertTrue(treeTableNode.getLoadMore()
                                .getTarget()
                                .isEmpty(),
                   "an unavailable load-more must NOT carry a routable Target - nothing left to click");
    }

    @Test
    public void testSubmittedDataWindowFieldWidensSameRenderRowWindow()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "A"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "B"), Collections.emptyList()),
                                                                                                 new Node("c", Map.of("name", "C"), Collections.emptyList()),
                                                                                                 new Node("d", Map.of("name", "D"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(2)
                                          .build();

        TreeTableNode firstRender = this.content(this.render(data));
        assertEquals(2, firstRender.getRows()
                                   .size(),
                     "default window must cap the first render to windowSize rows");

        List<String> loadMoreTargetPath = firstRender.getLoadMore()
                                                     .getTarget()
                                                     .get();
        String windowFieldKey = "treetable." + loadMoreTargetPath.subList(0, loadMoreTargetPath.size() - 1)
                                                                 .stream()
                                                                 .collect(Collectors.joining("."))
                                + ".root.windowLimit";

        Data submittedData = Data.newInstance()
                                 .setFieldValue(windowFieldKey, 4);

        // This is the exact mechanism (a) read path (Cliff C1a): TreeTableGridComponent.buildNode(...) derives the
        // query's limit from the SAME submitted Data object a POST /ui/event's post-handler render pass threads
        // through - see TreeTableLoadMoreEndToEndTest (react4j-core) for the full cross-tier proof.
        TreeTableNode secondRender = this.content(this.render(data, submittedData));
        assertEquals(4, secondRender.getRows()
                                    .size(),
                     "a submitted Data window field must widen the SAME re-render's row window with no round-trip lag");
    }

    @Test
    public void testLoadMoreSubComponentIsDiscoverableAtTheTargetsLocationAndRegistersItsOwnHandler()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(1)
                                          .build();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());

        // Level 1 (RerenderingContainer -> grid content), mirrors ReactUIServiceImpl's real registration walk.
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation)
                                                                          .collect(Collectors.toList());
        assertEquals(1, containerSubComponents.size());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));

        // Level 2 (grid -> load-more control) - only discoverable via THIS traversal (Slice 4 addition).
        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        assertEquals(4, gridSubComponents.size(),
                     "load-more + filter-toggle (plan-77) + one filter control + one sort toggle (Slice 6, 1 column) must be discoverable for handler registration");
        ParentLocationAndComponent loadMoreEntry = gridSubComponents.get(0);
        RenderableUIComponent<?> loadMoreComponent = (RenderableUIComponent<?>) loadMoreEntry.getComponent();
        Location loadMoreLocation = loadMoreComponent.asRenderer()
                                                     .getLocation(this.locationSupportFor(loadMoreEntry.getParentLocation()));

        TreeTableNode treeTableNode = this.content(this.render(data));
        assertEquals(treeTableNode.getLoadMore()
                                  .getTarget()
                                  .get(),
                     loadMoreLocation.get(),
                     "the discovered sub-component's Location must match the Target embedded in the rendered loadMore node (trap #4)");

        List<DataEventHandler> registeredHandlers = new ArrayList<>();
        EventHandlerRegistrationSupport capturingSupport = new EventHandlerRegistrationSupport() {
            @Override
            public EventHandlerRegistrationSupport register(EventHandler eventHandler)
            {
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport register(DataEventHandler eventHandler)
            {
                registeredHandlers.add(eventHandler);
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport registerAsRerenderingNode()
            {
                return this;
            }
        };
        loadMoreComponent.asRenderer()
                         .manageEventHandler(capturingSupport);
        assertEquals(1, registeredHandlers.size(), "the load-more control must register exactly one DataEventHandler");

        Data invokedData = Data.newInstance();
        registeredHandlers.get(0)
                          .invoke(invokedData, Data.newInstance());
        assertNotNull(invokedData.getFieldValue("treetable." + gridLocation.get()
                                                                           .stream()
                                                                           .collect(Collectors.joining("."))
                                                + ".root.windowLimit"),
                      "invoking the registered handler must advance the window field keyed by the grid's own Location");
    }

    // ------------------------------------------------------------------------------------------------------------
    // Slice 5 - expand/collapse + child fetch
    // ------------------------------------------------------------------------------------------------------------

    private String gridLocationJoinedFor(RerenderingContainerNode containerNode)
    {
        // A row's `target` field is ALWAYS populated (Slice 2), regardless of load-more availability, so deriving
        // the grid's own Location from row 0's target is reliable even when the root loadMore is unavailable
        // (unlike deriving it from the loadMore target, which is Target.empty() when not available).
        List<String> rowTargetPath = this.content(containerNode)
                                         .getRows()
                                         .get(0)
                                         .getTarget()
                                         .get();
        return rowTargetPath.subList(0, rowTargetPath.size() - 1)
                            .stream()
                            .collect(Collectors.joining("."));
    }

    private String expandedNodesFieldKeyFor(RerenderingContainerNode containerNode)
    {
        return "treetable." + this.gridLocationJoinedFor(containerNode) + ".expandedNodes";
    }

    private Data expandedData(RerenderingContainerNode containerNode, String... expandedNodeIds)
    {
        return Data.newInstance()
                   .setFieldValue(this.expandedNodesFieldKeyFor(containerNode), Arrays.asList(expandedNodeIds));
    }

    @Test
    public void testExpandedRowFetchesChildrenFlattenedAtDepthPlusOneWithOwnLoadMore()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(
                                                                                                 new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"),
                                                                                                          Arrays.asList(new Node("b1", Map.of("name", "Beta One"), Collections.emptyList()),
                                                                                                                        new Node("b2", Map.of("name", "Beta Two"), Collections.emptyList())))),
                                                                                   true);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        Data submittedData = this.expandedData(baseline, "b");

        TreeTableNode treeTableNode = this.content(this.render(data, submittedData));
        List<RowEntryNode> rows = treeTableNode.getRows();

        assertEquals(4, rows.size(), "row a, row b (expanded), and its two flattened children");
        assertEquals("a", rows.get(0)
                              .getNodeId());
        assertEquals(0, rows.get(0)
                            .getDepth());

        RowEntryNode rowB = rows.get(1);
        assertEquals("b", rowB.getNodeId());
        assertEquals(0, rowB.getDepth());
        assertTrue(rowB.isExpanded(), "row b must be expanded per the submitted expanded-node-id set");
        assertNotNull(rowB.getChildLoadMore(), "an expanded row must carry its own childLoadMore descriptor");
        assertFalse(rowB.getChildLoadMore()
                        .isAvailable(),
                    "2 children within a 500-row default window must not offer load-more");
        assertEquals(2L, rowB.getChildLoadMore()
                             .getTotalCount());

        RowEntryNode rowB1 = rows.get(2);
        assertEquals("b1", rowB1.getNodeId());
        assertEquals(1, rowB1.getDepth(), "children of an expanded row must render at depth + 1");
        assertFalse(rowB1.isExpanded());
        assertNull(rowB1.getChildLoadMore(), "a collapsed row must not carry a childLoadMore descriptor");

        RowEntryNode rowB2 = rows.get(3);
        assertEquals("b2", rowB2.getNodeId());
        assertEquals(1, rowB2.getDepth());

        assertNotEquals(rowB.getTarget()
                            .get(),
                        rowB1.getTarget()
                             .get(),
                        "a child row's Target must be distinct from its parent's (trap #4)");
    }

    @Test
    public void testCollapsingRowRemovesItsChildrenFromTheFlattenedList()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"),
                                                                                                          Arrays.asList(new Node("a1", Map.of("name", "Alpha One"), Collections.emptyList())))),
                                                                                   true);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);

        TreeTableNode expandedNode = this.content(this.render(data, this.expandedData(baseline, "a")));
        assertEquals(2, expandedNode.getRows()
                                    .size(),
                     "row a plus its one flattened child while expanded");

        TreeTableNode collapsedNode = this.content(this.render(data, this.expandedData(baseline)));
        assertEquals(1, collapsedNode.getRows()
                                     .size(),
                     "an empty expanded-node-id set must collapse row a and remove its child from the flattened list");
        assertFalse(collapsedNode.getRows()
                                 .get(0)
                                 .isExpanded());
    }

    @Test
    public void testNestedExpansionRecursesToGrandchildrenAtDepthTwo()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"),
                                                                                                          Arrays.asList(new Node("b", Map.of("name", "Beta"),
                                                                                                                                 Arrays.asList(new Node("c", Map.of("name", "Gamma"),
                                                                                                                                                        Collections.emptyList())))))),
                                                                                   true);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        Data submittedData = this.expandedData(baseline, "a", "b");

        TreeTableNode treeTableNode = this.content(this.render(data, submittedData));
        List<RowEntryNode> rows = treeTableNode.getRows();

        assertEquals(3, rows.size(), "a (expanded) -> b (expanded, its own child) -> c (grandchild)");
        assertEquals("a", rows.get(0)
                              .getNodeId());
        assertEquals(0, rows.get(0)
                            .getDepth());
        assertTrue(rows.get(0)
                       .isExpanded());

        RowEntryNode rowB = rows.get(1);
        assertEquals("b", rowB.getNodeId());
        assertEquals(1, rowB.getDepth());
        assertTrue(rowB.isExpanded(), "a child that is itself in the expanded-node-id set must also be expanded");
        assertNotNull(rowB.getChildLoadMore());

        RowEntryNode rowC = rows.get(2);
        assertEquals("c", rowC.getNodeId());
        assertEquals(2, rowC.getDepth(), "a grandchild of a nested-expanded node must render at depth 2");
    }

    @Test
    public void testChildGroupWindowIsIndependentFromRootWindow()
    {
        List<Node> manyChildren = new ArrayList<>();
        for (int i = 0; i < 3; i++)
        {
            manyChildren.add(new Node("b" + i, Map.of("name", "Beta " + i), Collections.emptyList()));
        }
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"), manyChildren)),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(1)
                                          .build();

        // Baseline (component default windowSize=1) roots only "a" (root window not yet widened); derive the grid's
        // own Location and explicitly widen ONLY the ROOT window field to 2 (via the SAME field-key mechanism
        // load-more uses) while marking "b" expanded, WITHOUT ever touching b's own child-window field - proving
        // the two fields never couple: b's children must still be capped at the component default (1).
        RerenderingContainerNode baseline = this.render(data);
        String gridLocationJoined = this.gridLocationJoinedFor(baseline);
        String rootWindowFieldKey = "treetable." + gridLocationJoined + ".root.windowLimit";
        String expandedFieldKey = "treetable." + gridLocationJoined + ".expandedNodes";
        Data submittedData = Data.newInstance()
                                 .setFieldValue(rootWindowFieldKey, 2)
                                 .setFieldValue(expandedFieldKey, Arrays.asList("b"));

        TreeTableNode treeTableNode = this.content(this.render(data, submittedData));

        assertEquals(2, treeTableNode.getRows()
                                     .stream()
                                     .filter(row -> row.getDepth() == 0)
                                     .count(),
                     "explicitly widening ONLY the root window field to 2 must cap ROOT rows to 2 (both a and b)");
        RowEntryNode rowB = treeTableNode.getRows()
                                         .stream()
                                         .filter(row -> "b".equals(row.getNodeId()))
                                         .findFirst()
                                         .get();
        assertTrue(rowB.getChildLoadMore()
                       .isAvailable(),
                   "b's OWN child window must stay at the component default (1), unaffected by widening the ROOT window field");
        assertEquals(1, treeTableNode.getRows()
                                     .stream()
                                     .filter(row -> row.getDepth() == 1)
                                     .count(),
                     "b's child window (component default 1) must cap the flattened grandchild rows to 1, independent of the widened root window");
    }

    @Test
    public void testOnlyExpandableRowsGetAnExpandControlSubComponentAndEachRowsIsDistinct()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"),
                                                                                                          Arrays.asList(new Node("b1", Map.of("name", "Beta One"), Collections.emptyList())))),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());

        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation)
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));

        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());

        // 1 root load-more + 1 filter-toggle (plan-77) + 1 filter control + 1 sort toggle (Slice 6, 1 column) + 1
        // expand control for row "b" only ("a" is not expandable and gets no control).
        assertEquals(5, gridSubComponents.size(),
                     "load-more + filter-toggle + filter + sort + exactly one expand control (row b) must be discoverable, no expand control for row a");

        List<DataEventHandler> registeredHandlers = new ArrayList<>();
        EventHandlerRegistrationSupport capturingSupport = new EventHandlerRegistrationSupport() {
            @Override
            public EventHandlerRegistrationSupport register(EventHandler eventHandler)
            {
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport register(DataEventHandler eventHandler)
            {
                registeredHandlers.add(eventHandler);
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport registerAsRerenderingNode()
            {
                return this;
            }
        };
        for (ParentLocationAndComponent entry : gridSubComponents)
        {
            ((RenderableUIComponent<?>) entry.getComponent()).asRenderer()
                                                             .manageEventHandler(capturingSupport);
        }
        assertEquals(5, registeredHandlers.size(), "load-more, filter-toggle, filter, sort, and row b's expand control must each register exactly one handler");

        // Invoking row b's expand handler must toggle "b" into the shared expandedNodes field for this grid.
        Data invokedData = Data.newInstance();
        DataEventHandler expandHandler = registeredHandlers.get(4);
        expandHandler.invoke(invokedData, Data.newInstance());
        String expandedFieldKey = "treetable." + gridLocation.get()
                                                             .stream()
                                                             .collect(Collectors.joining("."))
                                  + ".expandedNodes";
        assertEquals(Arrays.asList("b"),
                     invokedData.getFieldValue(expandedFieldKey)
                                .get()
                                .asStringList(),
                     "invoking row b's registered expand handler must toggle nodeId 'b' into the shared expandedNodes field");
    }

    @Test
    public void testChildGroupLoadMoreHasItsOwnDistinctTargetAndTheChildFetchIsScopedToItsParentNodeId()
    {
        List<Node> manyChildren = new ArrayList<>();
        for (int i = 0; i < 3; i++)
        {
            manyChildren.add(new Node("b" + i, Map.of("name", "Beta " + i), Collections.emptyList()));
        }
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("b", Map.of("name", "Beta"), manyChildren)), false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(trackingProvider)
                                          .windowSize(1)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        Data submittedData = this.expandedData(baseline, "b");
        recordedQueries.clear();

        TreeTableNode treeTableNode = this.content(this.render(data, submittedData));

        // AC3 "child fetch scoped to parentNodeId": exactly one recorded query must target parentNodeId "b".
        List<TreeTableQuery> childScopedQueries = recordedQueries.stream()
                                                                 .filter(query -> query.getParentNodeId()
                                                                                       .map("b"::equals)
                                                                                       .orElse(false))
                                                                 .collect(Collectors.toList());
        assertEquals(1, childScopedQueries.size(), "the render must issue exactly one query scoped to parentNodeId 'b'");
        assertEquals(0L, childScopedQueries.get(0)
                                           .getOffset());

        RowEntryNode rowB = treeTableNode.getRows()
                                         .get(0);
        assertEquals("b", rowB.getNodeId());
        assertTrue(rowB.isExpanded());
        assertNotNull(rowB.getChildLoadMore());
        assertTrue(rowB.getChildLoadMore()
                       .isAvailable(),
                   "3 children with a 1-row child window must offer load-more");
        List<String> childLoadMoreTarget = rowB.getChildLoadMore()
                                               .getTarget()
                                               .get();
        assertEquals("loadmore", childLoadMoreTarget.get(childLoadMoreTarget.size() - 1));
        assertNotEquals(childLoadMoreTarget,
                        treeTableNode.getLoadMore()
                                     .getTarget()
                                     .get(),
                        "the child group's own load-more Target must be distinct from the root group's load-more Target");
    }

    // ------------------------------------------------------------------------------------------------------------
    // Slice 6 - per-column filter + multi-column sort
    // ------------------------------------------------------------------------------------------------------------

    private String sortFieldKeyFor(RerenderingContainerNode containerNode)
    {
        return "treetable." + this.gridLocationJoinedFor(containerNode) + ".sort";
    }

    private Data sortedData(RerenderingContainerNode containerNode, String... encodedSortEntries)
    {
        return Data.newInstance()
                   .setFieldValue(this.sortFieldKeyFor(containerNode), Arrays.asList(encodedSortEntries));
    }

    // ------------------------------------------------------------------------------------------------------------
    // plan-77 - collapsible filter row (server-owned filtersVisible + funnel toggle)
    // ------------------------------------------------------------------------------------------------------------

    private String filtersVisibleFieldKeyFor(RerenderingContainerNode containerNode)
    {
        return "treetable." + this.gridLocationJoinedFor(containerNode) + ".filtersVisible";
    }

    @Test
    public void testFiltersVisibleDefaultsToFalse()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        assertFalse(treeTableNode.isFiltersVisible(), "filtersVisible must default to false (collapsed) at a baseline (no submitted Data) render");
        assertEquals(0, treeTableNode.getActiveFilterCount(), "activeFilterCount must be 0 when no filter is active");
        assertFalse(treeTableNode.getFilterToggleTarget()
                                 .isEmpty(),
                    "the funnel toggle must carry a routable Target even at initial render (like the filter/sort header controls)");
    }

    @Test
    public void testTogglingFiltersVisibleFieldRendersItTrue()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        Data submittedData = Data.newInstance()
                                 .setFieldValue(this.filtersVisibleFieldKeyFor(baseline), true);

        TreeTableNode toggledNode = this.content(this.render(data, submittedData));
        assertTrue(toggledNode.isFiltersVisible(), "a submitted filtersVisible=true field must render filtersVisible=true with no round-trip lag");
    }

    @Test
    public void testActiveFilterCountCountsNonEmptySubmittedFilterValuesAcrossColumns()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "age", "30"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name"), TreeTableColumn.of("age", "Age")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        assertEquals(0, this.content(baseline)
                            .getActiveFilterCount(),
                     "no submitted filter values must count as 0 active filters");

        List<TreeTableNode.ColumnNode> baselineColumns = this.content(baseline)
                                                             .getColumns();
        String nameFilterFieldKey = baselineColumns.get(0)
                                                   .getFilterFieldKey();
        String ageFilterFieldKey = baselineColumns.get(1)
                                                  .getFilterFieldKey();

        TreeTableNode oneFilterActive = this.content(this.render(data,
                                                                 Data.newInstance()
                                                                     .setFieldValue(nameFilterFieldKey, "Al")));
        assertEquals(1, oneFilterActive.getActiveFilterCount(), "exactly one non-empty submitted filter value must count as 1 active filter");

        TreeTableNode twoFiltersActive = this.content(this.render(data,
                                                                  Data.newInstance()
                                                                      .setFieldValue(nameFilterFieldKey, "Al")
                                                                      .setFieldValue(ageFilterFieldKey, "30")));
        assertEquals(2, twoFiltersActive.getActiveFilterCount(), "two non-empty submitted filter values must count as 2 active filters");
    }

    @Test
    public void testFilterToggleSubComponentIsDiscoverableAtTheTargetsLocationAndRegistersItsOwnHandler()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation)
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));

        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // Order for a single non-expandable-row column grid: loadmore(0), filtertoggle(1), filter0(2), sort0(3).
        ParentLocationAndComponent filterToggleEntry = gridSubComponents.get(1);
        RenderableUIComponent<?> filterToggleComponent = (RenderableUIComponent<?>) filterToggleEntry.getComponent();
        Location filterToggleLocation = filterToggleComponent.asRenderer()
                                                             .getLocation(this.locationSupportFor(filterToggleEntry.getParentLocation()));

        TreeTableNode treeTableNode = this.content(this.render(data));
        assertEquals(treeTableNode.getFilterToggleTarget()
                                  .get(),
                     filterToggleLocation.get(),
                     "the discovered filter-toggle sub-component's Location must match the filterToggleTarget embedded in the rendered node (trap #4)");

        List<DataEventHandler> registeredHandlers = new ArrayList<>();
        EventHandlerRegistrationSupport capturingSupport = new EventHandlerRegistrationSupport() {
            @Override
            public EventHandlerRegistrationSupport register(EventHandler eventHandler)
            {
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport register(DataEventHandler eventHandler)
            {
                registeredHandlers.add(eventHandler);
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport registerAsRerenderingNode()
            {
                return this;
            }
        };
        filterToggleComponent.asRenderer()
                             .manageEventHandler(capturingSupport);
        assertEquals(1, registeredHandlers.size(), "the filter-toggle control must register exactly one DataEventHandler");

        Data invokedData = Data.newInstance();
        registeredHandlers.get(0)
                          .invoke(invokedData, Data.newInstance());
        assertTrue(invokedData.getFieldValue("treetable." + gridLocation.get()
                                                                        .stream()
                                                                        .collect(Collectors.joining("."))
                                             + ".filtersVisible")
                              .get()
                              .asBoolean(),
                   "invoking the registered handler must flip the filtersVisible field keyed by the grid's own Location");
    }

    @Test
    public void testFilterValuesSurviveAVisibilityToggle()
    {
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"), Collections.emptyList())),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(trackingProvider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        String filterFieldKey = this.content(baseline)
                                    .getColumns()
                                    .get(0)
                                    .getFilterFieldKey();
        String filtersVisibleFieldKey = this.filtersVisibleFieldKeyFor(baseline);

        // A filter is active AND the filter row is currently hidden (filtersVisible absent -> defaults false).
        Data filteredHiddenData = Data.newInstance()
                                      .setFieldValue(filterFieldKey, "Al");
        recordedQueries.clear();
        TreeTableNode filteredHiddenNode = this.content(this.render(data, filteredHiddenData));
        assertEquals(1, recordedQueries.get(0)
                                       .getFilters()
                                       .size(),
                     "the filter must be applied to the query even while filtersVisible is false");
        assertEquals(1, filteredHiddenNode.getRows()
                                          .size());
        assertFalse(filteredHiddenNode.isFiltersVisible());

        // Toggling filtersVisible to true on the SAME submitted Data (the filter value stays present, unchanged)
        // must NOT clear the active filter - it must still narrow the query/rows exactly as before.
        Data filteredVisibleData = Data.newInstance()
                                       .setFieldValue(filterFieldKey, "Al")
                                       .setFieldValue(filtersVisibleFieldKey, true);
        recordedQueries.clear();
        TreeTableNode filteredVisibleNode = this.content(this.render(data, filteredVisibleData));
        assertTrue(filteredVisibleNode.isFiltersVisible(), "filtersVisible must now render true");
        assertEquals(1, recordedQueries.get(0)
                                       .getFilters()
                                       .size(),
                     "toggling filtersVisible to true must NOT clear the active filter value - it must still be applied to the query");
        assertEquals("Al", recordedQueries.get(0)
                                          .getFilters()
                                          .get(0)
                                          .getValue());
        assertEquals(1, filteredVisibleNode.getRows()
                                           .size(),
                     "the filtered row set must remain narrowed exactly as before the visibility toggle");
        assertEquals("Al",
                     filteredVisibleNode.getColumns()
                                        .get(0)
                                        .getFilterValue(),
                     "the emitted filterValue must still echo the currently-active filter text after the toggle");
    }

    @Test
    public void testColumnNodesCarryFilterAndSortControlsAtInitialRenderWithNoActiveFilterOrSort()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));
        TreeTableNode.ColumnNode column = treeTableNode.getColumns()
                                                       .get(0);

        assertNull(column.getFilterValue(), "no filter must be active at a baseline (no submitted Data) render");
        assertNull(column.getSortDirection(), "no sort must be active at a baseline (no submitted Data) render");
        assertNotNull(column.getFilterFieldKey(), "the frontend must be told the deterministic field key to write the raw filter text into");
        assertFalse(column.getFilterTarget()
                          .isEmpty(),
                    "the filter control must carry a routable Target even at initial render (header controls register normally)");
        assertFalse(column.getSortTarget()
                          .isEmpty(),
                    "the sort toggle must carry a routable Target even at initial render");
        assertNotEquals(column.getFilterTarget()
                              .get(),
                        column.getSortTarget()
                              .get(),
                        "the filter control and the sort toggle must resolve to distinct positional Targets (trap #4)");
    }

    @Test
    public void testNonEmptyFilterValueBuildsOneContainsColumnFilterAndBlankValueBuildsNone()
    {
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"), Collections.emptyList())),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(trackingProvider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        String filterFieldKey = this.content(baseline)
                                    .getColumns()
                                    .get(0)
                                    .getFilterFieldKey();

        // Non-empty filter value -> exactly one CONTAINS ColumnFilter, narrowing the returned rows (AC4).
        Data filteredSubmittedData = Data.newInstance()
                                         .setFieldValue(filterFieldKey, "Al");
        recordedQueries.clear();
        TreeTableNode filteredNode = this.content(this.render(data, filteredSubmittedData));

        assertEquals(1, recordedQueries.size());
        assertEquals(1, recordedQueries.get(0)
                                       .getFilters()
                                       .size(),
                     "a non-empty filter value must build exactly one ColumnFilter");
        ColumnFilter builtFilter = recordedQueries.get(0)
                                                  .getFilters()
                                                  .get(0);
        assertEquals("name", builtFilter.getColumnKey());
        assertEquals(ColumnFilter.FilterOperator.CONTAINS, builtFilter.getOperator(), "CONTAINS is the chosen default FilterOperator for text columns");
        assertEquals("Al", builtFilter.getValue());
        assertEquals(1, filteredNode.getRows()
                                    .size(),
                     "the CONTAINS filter must narrow the returned rows to the matching row only");
        assertEquals("a", filteredNode.getRows()
                                      .get(0)
                                      .getNodeId());
        assertEquals("Al",
                     filteredNode.getColumns()
                                 .get(0)
                                 .getFilterValue(),
                     "the emitted filterValue must echo the currently-active filter text");

        // Blank/whitespace-only value -> NO filter at all (empty value -> no filter, per AC4).
        Data blankSubmittedData = Data.newInstance()
                                      .setFieldValue(filterFieldKey, "   ");
        recordedQueries.clear();
        TreeTableNode unfilteredNode = this.content(this.render(data, blankSubmittedData));
        assertEquals(0, recordedQueries.get(0)
                                       .getFilters()
                                       .size(),
                     "a blank/whitespace-only filter value must build NO ColumnFilter");
        assertEquals(2, unfilteredNode.getRows()
                                      .size(),
                     "with no active filter, both rows must be returned");
        assertNull(unfilteredNode.getColumns()
                                 .get(0)
                                 .getFilterValue(),
                   "a blank/whitespace-only filter value must be reported as no active filter");
    }

    @Test
    public void testSortToggleHandlerCyclesAscendingDescendingThenRemovesTheEntryFromTheOrderedSortField()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation)
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));

        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // Order for a single non-expandable-row column grid: loadmore(0), filtertoggle(1), filter0(2), sort0(3).
        RenderableUIComponent<?> sortToggleComponent = (RenderableUIComponent<?>) gridSubComponents.get(3)
                                                                                                   .getComponent();

        List<DataEventHandler> registeredHandlers = new ArrayList<>();
        EventHandlerRegistrationSupport capturingSupport = new EventHandlerRegistrationSupport() {
            @Override
            public EventHandlerRegistrationSupport register(EventHandler eventHandler)
            {
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport register(DataEventHandler eventHandler)
            {
                registeredHandlers.add(eventHandler);
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport registerAsRerenderingNode()
            {
                return this;
            }
        };
        sortToggleComponent.asRenderer()
                           .manageEventHandler(capturingSupport);
        assertEquals(1, registeredHandlers.size());
        DataEventHandler sortHandler = registeredHandlers.get(0);

        String sortFieldKey = "treetable." + gridLocation.get()
                                                         .stream()
                                                         .collect(Collectors.joining("."))
                              + ".sort";

        Data click1 = sortHandler.invoke(Data.newInstance(), Data.newInstance())
                                 .getData();
        assertEquals(Arrays.asList("name:ASCENDING"),
                     click1.getFieldValue(sortFieldKey)
                           .get()
                           .asStringList(),
                     "the first click must sort ASCENDING");

        Data click2 = sortHandler.invoke(click1, Data.newInstance())
                                 .getData();
        assertEquals(Arrays.asList("name:DESCENDING"),
                     click2.getFieldValue(sortFieldKey)
                           .get()
                           .asStringList(),
                     "the second click must flip the SAME entry to DESCENDING, keeping its position");

        Data click3 = sortHandler.invoke(click2, Data.newInstance())
                                 .getData();
        assertTrue(click3.getFieldValue(sortFieldKey)
                         .map(value -> value.asStringList())
                         .orElse(Collections.emptyList())
                         .isEmpty(),
                   "the third click must cycle off entirely (none) - ASCENDING -> DESCENDING -> none");
    }

    @Test
    public void testMultiColumnSortOrderedListIsBuiltWithIndexZeroAsPrimaryAndEmittedAsPerColumnSortDirectionIndicator()
    {
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "age", "30"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta", "age", "25"),
                                                                                                          Collections.emptyList())),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("age", "Age"), TreeTableColumn.of("name", "Name")))
                                          .dataProvider(trackingProvider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        Data submittedData = this.sortedData(baseline, "age:DESCENDING", "name:ASCENDING");

        recordedQueries.clear();
        TreeTableNode treeTableNode = this.content(this.render(data, submittedData));

        List<SortColumn> sorts = recordedQueries.get(0)
                                                .getSorts();
        assertEquals(2, sorts.size());
        assertEquals("age", sorts.get(0)
                                 .getColumnKey(),
                     "click order index 0 must be the PRIMARY sort column");
        assertEquals(SortColumn.SortDirection.DESCENDING, sorts.get(0)
                                                               .getDirection());
        assertEquals("name", sorts.get(1)
                                  .getColumnKey(),
                     "index 1 must be the SECONDARY sort column");
        assertEquals(SortColumn.SortDirection.ASCENDING, sorts.get(1)
                                                              .getDirection());

        TreeTableNode.ColumnNode ageColumn = treeTableNode.getColumns()
                                                          .stream()
                                                          .filter(column -> "age".equals(column.getKey()))
                                                          .findFirst()
                                                          .get();
        TreeTableNode.ColumnNode nameColumn = treeTableNode.getColumns()
                                                           .stream()
                                                           .filter(column -> "name".equals(column.getKey()))
                                                           .findFirst()
                                                           .get();
        assertEquals(SortColumn.SortDirection.DESCENDING, ageColumn.getSortDirection(),
                     "the emitted per-column sort-direction indicator must reflect the active primary sort");
        assertEquals(SortColumn.SortDirection.ASCENDING, nameColumn.getSortDirection(),
                     "the emitted per-column sort-direction indicator must reflect the active secondary sort");
    }

    @Test
    public void testFilterAndSortAreThreadedIntoBothRootAndExpandedChildGroupQueries()
    {
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"),
                                                                                                          Arrays.asList(new Node("b1", Map.of("name", "Beta One"),
                                                                                                                                 Collections.emptyList())))),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(trackingProvider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        String filterFieldKey = this.content(baseline)
                                    .getColumns()
                                    .get(0)
                                    .getFilterFieldKey();
        String expandedFieldKey = this.expandedNodesFieldKeyFor(baseline);
        String sortFieldKey = this.sortFieldKeyFor(baseline);

        Data submittedData = Data.newInstance()
                                 .setFieldValue(filterFieldKey, "e")
                                 .setFieldValue(sortFieldKey, Arrays.asList("name:DESCENDING"))
                                 .setFieldValue(expandedFieldKey, Arrays.asList("b"));

        recordedQueries.clear();
        this.content(this.render(data, submittedData));

        assertEquals(2, recordedQueries.size(), "one root fetch and one child fetch scoped to b's children");
        for (TreeTableQuery query : recordedQueries)
        {
            assertEquals(1, query.getFilters()
                                 .size(),
                         "the SAME active filter must be threaded into every query, root AND every expanded child group alike (documented interaction decision)");
            assertEquals("e", query.getFilters()
                                   .get(0)
                                   .getValue());
            assertEquals(1, query.getSorts()
                                 .size(),
                         "the SAME active sort spec must ALSO be threaded into every query, root AND every expanded child group alike");
            assertEquals("name", query.getSorts()
                                      .get(0)
                                      .getColumnKey());
            assertEquals(SortColumn.SortDirection.DESCENDING, query.getSorts()
                                                                   .get(0)
                                                                   .getDirection());
        }
    }

    @Test
    public void testMultipleActiveColumnFiltersEachBuildTheirOwnColumnFilterEntry()
    {
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "age", "30"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta", "age", "20"),
                                                                                                          Collections.emptyList())),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name"), TreeTableColumn.of("age", "Age")))
                                          .dataProvider(trackingProvider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        List<TreeTableNode.ColumnNode> baselineColumns = this.content(baseline)
                                                             .getColumns();
        String nameFilterFieldKey = baselineColumns.get(0)
                                                   .getFilterFieldKey();
        String ageFilterFieldKey = baselineColumns.get(1)
                                                  .getFilterFieldKey();

        Data submittedData = Data.newInstance()
                                 .setFieldValue(nameFilterFieldKey, "Al")
                                 .setFieldValue(ageFilterFieldKey, "30");

        recordedQueries.clear();
        this.content(this.render(data, submittedData));

        assertEquals(2, recordedQueries.get(0)
                                       .getFilters()
                                       .size(),
                     "two simultaneously-active per-column filters must build TWO ColumnFilter entries in the SAME query (AND-combined by the provider per the SPI contract)");
    }

    @Test
    public void testWindowSizeIsNotResetWhenAFilterValueChanges()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(1)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        String gridLocationJoined = this.gridLocationJoinedFor(baseline);
        String rootWindowFieldKey = "treetable." + gridLocationJoined + ".root.windowLimit";
        String filterFieldKey = this.content(baseline)
                                    .getColumns()
                                    .get(0)
                                    .getFilterFieldKey();

        // Simulate an EARLIER load-more click that already widened the root window to 2, THEN a filter value being
        // typed on the SAME submitted Data (documented interaction decision: a filter change does NOT reset
        // per-group windows to the first page - the widened window stays intact, least-surprising since a filter
        // only ever narrows the underlying result set).
        Data submittedData = Data.newInstance()
                                 .setFieldValue(rootWindowFieldKey, 2)
                                 .setFieldValue(filterFieldKey, "Al");

        TreeTableNode treeTableNode = this.content(this.render(data, submittedData));
        assertEquals(1, treeTableNode.getRows()
                                     .size(),
                     "the CONTAINS filter narrows the matching rows to one, independent of window size");

        // Prove the window field itself was NOT silently reset: re-render with the SAME window field but NO filter
        // must still show the widened 2-row window (not fall back to the component default windowSize=1).
        Data windowOnlyData = Data.newInstance()
                                  .setFieldValue(rootWindowFieldKey, 2);
        TreeTableNode unfilteredNode = this.content(this.render(data, windowOnlyData));
        assertEquals(2, unfilteredNode.getRows()
                                      .size(),
                     "the previously-widened window field must remain intact and unaffected by filter/sort logic - no reset code path exists for it");
    }

    // ------------------------------------------------------------------------------------------------------------
    // plan-78 - global + per-column filter/sort enable/disable, initially-visible default
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testGlobalFilterDisabledOmitsFunnelToggleAndPerColumnFilterControlsButKeepsSort()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .filterEnabled(false)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        assertFalse(treeTableNode.isFilterEnabled(), "filterEnabled must render false when withFilterEnabled(false) was configured");
        assertTrue(treeTableNode.isSortEnabled(), "sortEnabled must be unaffected by filterEnabled");
        assertTrue(treeTableNode.getFilterToggleTarget()
                                .isEmpty(),
                   "a disabled filter feature must not emit a routable filterToggleTarget");
        assertEquals(0, treeTableNode.getActiveFilterCount());
        assertFalse(treeTableNode.isFiltersVisible());

        TreeTableNode.ColumnNode column = treeTableNode.getColumns()
                                                       .get(0);
        assertNull(column.getFilterFieldKey(), "a disabled filter feature must not emit a filterFieldKey for any column");
        assertTrue(column.getFilterTarget()
                         .isEmpty(),
                   "a disabled filter feature must not emit a routable filterTarget for any column");
        assertFalse(column.getSortTarget()
                          .isEmpty(),
                    "sort must remain untouched - the column's sortTarget must still be routable");

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation)
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));
        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // load-more (unaffected) + one sort toggle (filter disabled: no funnel toggle, no filter control at all).
        assertEquals(2, gridSubComponents.size(),
                     "a disabled filter feature must not register a funnel-toggle or per-column filter sub-component at all");
    }

    @Test
    public void testGlobalFilterEffectivelyDisabledWhenNoConfiguredColumnIsFilterable()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")
                                                                                .withFilterable(false)))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          // filterEnabled left at its true default - the ABSENCE of any filterable
                                          // column must ALSO collapse the whole feature.
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        assertFalse(treeTableNode.isFilterEnabled(),
                    "filterEnabled must render false when NO configured column is filterable, even though the withFilterEnabled flag itself defaults true");
        assertTrue(treeTableNode.getFilterToggleTarget()
                                .isEmpty());
    }

    @Test
    public void testGlobalSortDisabledOmitsAllSortTogglesButKeepsFilter()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .sortEnabled(false)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        assertFalse(treeTableNode.isSortEnabled(), "sortEnabled must render false when withSortEnabled(false) was configured");
        assertTrue(treeTableNode.isFilterEnabled(), "filterEnabled must be unaffected by sortEnabled");
        assertFalse(treeTableNode.getFilterToggleTarget()
                                 .isEmpty(),
                    "filter must remain untouched - the funnel toggle must still be routable");

        TreeTableNode.ColumnNode column = treeTableNode.getColumns()
                                                       .get(0);
        assertNull(column.getSortDirection());
        assertTrue(column.getSortTarget()
                         .isEmpty(),
                   "a disabled sort feature must not emit a routable sortTarget for any column");
        assertFalse(column.getFilterTarget()
                          .isEmpty(),
                    "filter must remain untouched - the column's filterTarget must still be routable");

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation)
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));
        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // load-more + filter-toggle + one filter control (sort disabled: no sort toggle at all).
        assertEquals(3, gridSubComponents.size(), "a disabled sort feature must not register a per-column sort-toggle sub-component at all");
    }

    @Test
    public void testPerColumnFilterableFalseExcludesThatColumnsFilterControlButSiblingKeepsIts()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "age", "30"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")
                                                                                .withFilterable(false),
                                                                 TreeTableColumn.of("age", "Age")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));
        assertTrue(treeTableNode.isFilterEnabled(), "the feature stays enabled overall since at least one column (age) is filterable");

        TreeTableNode.ColumnNode nameColumn = treeTableNode.getColumns()
                                                           .get(0);
        assertFalse(nameColumn.isFilterable());
        assertNull(nameColumn.getFilterFieldKey(), "a non-filterable column must not get a filterFieldKey");
        assertTrue(nameColumn.getFilterTarget()
                             .isEmpty(),
                   "a non-filterable column must not get a routable filterTarget");

        TreeTableNode.ColumnNode ageColumn = treeTableNode.getColumns()
                                                          .get(1);
        assertTrue(ageColumn.isFilterable());
        assertNotNull(ageColumn.getFilterFieldKey(), "a sibling filterable column must still get its filterFieldKey");
        assertFalse(ageColumn.getFilterTarget()
                             .isEmpty(),
                    "a sibling filterable column must still get a routable filterTarget");
    }

    @Test
    public void testPerColumnSortableFalseExcludesThatColumnsSortToggleButSiblingKeepsIts()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "age", "30"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")
                                                                                .withSortable(false),
                                                                 TreeTableColumn.of("age", "Age")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        TreeTableNode.ColumnNode nameColumn = treeTableNode.getColumns()
                                                           .get(0);
        assertFalse(nameColumn.isSortable());
        assertTrue(nameColumn.getSortTarget()
                             .isEmpty(),
                   "a non-sortable column must not get a routable sortTarget");

        TreeTableNode.ColumnNode ageColumn = treeTableNode.getColumns()
                                                          .get(1);
        assertTrue(ageColumn.isSortable());
        assertFalse(ageColumn.getSortTarget()
                             .isEmpty(),
                    "a sibling sortable column must still get a routable sortTarget");
    }

    @Test
    public void testWithFiltersInitiallyVisibleTrueDefaultsFiltersVisibleTrueAtBaseline()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .filtersInitiallyVisible(true)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        assertTrue(treeTableNode.isFiltersVisible(), "withFiltersInitiallyVisible(true) must default filtersVisible to true when no submitted field is present");
    }

    @Test
    public void testActiveFilterCountIgnoresNonFilterableColumnsEvenWithAStaleSubmittedValue()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "age", "30"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")
                                                                                .withFilterable(false),
                                                                 TreeTableColumn.of("age", "Age")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        String gridLocationJoined = this.gridLocationJoinedFor(baseline);
        // the non-filterable "name" column never gets a real filterFieldKey emitted, but mirror its would-be key
        // (same deterministic naming scheme as TreeTableGridComponent.filterFieldKey) to prove a stale/injected
        // submitted value under it is defensively ignored.
        String staleNameFilterFieldKey = "treetable." + gridLocationJoined + ".filter.name";
        Data submittedData = Data.newInstance()
                                 .setFieldValue(staleNameFilterFieldKey, "Al");

        TreeTableNode treeTableNode = this.content(this.render(data, submittedData));
        assertEquals(0, treeTableNode.getActiveFilterCount(),
                     "a submitted value under a non-filterable column's field key must never count toward activeFilterCount");
    }

    // ------------------------------------------------------------------------------------------------------------
    // plan-79 - per-column INITIAL sort direction
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testSortableColumnWithInitialSortDirectionSeedsQueryAndIndicatorAtFirstRender()
    {
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "age", 10), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta", "age", 30), Collections.emptyList()),
                                                                                                 new Node("c", Map.of("name", "Gamma", "age", 20), Collections.emptyList())),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("age", "Age")
                                                                                .withInitialSortDirection(SortColumn.SortDirection.DESCENDING)))
                                          .dataProvider(trackingProvider)
                                          .windowSize(500)
                                          .build();

        recordedQueries.clear();
        TreeTableNode treeTableNode = this.content(this.render(data));

        assertEquals(1, recordedQueries.size(), "first render (no submitted Data at all) must still issue exactly one root query");
        List<SortColumn> sorts = recordedQueries.get(0)
                                                .getSorts();
        assertEquals(1, sorts.size(), "the sortable column's declared initial sort direction must seed the query's sort list on first render");
        assertEquals("age", sorts.get(0)
                                 .getColumnKey());
        assertEquals(SortColumn.SortDirection.DESCENDING, sorts.get(0)
                                                               .getDirection());

        TreeTableNode.ColumnNode ageColumn = treeTableNode.getColumns()
                                                          .get(0);
        assertEquals(SortColumn.SortDirection.DESCENDING, ageColumn.getSortDirection(),
                     "the emitted per-column sortDirection indicator must reflect the seeded initial sort on first render");

        List<RowEntryNode> rows = treeTableNode.getRows();
        assertEquals(3, rows.size());
        assertEquals("b", rows.get(0)
                              .getNodeId(),
                     "rows must actually come back DESCENDING by age (30, 20, 10) - the seed reached the real provider fetch");
        assertEquals("c", rows.get(1)
                              .getNodeId());
        assertEquals("a", rows.get(2)
                              .getNodeId());
    }

    @Test
    public void testMultipleColumnsWithInitialSortDirectionFollowDeclarationOrderPriority()
    {
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "age", 10), Collections.emptyList())),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("age", "Age")
                                                                                .withInitialSortDirection(SortColumn.SortDirection.DESCENDING),
                                                                 TreeTableColumn.of("name", "Name")
                                                                                .withInitialSortDirection(SortColumn.SortDirection.ASCENDING)))
                                          .dataProvider(trackingProvider)
                                          .windowSize(500)
                                          .build();

        recordedQueries.clear();
        this.content(this.render(data));

        List<SortColumn> sorts = recordedQueries.get(0)
                                                .getSorts();
        assertEquals(2, sorts.size());
        assertEquals("age", sorts.get(0)
                                 .getColumnKey(),
                     "the FIRST declared column with an initial sort direction must be the PRIMARY sort key");
        assertEquals(SortColumn.SortDirection.DESCENDING, sorts.get(0)
                                                               .getDirection());
        assertEquals("name", sorts.get(1)
                                  .getColumnKey(),
                     "the SECOND declared column with an initial sort direction must be the SECONDARY sort key");
        assertEquals(SortColumn.SortDirection.ASCENDING, sorts.get(1)
                                                              .getDirection());
    }

    @Test
    public void testNonSortableColumnWithInitialSortDirectionIsNotSeededIntoTheQueryOrIndicator()
    {
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "age", 10), Collections.emptyList())),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("age", "Age")
                                                                                .withSortable(false)
                                                                                .withInitialSortDirection(SortColumn.SortDirection.DESCENDING)))
                                          .dataProvider(trackingProvider)
                                          .windowSize(500)
                                          .build();

        recordedQueries.clear();
        TreeTableNode treeTableNode = this.content(this.render(data));

        assertTrue(recordedQueries.get(0)
                                  .getSorts()
                                  .isEmpty(),
                   "a non-sortable column's initial sort direction must be ignored - no seed entry in the query");

        TreeTableNode.ColumnNode ageColumn = treeTableNode.getColumns()
                                                          .get(0);
        assertNull(ageColumn.getSortDirection(), "a non-sortable column must never emit a sortDirection indicator, even with an initial direction configured");
        assertTrue(ageColumn.getSortTarget()
                            .isEmpty(),
                   "a non-sortable column must not get a routable sortTarget regardless of initial sort direction");
    }

    @Test
    public void testUserSortClickOverridesTheInitialSeedOnceTheSortFieldIsSubmitted()
    {
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "age", 10), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta", "age", 30), Collections.emptyList())),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("age", "Age")
                                                                                .withInitialSortDirection(SortColumn.SortDirection.DESCENDING)))
                                          .dataProvider(trackingProvider)
                                          .windowSize(500)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);

        // the user explicitly sorts ASCENDING (opposite of the configured initial DESCENDING) - the submitted value
        // must win outright, the initial seed must not apply at all.
        recordedQueries.clear();
        Data ascendingSubmitted = this.sortedData(baseline, "age:ASCENDING");
        TreeTableNode ascendingNode = this.content(this.render(data, ascendingSubmitted));
        assertEquals(1, recordedQueries.get(0)
                                       .getSorts()
                                       .size());
        assertEquals(SortColumn.SortDirection.ASCENDING, recordedQueries.get(0)
                                                                        .getSorts()
                                                                        .get(0)
                                                                        .getDirection(),
                     "an explicitly submitted sort must override the column's configured initial sort direction");
        assertEquals(SortColumn.SortDirection.ASCENDING, ascendingNode.getColumns()
                                                                      .get(0)
                                                                      .getSortDirection());

        // the user cycles the sort OFF entirely (third click) - the sort field is PRESENT but its list is empty; the
        // initial seed must NOT reappear (present-but-empty is an explicit user choice, not "absent").
        recordedQueries.clear();
        Data noneSubmitted = this.sortedData(baseline);
        TreeTableNode noneNode = this.content(this.render(data, noneSubmitted));
        assertTrue(recordedQueries.get(0)
                                  .getSorts()
                                  .isEmpty(),
                   "a present-but-empty submitted sort field (user cycled off) must NOT fall back to the initial seed");
        assertNull(noneNode.getColumns()
                           .get(0)
                           .getSortDirection());
    }

    // ------------------------------------------------------------------------------------------------------------
    // plan-82 bugfix - initial sort dropped on the first sort-toggle/reorder click (handler path must build on the
    // SAME seeded default readSorts already applies at render time - see TreeTableRendererImpl.effectiveEncodedSorts)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testMultiColumnModeFirstSortToggleClickOnAnotherColumnBuildsOnTheSeededInitialSort()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "owner", "Bob"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta", "owner", "Ann"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")
                                                                                .withInitialSortDirection(SortColumn.SortDirection.DESCENDING),
                                                                 TreeTableColumn.of("owner", "Owner")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .multiColumnSortEnabled(true)
                                          .build();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        // No submitted Data at all - exactly the FIRST sort-toggle click after the seeded initial render (the sort
        // field has never been written yet, matching the live-repro report).
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation, Optional.empty())
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));
        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // name is seeded active (initial DESCENDING) so it additionally registers a reorder control: loadmore(0),
        // filtertoggle(1), filter0[name](2), sort0[name](3), sortpriority0[name](4), filter1[owner](5), sort1[owner](6).
        assertEquals(7, gridSubComponents.size());
        DataEventHandler ownerSortHandler = this.captureSingleHandler(gridSubComponents.get(6));

        String sortFieldKey = "treetable." + gridLocation.get()
                                                         .stream()
                                                         .collect(Collectors.joining("."))
                              + ".sort";

        // THE decisive first click: eventData carries NO sort field at all yet.
        Data afterOwnerClick = ownerSortHandler.invoke(Data.newInstance(), Data.newInstance())
                                               .getData();
        assertEquals(Arrays.asList("name:DESCENDING", "owner:ASCENDING"), afterOwnerClick.getFieldValue(sortFieldKey)
                                                                                         .get()
                                                                                         .asStringList(),
                     "the seeded Name DESC must NOT be discarded - clicking Owner must build ON TOP of it as priority 2, not replace it");

        TreeTableNode node = this.content(this.render(data, afterOwnerClick));
        assertEquals(1, node.getColumns()
                            .get(0)
                            .getSortPriority(),
                     "Name must remain priority 1");
        assertEquals(SortColumn.SortDirection.DESCENDING, node.getColumns()
                                                              .get(0)
                                                              .getSortDirection());
        assertEquals(2, node.getColumns()
                            .get(1)
                            .getSortPriority(),
                     "Owner must become priority 2");
        assertEquals(SortColumn.SortDirection.ASCENDING, node.getColumns()
                                                             .get(1)
                                                             .getSortDirection());
        assertEquals(2, node.getActiveSortCount(), "activeSortCount must reflect BOTH the seeded and the newly-clicked column");
    }

    private TreeTableData twoColumnSingleModeDataWithNameSeededDescending()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "owner", "Bob"), Collections.emptyList())),
                                                                                   false);
        return TreeTableData.builder()
                            .columns(Arrays.asList(TreeTableColumn.of("name", "Name")
                                                                  .withInitialSortDirection(SortColumn.SortDirection.DESCENDING),
                                                   TreeTableColumn.of("owner", "Owner")))
                            .dataProvider(provider)
                            .windowSize(500)
                            // multiColumnSortEnabled defaults to false - single-column mode.
                            .build();
    }

    @Test
    public void testSingleColumnModeFirstClickOnTheSeededColumnItselfCyclesFromItsSeededDirectionRatherThanRestartingAtAscending()
    {
        TreeTableData data = this.twoColumnSingleModeDataWithNameSeededDescending();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation, Optional.empty())
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));
        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // single mode never registers a reorder control: loadmore(0), filtertoggle(1), filter0[name](2),
        // sort0[name](3), filter1[owner](4), sort1[owner](5).
        assertEquals(6, gridSubComponents.size());
        DataEventHandler nameSortHandler = this.captureSingleHandler(gridSubComponents.get(3));

        String sortFieldKey = "treetable." + gridLocation.get()
                                                         .stream()
                                                         .collect(Collectors.joining("."))
                              + ".sort";

        Data afterNameClick = nameSortHandler.invoke(Data.newInstance(), Data.newInstance())
                                             .getData();
        assertTrue(afterNameClick.getFieldValue(sortFieldKey)
                                 .map(value -> value.asStringList())
                                 .orElse(Collections.emptyList())
                                 .isEmpty(),
                   "clicking the SEEDED column itself must cycle it OFF from its seeded DESCENDING direction (DESCENDING -> none), never restart at ASCENDING");
    }

    @Test
    public void testSingleColumnModeFirstClickOnADifferentColumnReplacesTheSeededColumn()
    {
        TreeTableData data = this.twoColumnSingleModeDataWithNameSeededDescending();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation, Optional.empty())
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));
        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        assertEquals(6, gridSubComponents.size());
        DataEventHandler ownerSortHandler = this.captureSingleHandler(gridSubComponents.get(5));

        String sortFieldKey = "treetable." + gridLocation.get()
                                                         .stream()
                                                         .collect(Collectors.joining("."))
                              + ".sort";

        Data afterOwnerClick = ownerSortHandler.invoke(Data.newInstance(), Data.newInstance())
                                               .getData();
        assertEquals(Arrays.asList("owner:ASCENDING"), afterOwnerClick.getFieldValue(sortFieldKey)
                                                                      .get()
                                                                      .asStringList(),
                     "single-column mode: clicking a DIFFERENT column must REPLACE the seeded column with itself alone");
    }

    @Test
    public void testSortPriorityReorderHandlerSeedsFromInitialSortWhenInvokedBeforeAnySortFieldWrite()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha", "owner", "Bob"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")
                                                                                .withInitialSortDirection(SortColumn.SortDirection.DESCENDING),
                                                                 TreeTableColumn.of("owner", "Owner")
                                                                                .withInitialSortDirection(SortColumn.SortDirection.ASCENDING)))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .multiColumnSortEnabled(true)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        String sortFieldKey = this.sortFieldKeyFor(baseline);
        String ownerPriorityFieldKey = this.content(baseline)
                                           .getColumns()
                                           .get(1)
                                           .getSortPriorityFieldKey();
        assertNotNull(ownerPriorityFieldKey, "owner is seeded active (initial ASCENDING) so it must carry a sortPriorityFieldKey from the very first render");

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        // No submitted Data at all - the reorder control fires before ANY sort-field write, exactly the seeded baseline.
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation, Optional.empty())
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));
        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // both name and owner are seeded active: loadmore(0), filtertoggle(1), filter0[name](2), sort0[name](3),
        // sortpriority0[name](4), filter1[owner](5), sort1[owner](6), sortpriority1[owner](7).
        assertEquals(8, gridSubComponents.size());
        DataEventHandler ownerReorderHandler = this.captureSingleHandler(gridSubComponents.get(7));

        // eventData carries NO sort field (before any write) but DOES carry owner's requested new priority (1) -
        // mirrors the write-then-dispatch mechanism (b) the frontend uses.
        Data eventData = Data.newInstance()
                             .setFieldValue(ownerPriorityFieldKey, 1);
        Data afterReorder = ownerReorderHandler.invoke(eventData, Data.newInstance())
                                               .getData();
        assertEquals(Arrays.asList("owner:ASCENDING", "name:DESCENDING"), afterReorder.getFieldValue(sortFieldKey)
                                                                                      .get()
                                                                                      .asStringList(),
                     "the reorder handler must operate on the SEEDED [name,owner] list (not an empty one) when fired before any sort-field write");
    }

    // ------------------------------------------------------------------------------------------------------------
    // plan-80 - flat-mode (flatten-the-tree) toggle
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testFlatModeOffByDefaultRendersOrdinaryTreeRowsAndNoRoutableFlatToggleTarget()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"),
                                                                                                          Arrays.asList(new Node("a1", Map.of("name", "Alpha One"), Collections.emptyList())))),
                                                                                   true);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        assertFalse(treeTableNode.isFlatMode(), "flatMode must default to false when withInitiallyFlat was never called");
        assertFalse(treeTableNode.isFlatModeToggleEnabled(), "the toggle must be absent by default (opt-in)");
        assertTrue(treeTableNode.getFlatToggleTarget()
                                .isEmpty(),
                   "a disabled toggle must not emit a routable flatToggleTarget");
        assertEquals(1, treeTableNode.getRows()
                                     .size(),
                     "a1's parent 'a' is not expanded by default, so only row a is emitted - ordinary tree behavior unchanged");
    }

    @Test
    public void testWithInitiallyFlatTrueDefaultsFlatModeTrueAtBaseline()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"),
                                                                                                          Arrays.asList(new Node("a1", Map.of("name", "Alpha One"), Collections.emptyList())))),
                                                                                   true);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .initiallyFlat(true)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));

        assertTrue(treeTableNode.isFlatMode(), "withInitiallyFlat(true) must default flatMode to true when no submitted field is present");
    }

    @Test
    public void testFlatModeRowsAreAllDepthZeroNonExpandableWithNoChildLoadMoreAndSpanMultipleDepths()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"),
                                                                                                          Arrays.asList(new Node("b1", Map.of("name", "Beta One"), Collections.emptyList())))),
                                                                                   true);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .initiallyFlat(true)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));
        List<RowEntryNode> rows = treeTableNode.getRows();

        assertEquals(3, rows.size(), "a flat fetch must include every node at every depth: a, b, AND b's child b1");
        List<String> ids = rows.stream()
                               .map(RowEntryNode::getNodeId)
                               .collect(Collectors.toList());
        assertTrue(ids.contains("b1"), "the flat window must contain a node from a NON-root depth (b1) - proving true flattening, not just root rows");

        for (RowEntryNode row : rows)
        {
            assertEquals(0, row.getDepth(), "every row must render at depth 0 in flat mode: " + row.getNodeId());
            assertFalse(row.isExpandable(), "every row must render non-expandable in flat mode (no carets): " + row.getNodeId());
            assertFalse(row.isExpanded());
            assertNull(row.getChildLoadMore(), "no row may carry a childLoadMore descriptor in flat mode: " + row.getNodeId());
        }
    }

    @Test
    public void testFlatModeRegistersNoPerRowExpandControlsEvenForNodesWithChildren()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"),
                                                                                                          Arrays.asList(new Node("b1", Map.of("name", "Beta One"), Collections.emptyList())))),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .initiallyFlat(true)
                                          .build();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation)
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));

        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // load-more + filter-toggle + one filter control + one sort toggle ONLY - NO row-expand control for "b" even
        // though it structurally has a child (would be expandable in tree mode), because flat mode never recurses
        // into collectRowSubComponents/child fetches at all (both traversals agree, mirroring buildNode).
        assertEquals(4, gridSubComponents.size(),
                     "flat mode must register NO per-row expand controls and NO child fetches, even for a node that would be expandable in tree mode");
    }

    @Test
    public void testFlatModeRootWindowAndLoadMorePageTheWholeFlattenedList()
    {
        List<Node> manyChildren = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            manyChildren.add(new Node("b" + i, Map.of("name", "Beta " + i), Collections.emptyList()));
        }
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"), manyChildren)),
                                                                                   true);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(3)
                                          .initiallyFlat(true)
                                          .build();

        // Total flattened nodes: a, b, b0..b4 = 7.
        TreeTableNode treeTableNode = this.content(this.render(data));
        assertEquals(3, treeTableNode.getRows()
                                     .size(),
                     "the root windowSize must cap the flat row set exactly like a tree root fetch");
        assertTrue(treeTableNode.getLoadMore()
                                .isAvailable());
        assertEquals(7L, treeTableNode.getLoadMore()
                                      .getTotalCount(),
                     "loadMore's totalCount must reflect the WHOLE flattened node set (7), not just the 2-row root sibling group");

        // Widening the ROOT window field (the SAME load-more mechanism as tree mode) must page further into the
        // flat list - "only the root-level window + root load-more page the flat list" (per brief).
        RerenderingContainerNode baseline = this.render(data);
        String gridLocationJoined = this.gridLocationJoinedFor(baseline);
        String rootWindowFieldKey = "treetable." + gridLocationJoined + ".root.windowLimit";
        Data submittedData = Data.newInstance()
                                 .setFieldValue(rootWindowFieldKey, 7);

        TreeTableNode widenedNode = this.content(this.render(data, submittedData));
        assertEquals(7, widenedNode.getRows()
                                   .size(),
                     "widening the root window (the SAME load-more mechanism as tree mode) must page further into the flat list");
        assertFalse(widenedNode.getLoadMore()
                               .isAvailable());
    }

    @Test
    public void testFlatModeIgnoresTheExpandedNodeIdSetEntirely()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"),
                                                                                                          Arrays.asList(new Node("a1", Map.of("name", "Alpha One"), Collections.emptyList())))),
                                                                                   true);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .initiallyFlat(true)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        // Even with "a" explicitly marked expanded in the shared expandedNodes field, flat mode must ignore it
        // entirely - the flattened rows already include a1 regardless, and no row may render expanded==true.
        Data submittedData = this.expandedData(baseline, "a");

        TreeTableNode treeTableNode = this.content(this.render(data, submittedData));
        assertEquals(2, treeTableNode.getRows()
                                     .size());
        for (RowEntryNode row : treeTableNode.getRows())
        {
            assertFalse(row.isExpanded(), "the expanded-node-id set must be ignored entirely while flatMode is true");
        }
    }

    @Test
    public void testFlatModeIssuesExactlyOneQueryWithEmptyParentNodeIdAndIsFlatTrue()
    {
        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"),
                                                                                                          Arrays.asList(new Node("b1", Map.of("name", "Beta One"), Collections.emptyList())))),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(trackingProvider)
                                          .windowSize(500)
                                          .initiallyFlat(true)
                                          .build();

        recordedQueries.clear();
        this.content(this.render(data));

        assertEquals(1, recordedQueries.size(), "flat mode must issue exactly ONE query - no per-expanded-group child fetches");
        assertTrue(recordedQueries.get(0)
                                  .isFlat(),
                   "the query issued in flat mode must carry isFlat==true");
        assertFalse(recordedQueries.get(0)
                                   .getParentNodeId()
                                   .isPresent(),
                    "the query issued in flat mode must always carry an EMPTY parentNodeId");
    }

    @Test
    public void testFlatModeAppliesFilterAndSortAcrossTheWholeFlattenedSet()
    {
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Zebra"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "Beta"),
                                                                                                          Arrays.asList(new Node("b1", Map.of("name", "Alpha"), Collections.emptyList())))),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(delegate)
                                          .windowSize(500)
                                          .initiallyFlat(true)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        String sortFieldKey = this.sortFieldKeyFor(baseline);

        TreeTableNode sortedNode = this.content(this.render(data,
                                                            Data.newInstance()
                                                                .setFieldValue(sortFieldKey, Arrays.asList("name:ASCENDING"))));

        List<String> ids = sortedNode.getRows()
                                     .stream()
                                     .map(RowEntryNode::getNodeId)
                                     .collect(Collectors.toList());
        assertEquals(Arrays.asList("b1", "b", "a"), ids,
                     "sort must order ACROSS the whole flattened set (Alpha=b1, Beta=b, Zebra=a), not per sibling-group");

        String filterFieldKey = this.content(baseline)
                                    .getColumns()
                                    .get(0)
                                    .getFilterFieldKey();
        TreeTableNode filteredNode = this.content(this.render(data,
                                                              Data.newInstance()
                                                                  .setFieldValue(filterFieldKey, "eta")));
        List<String> filteredIds = filteredNode.getRows()
                                               .stream()
                                               .map(RowEntryNode::getNodeId)
                                               .collect(Collectors.toList());
        assertEquals(Collections.singletonList("b"), filteredIds, "filter must narrow ACROSS the whole flattened set, not per sibling-group");
    }

    @Test
    public void testFlatModeToggleSubComponentOnlyPresentWhenEnabled()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableData disabledData = TreeTableData.builder()
                                                  .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                                  .dataProvider(provider)
                                                  .windowSize(500)
                                                  .build();

        TreeTableNode disabledNode = this.content(this.render(disabledData));
        assertFalse(disabledNode.isFlatModeToggleEnabled());
        assertTrue(disabledNode.getFlatToggleTarget()
                               .isEmpty());

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl disabledRenderer = new TreeTableRendererImpl(disabledData, uiComponentFactory, context);
        Location disabledContainerLocation = disabledRenderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> disabledContainerSubComponents = disabledRenderer.getSubComponents(disabledContainerLocation)
                                                                                          .collect(Collectors.toList());
        RenderableUIComponent<?> disabledGridComponent = (RenderableUIComponent<?>) disabledContainerSubComponents.get(0)
                                                                                                                  .getComponent();
        Location disabledGridLocation = disabledGridComponent.asRenderer()
                                                             .getLocation(this.locationSupportFor(disabledContainerSubComponents.get(0)
                                                                                                                                .getParentLocation()));
        List<ParentLocationAndComponent> disabledGridSubComponents = disabledGridComponent.asRenderer()
                                                                                          .getSubComponents(disabledGridLocation)
                                                                                          .collect(Collectors.toList());
        // load-more + filter-toggle + one filter control + one sort toggle (flat toggle DISABLED - no extra sub-component).
        assertEquals(4, disabledGridSubComponents.size(), "a disabled flat-mode toggle must not register a sub-component at all");

        TreeTableData enabledData = disabledData.toBuilder()
                                                .flatModeToggleEnabled(true)
                                                .build();
        TreeTableNode enabledNode = this.content(this.render(enabledData));
        assertTrue(enabledNode.isFlatModeToggleEnabled());
        assertFalse(enabledNode.getFlatToggleTarget()
                               .isEmpty(),
                    "an enabled flat-mode toggle must carry a routable Target");

        TreeTableRendererImpl enabledRenderer = new TreeTableRendererImpl(enabledData, uiComponentFactory, context);
        Location enabledContainerLocation = enabledRenderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> enabledContainerSubComponents = enabledRenderer.getSubComponents(enabledContainerLocation)
                                                                                        .collect(Collectors.toList());
        RenderableUIComponent<?> enabledGridComponent = (RenderableUIComponent<?>) enabledContainerSubComponents.get(0)
                                                                                                                .getComponent();
        Location enabledGridLocation = enabledGridComponent.asRenderer()
                                                           .getLocation(this.locationSupportFor(enabledContainerSubComponents.get(0)
                                                                                                                             .getParentLocation()));
        List<ParentLocationAndComponent> enabledGridSubComponents = enabledGridComponent.asRenderer()
                                                                                        .getSubComponents(enabledGridLocation)
                                                                                        .collect(Collectors.toList());
        assertEquals(5, enabledGridSubComponents.size(), "an enabled flat-mode toggle must register exactly one additional sub-component");
    }

    @Test
    public void testFlatModeToggleSubComponentIsDiscoverableAtTheTargetsLocationAndRegistersItsOwnHandlerDistinctFromOtherTargets()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .flatModeToggleEnabled(true)
                                          .build();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation)
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));

        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // Order for a single non-expandable-row column grid with the flat toggle enabled: loadmore(0),
        // filtertoggle(1), flattoggle(2), filter0(3), sort0(4).
        ParentLocationAndComponent flatToggleEntry = gridSubComponents.get(2);
        RenderableUIComponent<?> flatToggleComponent = (RenderableUIComponent<?>) flatToggleEntry.getComponent();
        Location flatToggleLocation = flatToggleComponent.asRenderer()
                                                         .getLocation(this.locationSupportFor(flatToggleEntry.getParentLocation()));

        TreeTableNode treeTableNode = this.content(this.render(data));
        assertEquals(treeTableNode.getFlatToggleTarget()
                                  .get(),
                     flatToggleLocation.get(),
                     "the discovered flat-toggle sub-component's Location must match the flatToggleTarget embedded in the rendered node (trap #4)");
        assertNotEquals(treeTableNode.getFlatToggleTarget()
                                     .get(),
                        treeTableNode.getFilterToggleTarget()
                                     .get(),
                        "the flat-toggle Target must be distinct from the funnel filter toggle's Target (trap #4)");
        assertNotEquals(treeTableNode.getFlatToggleTarget()
                                     .get(),
                        treeTableNode.getColumns()
                                     .get(0)
                                     .getSortTarget()
                                     .get(),
                        "the flat-toggle Target must be distinct from the column sort Target (trap #4)");

        List<DataEventHandler> registeredHandlers = new ArrayList<>();
        EventHandlerRegistrationSupport capturingSupport = new EventHandlerRegistrationSupport() {
            @Override
            public EventHandlerRegistrationSupport register(EventHandler eventHandler)
            {
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport register(DataEventHandler eventHandler)
            {
                registeredHandlers.add(eventHandler);
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport registerAsRerenderingNode()
            {
                return this;
            }
        };
        flatToggleComponent.asRenderer()
                           .manageEventHandler(capturingSupport);
        assertEquals(1, registeredHandlers.size(), "the flat-toggle control must register exactly one DataEventHandler");

        Data invokedData = Data.newInstance();
        registeredHandlers.get(0)
                          .invoke(invokedData, Data.newInstance());
        assertTrue(invokedData.getFieldValue("treetable." + gridLocation.get()
                                                                        .stream()
                                                                        .collect(Collectors.joining("."))
                                             + ".flatMode")
                              .get()
                              .asBoolean(),
                   "invoking the registered handler must flip the flatMode field (from the withInitiallyFlat default false) to true");
    }

    // ------------------------------------------------------------------------------------------------------------
    // plan-81 - single-vs-multi column sort mode, per-column sortPriority, and the priority re-rank/reorder control
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Registers a capturing {@link EventHandlerRegistrationSupport}, calls {@code manageEventHandler} on the given
     * sub-component, and returns the single {@link DataEventHandler} it registers - reduces boilerplate shared by
     * every handler-invocation test in this file (see the inline anonymous-class variant still used by the older
     * Slice 4/5/6 tests above for the equivalent hand-rolled shape).
     */
    private DataEventHandler captureSingleHandler(ParentLocationAndComponent entry)
    {
        RenderableUIComponent<?> component = (RenderableUIComponent<?>) entry.getComponent();
        List<DataEventHandler> registeredHandlers = new ArrayList<>();
        EventHandlerRegistrationSupport capturingSupport = new EventHandlerRegistrationSupport() {
            @Override
            public EventHandlerRegistrationSupport register(EventHandler eventHandler)
            {
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport register(DataEventHandler eventHandler)
            {
                registeredHandlers.add(eventHandler);
                return this;
            }

            @Override
            public EventHandlerRegistrationSupport registerAsRerenderingNode()
            {
                return this;
            }
        };
        component.asRenderer()
                 .manageEventHandler(capturingSupport);
        assertEquals(1, registeredHandlers.size(), "exactly one DataEventHandler must be registered for this sub-component");
        return registeredHandlers.get(0);
    }

    @Test
    public void testMultiColumnSortEnabledDefaultsToFalseAndActiveSortCountReflectsTheSortList()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("name", "Name")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .build();

        TreeTableNode treeTableNode = this.content(this.render(data));
        assertFalse(treeTableNode.isMultiColumnSortEnabled(),
                    "multiColumnSortEnabled must default to false (single-column mode) when withMultiColumnSortEnabled was never called");
        assertEquals(0, treeTableNode.getActiveSortCount(), "activeSortCount must be 0 when nothing is sorted");
        assertEquals(0, treeTableNode.getColumns()
                                     .get(0)
                                     .getSortPriority(),
                     "an unsorted column's sortPriority must be 0");
    }

    @Test
    public void testSingleColumnModeDefaultReplacesActiveColumnOnAnotherColumnsSortToggleClick()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("x", Map.of("a", "1", "b", "2"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("a", "A"), TreeTableColumn.of("b", "B")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          // multiColumnSortEnabled defaults to false - single-column mode.
                                          .build();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation)
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));
        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // Order for a 2-column grid in single-column mode (no reorder controls ever register in single mode):
        // loadmore(0), filtertoggle(1), filter0[a](2), sort0[a](3), filter1[b](4), sort1[b](5).
        assertEquals(6, gridSubComponents.size(), "single-column mode must never register a priority-reorder sub-component");
        DataEventHandler bSortHandler = this.captureSingleHandler(gridSubComponents.get(5));

        String sortFieldKey = "treetable." + gridLocation.get()
                                                         .stream()
                                                         .collect(Collectors.joining("."))
                              + ".sort";

        // Simulate column A already being the sole active sort (ASCENDING), as if the user had clicked A first.
        Data withAActive = Data.newInstance()
                               .setFieldValue(sortFieldKey, Arrays.asList("a:ASCENDING"));

        // THE decisive click: toggling B while A is active must REPLACE the whole list with just B.
        Data afterBClick = bSortHandler.invoke(withAActive, Data.newInstance())
                                       .getData();
        assertEquals(Arrays.asList("b:ASCENDING"), afterBClick.getFieldValue(sortFieldKey)
                                                              .get()
                                                              .asStringList(),
                     "single-column mode: clicking B while A was sorted must make the sort list contain ONLY B");

        TreeTableNode nodeAfterBClick = this.content(this.render(data, afterBClick));
        assertEquals(0, nodeAfterBClick.getColumns()
                                       .get(0)
                                       .getSortPriority(),
                     "A's sortPriority must return to 0 once cleared by B's click");
        assertEquals(1, nodeAfterBClick.getColumns()
                                       .get(1)
                                       .getSortPriority(),
                     "B becomes the sole active column, at priority 1");
        assertEquals(1, nodeAfterBClick.getActiveSortCount());
        assertFalse(nodeAfterBClick.isMultiColumnSortEnabled());
    }

    @Test
    public void testMultiColumnModeAccumulatesPriorityAndCyclingPrimaryOffPromotesSecondaryToPriorityOne()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("x", Map.of("a", "1", "b", "2"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("a", "A"), TreeTableColumn.of("b", "B")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .multiColumnSortEnabled(true)
                                          .build();

        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation)
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));
        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // Order with NEITHER column active yet (no reorder control registers until a column is active), multi
        // mode: loadmore(0), filtertoggle(1), filter0[a](2), sort0[a](3), filter1[b](4), sort1[b](5).
        assertEquals(6, gridSubComponents.size());
        DataEventHandler aSortHandler = this.captureSingleHandler(gridSubComponents.get(3));
        DataEventHandler bSortHandler = this.captureSingleHandler(gridSubComponents.get(5));

        String sortFieldKey = "treetable." + gridLocation.get()
                                                         .stream()
                                                         .collect(Collectors.joining("."))
                              + ".sort";

        // Click A (becomes PRIMARY, ASCENDING), then click B (ACCUMULATES as SECONDARY, ASCENDING) - echoing
        // forward A's click, exactly as a real client resubmits the FULL Data snapshot on every subsequent event.
        Data afterA = aSortHandler.invoke(Data.newInstance(), Data.newInstance())
                                  .getData();
        Data afterAThenB = bSortHandler.invoke(afterA, Data.newInstance())
                                       .getData();
        assertEquals(Arrays.asList("a:ASCENDING", "b:ASCENDING"), afterAThenB.getFieldValue(sortFieldKey)
                                                                             .get()
                                                                             .asStringList());

        TreeTableNode node = this.content(this.render(data, afterAThenB));
        assertEquals(1, node.getColumns()
                            .get(0)
                            .getSortPriority(),
                     "A (clicked first) must be PRIMARY, priority 1");
        assertEquals(2, node.getColumns()
                            .get(1)
                            .getSortPriority(),
                     "B (clicked second) must be SECONDARY, priority 2");
        assertEquals(2, node.getActiveSortCount());
        assertTrue(node.isMultiColumnSortEnabled());

        // Cycle A off entirely: ASCENDING -> DESCENDING -> none (2 more clicks on A) must promote B to priority 1.
        Data afterASecondClick = aSortHandler.invoke(afterAThenB, Data.newInstance())
                                             .getData();
        Data afterAThirdClick = aSortHandler.invoke(afterASecondClick, Data.newInstance())
                                            .getData();
        assertEquals(Arrays.asList("b:ASCENDING"), afterAThirdClick.getFieldValue(sortFieldKey)
                                                                   .get()
                                                                   .asStringList(),
                     "cycling A off entirely must leave only B in the ordered sort list");

        TreeTableNode nodeAfter = this.content(this.render(data, afterAThirdClick));
        assertEquals(0, nodeAfter.getColumns()
                                 .get(0)
                                 .getSortPriority(),
                     "A must return to priority 0 once cycled off");
        assertEquals(1, nodeAfter.getColumns()
                                 .get(1)
                                 .getSortPriority(),
                     "B must be PROMOTED to priority 1 once A is removed");
        assertEquals(1, nodeAfter.getActiveSortCount());
    }

    /**
     * Fresh (never previously mutated) submitted {@link Data} carrying a 3-column active sort spec
     * ({@code a:ASCENDING, b:DESCENDING, c:ASCENDING}) plus, optionally, a requested new priority written under
     * {@code bPriorityFieldKey} - the {@code Data} implementation's {@code setFieldValue} mutates-and-returns
     * {@code this}, so every reorder scenario below must start from a BRAND NEW {@link Data#newInstance()} rather
     * than reusing/chaining one shared instance across assertions.
     */
    private Data threeColumnActiveSortData(String sortFieldKey, String bPriorityFieldKey, Integer requestedPriority)
    {
        Data data = Data.newInstance()
                        .setFieldValue(sortFieldKey, Arrays.asList("a:ASCENDING", "b:DESCENDING", "c:ASCENDING"));
        if (requestedPriority != null)
        {
            data.setFieldValue(bPriorityFieldKey, requestedPriority);
        }
        return data;
    }

    @Test
    public void testSortPriorityReorderHandlerMovesColumnToChosenPositionPreservingDirectionsAndIgnoresOutOfRangeOrNoOpValues()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("x", Map.of("a", "1", "b", "2", "c", "3"), Collections.emptyList())),
                                                                                   false);
        TreeTableData data = TreeTableData.builder()
                                          .columns(Arrays.asList(TreeTableColumn.of("a", "A"), TreeTableColumn.of("b", "B"), TreeTableColumn.of("c", "C")))
                                          .dataProvider(provider)
                                          .windowSize(500)
                                          .multiColumnSortEnabled(true)
                                          .build();

        RerenderingContainerNode baseline = this.render(data);
        String sortFieldKey = this.sortFieldKeyFor(baseline);
        Data activeSort = this.threeColumnActiveSortData(sortFieldKey, null, null);

        // Discover B's priority-reorder sub-component: it is gated on B being CURRENTLY ACTIVE, so it is only
        // discoverable when the registration walk is driven with `activeSort` as the submitted Data (the 2-arg
        // getSubComponents overload).
        ComponentContext context = this.newContext();
        UIComponentFactory uiComponentFactory = this.newFactory(context);
        TreeTableRendererImpl renderer = new TreeTableRendererImpl(data, uiComponentFactory, context);
        Location containerLocation = renderer.getLocation(this.rootLocationSupport());
        List<ParentLocationAndComponent> containerSubComponents = renderer.getSubComponents(containerLocation, Optional.of(activeSort))
                                                                          .collect(Collectors.toList());
        ParentLocationAndComponent gridEntry = containerSubComponents.get(0);
        RenderableUIComponent<?> gridComponent = (RenderableUIComponent<?>) gridEntry.getComponent();
        Location gridLocation = gridComponent.asRenderer()
                                             .getLocation(this.locationSupportFor(gridEntry.getParentLocation()));
        List<ParentLocationAndComponent> gridSubComponents = gridComponent.asRenderer()
                                                                          .getSubComponents(gridLocation)
                                                                          .collect(Collectors.toList());
        // Order with all 3 columns active, multi mode: loadmore(0), filtertoggle(1),
        // filter0[a](2), sort0[a](3), sortpriority0[a](4),
        // filter1[b](5), sort1[b](6), sortpriority1[b](7),
        // filter2[c](8), sort2[c](9), sortpriority2[c](10).
        assertEquals(11, gridSubComponents.size(),
                     "each of the 3 active, sortable columns must additionally register its own priority-reorder sub-component in multi mode");
        DataEventHandler bReorderHandler = this.captureSingleHandler(gridSubComponents.get(7));

        TreeTableNode nodeBeforeReorder = this.content(this.render(data, activeSort));
        String bPriorityFieldKey = nodeBeforeReorder.getColumns()
                                                    .get(1)
                                                    .getSortPriorityFieldKey();
        assertNotNull(bPriorityFieldKey, "an active column in multi mode must carry a sortPriorityFieldKey");

        // THE decisive reorder: write B's chosen priority (1) into its field, THEN fire the reorder handler
        // (mechanism (b), mirrors the filter input's write-then-dispatch pattern).
        Data withRequestedPriorityOne = this.threeColumnActiveSortData(sortFieldKey, bPriorityFieldKey, 1);
        Data afterReorder = bReorderHandler.invoke(withRequestedPriorityOne, Data.newInstance())
                                           .getData();
        assertEquals(Arrays.asList("b:DESCENDING", "a:ASCENDING", "c:ASCENDING"), afterReorder.getFieldValue(sortFieldKey)
                                                                                              .get()
                                                                                              .asStringList(),
                     "B must move to position 1, shifting A and C down while preserving relative order and every column's direction");

        TreeTableNode nodeAfterReorder = this.content(this.render(data, afterReorder));
        assertEquals(2, nodeAfterReorder.getColumns()
                                        .get(0)
                                        .getSortPriority(),
                     "A shifted to priority 2");
        assertEquals(1, nodeAfterReorder.getColumns()
                                        .get(1)
                                        .getSortPriority(),
                     "B moved to priority 1");
        assertEquals(3, nodeAfterReorder.getColumns()
                                        .get(2)
                                        .getSortPriority(),
                     "C stays at priority 3");

        // Out-of-range (too low: 0) must be ignored - the sort list stays untouched (clamp/ignore defensively).
        Data withRequestedPriorityZero = this.threeColumnActiveSortData(sortFieldKey, bPriorityFieldKey, 0);
        Data afterOutOfRangeLow = bReorderHandler.invoke(withRequestedPriorityZero, Data.newInstance())
                                                 .getData();
        assertEquals(Arrays.asList("a:ASCENDING", "b:DESCENDING", "c:ASCENDING"), afterOutOfRangeLow.getFieldValue(sortFieldKey)
                                                                                                    .get()
                                                                                                    .asStringList(),
                     "an out-of-range (too low) requested priority must be ignored - the sort list stays unchanged");

        // Out-of-range (too high: 4, only 3 active columns) must be ignored.
        Data withRequestedPriorityFour = this.threeColumnActiveSortData(sortFieldKey, bPriorityFieldKey, 4);
        Data afterOutOfRangeHigh = bReorderHandler.invoke(withRequestedPriorityFour, Data.newInstance())
                                                  .getData();
        assertEquals(Arrays.asList("a:ASCENDING", "b:DESCENDING", "c:ASCENDING"), afterOutOfRangeHigh.getFieldValue(sortFieldKey)
                                                                                                     .get()
                                                                                                     .asStringList(),
                     "an out-of-range (too high) requested priority must be ignored - the sort list stays unchanged");

        // A no-op (B's OWN current position, 2) must also leave the list untouched.
        Data withRequestedPriorityTwo = this.threeColumnActiveSortData(sortFieldKey, bPriorityFieldKey, 2);
        Data afterNoOp = bReorderHandler.invoke(withRequestedPriorityTwo, Data.newInstance())
                                        .getData();
        assertEquals(Arrays.asList("a:ASCENDING", "b:DESCENDING", "c:ASCENDING"), afterNoOp.getFieldValue(sortFieldKey)
                                                                                           .get()
                                                                                           .asStringList(),
                     "requesting the column's own CURRENT position must be a no-op");
    }

    @Test
    public void testSortPriorityFieldAndTargetOnlyEmittedForActiveColumnsInMultiModeElseEmpty()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("x", Map.of("a", "1", "b", "2"), Collections.emptyList())),
                                                                                   false);
        TreeTableData multiData = TreeTableData.builder()
                                               .columns(Arrays.asList(TreeTableColumn.of("a", "A"), TreeTableColumn.of("b", "B")))
                                               .dataProvider(provider)
                                               .windowSize(500)
                                               .multiColumnSortEnabled(true)
                                               .build();

        // Baseline (no submitted Data): neither column is active yet - both must be null/empty.
        TreeTableNode baselineNode = this.content(this.render(multiData));
        for (TreeTableNode.ColumnNode column : baselineNode.getColumns())
        {
            assertNull(column.getSortPriorityFieldKey(), "an inactive column must not carry a sortPriorityFieldKey");
            assertTrue(column.getSortPriorityReorderTarget()
                             .isEmpty(),
                       "an inactive column must not carry a routable sortPriorityReorderTarget");
            assertEquals(0, column.getSortPriority());
        }

        // Only "a" active: "a" must carry the field/target, "b" must still be empty.
        RerenderingContainerNode baselineContainer = this.render(multiData);
        String sortFieldKey = this.sortFieldKeyFor(baselineContainer);
        Data onlyAActive = Data.newInstance()
                               .setFieldValue(sortFieldKey, Arrays.asList("a:ASCENDING"));
        TreeTableNode onlyANode = this.content(this.render(multiData, onlyAActive));
        TreeTableNode.ColumnNode columnA = onlyANode.getColumns()
                                                    .get(0);
        TreeTableNode.ColumnNode columnB = onlyANode.getColumns()
                                                    .get(1);
        assertNotNull(columnA.getSortPriorityFieldKey(), "an ACTIVE column in multi mode must carry a sortPriorityFieldKey");
        assertFalse(columnA.getSortPriorityReorderTarget()
                           .isEmpty(),
                    "an ACTIVE column in multi mode must carry a routable sortPriorityReorderTarget");
        assertEquals(1, columnA.getSortPriority());
        assertNull(columnB.getSortPriorityFieldKey(), "an INACTIVE column must not carry a sortPriorityFieldKey even in multi mode");
        assertTrue(columnB.getSortPriorityReorderTarget()
                          .isEmpty());
        assertEquals(0, columnB.getSortPriority());

        // Same active sort but SINGLE-column mode (multiColumnSortEnabled=false, default): the field/target must
        // be absent even for the active column - the priority re-rank control has no meaning outside multi mode.
        TreeTableData singleData = multiData.toBuilder()
                                            .multiColumnSortEnabled(false)
                                            .build();
        TreeTableNode singleModeNode = this.content(this.render(singleData, onlyAActive));
        TreeTableNode.ColumnNode singleColumnA = singleModeNode.getColumns()
                                                               .get(0);
        assertEquals(1, singleColumnA.getSortPriority(), "sortPriority is emitted regardless of mode");
        assertNull(singleColumnA.getSortPriorityFieldKey(), "single-column mode must never emit a sortPriorityFieldKey, even for the active column");
        assertTrue(singleColumnA.getSortPriorityReorderTarget()
                                .isEmpty(),
                   "single-column mode must never emit a routable sortPriorityReorderTarget");
    }
}
