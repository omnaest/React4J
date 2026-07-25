package org.omnaest.react4j.component.treetable.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.treetable.TreeTable;
import org.omnaest.react4j.component.treetable.internal.renderer.node.TreeTableNode;
import org.omnaest.react4j.component.treetable.internal.renderer.node.TreeTableNode.RowEntryNode;
import org.omnaest.react4j.component.treetable.provider.InMemoryTreeTableDataProvider;
import org.omnaest.react4j.component.treetable.provider.InMemoryTreeTableDataProvider.Node;
import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.component.treetable.provider.TreeTableDataProvider;
import org.omnaest.react4j.component.treetable.provider.TreeTableQuery;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.react4j.service.internal.component.RerenderingContainerImpl;
import org.omnaest.react4j.service.internal.nodes.RerenderingContainerNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Tests {@link TreeTableImpl#refresh()} (plan-76 Slice 7, AC6): "refresh() re-fetches through the same
 * {@code provider.fetch} seam" — see {@link TreeTable#refresh()}'s javadoc for the full, honest contract (this
 * component never caches a fetch result, so {@code refresh()} is a documented no-op relative to the ALREADY-guaranteed
 * fresh-fetch-per-render behavior; there is no separate/cached path for it to diverge from).
 *
 * @author omnaest
 */
public class TreeTableImplTest
{
    /**
     * Builds a {@link ComponentContext} mock whose {@link ComponentContext#getUiComponentFactory()} answers a
     * {@link UIComponentFactory} mock wired back to the SAME context (unlike {@code TreeTableRendererImplTest},
     * which constructs {@code TreeTableRendererImpl} directly with an explicit factory argument, this test goes
     * through the real production seam {@code TreeTableImpl.asRenderer()} -&gt; {@code getUiComponentFactory()} -&gt;
     * {@code context.getUiComponentFactory()}, so the mock context must serve the factory itself).
     */
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        UIComponentFactory factory = mock(UIComponentFactory.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        when(context.getUiComponentFactory()).thenReturn(factory);
        when(factory.newRerenderingContainer()).thenAnswer(invocation -> new RerenderingContainerImpl(context));
        return context;
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

    private TreeTableNode render(TreeTableImpl treeTable, Data submittedData)
    {
        RenderableUIComponent<?> renderable = (RenderableUIComponent<?>) treeTable;
        UIComponentRenderer renderer = renderable.asRenderer();
        Location location = renderer.getLocation(this.rootLocationSupport());
        org.omnaest.react4j.domain.raw.Node node = renderer.render(this.realRenderingProcessor(), location, Optional.ofNullable(submittedData));
        return (TreeTableNode) ((RerenderingContainerNode) node).getContent();
    }

    @Test
    public void testRefreshReturnsTheSameInstanceAndLeavesConfiguredStateUnchanged()
    {
        ComponentContext context = this.newContext();

        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "Alpha"), Collections.emptyList())), false);
        TreeTableImpl treeTable = new TreeTableImpl(context);
        treeTable.withColumns(TreeTableColumn.of("name", "Name"))
                 .withDataProvider(provider)
                 .withWindowSize(3);

        TreeTable refreshed = treeTable.refresh();

        assertSame(treeTable, refreshed, "refresh() must return this for fluent chaining, mirroring withColumns/withDataProvider/withWindowSize");

        TreeTableNode node = this.render(treeTable, null);
        assertEquals(1, node.getColumns()
                            .size(),
                     "refresh() must not mutate the previously-configured columns");
        assertEquals("name", node.getColumns()
                                 .get(0)
                                 .getKey());
        List<RowEntryNode> rows = node.getRows();
        assertEquals(1, rows.size());
        assertEquals("a", rows.get(0)
                              .getNodeId(),
                     "refresh() must not swap out the previously-configured dataProvider");
    }

    @Test
    public void testRefreshDoesNotBypassTheProviderFetchSeamAcrossRepeatedRenders()
    {
        ComponentContext context = this.newContext();

        List<TreeTableQuery> recordedQueries = new ArrayList<>();
        InMemoryTreeTableDataProvider delegate = new InMemoryTreeTableDataProvider(Arrays.asList(new Node("a", Map.of("name", "A"), Collections.emptyList()),
                                                                                                 new Node("b", Map.of("name", "B"), Collections.emptyList()),
                                                                                                 new Node("c", Map.of("name", "C"), Collections.emptyList())),
                                                                                   false);
        TreeTableDataProvider trackingProvider = query ->
        {
            recordedQueries.add(query);
            return delegate.fetch(query);
        };

        TreeTableImpl treeTable = new TreeTableImpl(context);
        treeTable.withColumns(TreeTableColumn.of("name", "Name"))
                 .withDataProvider(trackingProvider)
                 .withWindowSize(1);

        // Pass 1: an ordinary render (stands in for the interactive re-query seam) — exactly one fetch, capped by
        // the configured window size of 1.
        TreeTableNode firstRender = this.render(treeTable, null);
        assertEquals(1, recordedQueries.size(), "the first render must call provider.fetch exactly once");
        assertEquals(1, firstRender.getRows()
                                   .size());

        // Derive the grid's own window field key from row 0's target the SAME way
        // TreeTableRendererImplTest.testSubmittedDataWindowFieldWidensSameRenderRowWindow does, rather than
        // hardcoding the Location path.
        List<String> row0TargetPath = firstRender.getRows()
                                                 .get(0)
                                                 .getTarget()
                                                 .get();
        String windowFieldKey = "treetable." + String.join(".",
                                                           row0TargetPath.subList(0, row0TargetPath.size() - 1))
                                + ".root.windowLimit";

        // Explicit refresh(), per AC6, must sit on the SAME seam: it must neither add a second, divergent fetch nor
        // suppress the next render's fetch. Since there is no cache to invalidate (see refresh() javadoc), calling
        // it does not itself trigger a fetch — the NEXT render still does, with whatever query state is CURRENT at
        // that time.
        assertSame(treeTable, treeTable.refresh());
        assertEquals(1, recordedQueries.size(), "refresh() itself must not perform an out-of-seam fetch outside of a render pass");

        // Pass 2: a second render with a WIDENED window (simulating the query state having moved on, e.g. via an
        // earlier load-more click) must still route through the exact same provider.fetch seam and reflect the
        // CURRENT query — proving there is no cached page surviving from before the refresh() call that a
        // divergent path could have returned instead.
        recordedQueries.clear();
        Data widenedWindow = Data.newInstance()
                                 .setFieldValue(windowFieldKey, 3);
        TreeTableNode secondRender = this.render(treeTable, widenedWindow);
        assertEquals(1, recordedQueries.size(), "the second render (post-refresh) must ALSO call provider.fetch exactly once, through the same seam");
        assertEquals(0L, recordedQueries.get(0)
                                        .getOffset());
        assertEquals(3, secondRender.getRows()
                                    .size(),
                     "the second render must reflect the CURRENT query state, proving no stale/cached page from before refresh() survives");
    }
}
