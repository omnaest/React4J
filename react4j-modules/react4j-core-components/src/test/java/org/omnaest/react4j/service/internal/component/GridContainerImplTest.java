package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.GridContainerNode;
import org.omnaest.react4j.service.internal.nodes.GridContainerNode.CellNode;
import org.omnaest.react4j.service.internal.nodes.GridContainerNode.RowNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link GridContainerImpl} builder API ({@code addRow}/{@code addRowContent}/
 * {@code withLinkLocator}/{@code withUnlimitedColumns}) maps onto the produced {@link GridContainerNode} rows/cells,
 * including the default even-split column span.
 *
 * @see GridContainerImpl
 * @author omnaest
 */
public class GridContainerImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsHaveEmptyRowsAndLimitedColumns()
    {
        ComponentContext context = this.newContext();
        GridContainerImpl grid = new GridContainerImpl(context);

        Node node = grid.asRenderer()
                        .render(mock(RenderingProcessor.class), Location.of("root"), Optional.empty());

        GridContainerNode gridNode = (GridContainerNode) node;
        assertTrue(gridNode.getRows()
                           .isEmpty());
        assertEquals(false, gridNode.isUnlimitedColumns());
    }

    @Test
    public void testAddRowContentDefaultsColumnSpanToFullWidth()
    {
        ComponentContext context = this.newContext();
        GridContainerImpl grid = new GridContainerImpl(context);

        UIComponent<?> content = mock(UIComponent.class);
        grid.addRowContent(content);

        Node node = grid.asRenderer()
                        .render(mock(RenderingProcessor.class), Location.of("root"), Optional.empty());

        RowNode row = ((GridContainerNode) node).getRows()
                                                .get(0);
        assertEquals(1, row.getCells()
                           .size());
        assertEquals(12, row.getCells()
                            .get(0)
                            .getColspan());
    }

    @Test
    public void testTwoCellsInARowSplitColumnsEvenly()
    {
        ComponentContext context = this.newContext();
        GridContainerImpl grid = new GridContainerImpl(context);

        grid.addRow(row -> row.addCellContent(mock(UIComponent.class))
                              .addCellContent(mock(UIComponent.class)));

        Node node = grid.asRenderer()
                        .render(mock(RenderingProcessor.class), Location.of("root"), Optional.empty());

        List<CellNode> cells = ((GridContainerNode) node).getRows()
                                                         .get(0)
                                                         .getCells();
        assertEquals(2, cells.size());
        assertEquals(6, cells.get(0)
                             .getColspan());
        assertEquals(6, cells.get(1)
                             .getColspan());
    }

    @Test
    public void testExplicitColumnSpanOverridesDefaultSplit()
    {
        ComponentContext context = this.newContext();
        GridContainerImpl grid = new GridContainerImpl(context);

        grid.addRow(row -> row.addCell(cell -> cell.withColumnSpan(4)
                                                   .withContent(mock(UIComponent.class))));

        Node node = grid.asRenderer()
                        .render(mock(RenderingProcessor.class), Location.of("root"), Optional.empty());

        assertEquals(4, ((GridContainerNode) node).getRows()
                                                  .get(0)
                                                  .getCells()
                                                  .get(0)
                                                  .getColspan());
    }

    @Test
    public void testCellContentIsRendered()
    {
        ComponentContext context = this.newContext();
        GridContainerImpl grid = new GridContainerImpl(context);
        UIComponent<?> content = mock(UIComponent.class);
        grid.addRowContent(content);

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = Location.of("root");
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(org.mockito.ArgumentMatchers.eq(content), org.mockito.ArgumentMatchers.any())).thenReturn(contentNode);

        Node node = grid.asRenderer()
                        .render(renderingProcessor, location, Optional.empty());

        CellNode cell = ((GridContainerNode) node).getRows()
                                                  .get(0)
                                                  .getCells()
                                                  .get(0);
        assertSame(contentNode, cell.getContent());
    }

    @Test
    public void testWithLinkLocatorAndUnlimitedColumnsMapToNode()
    {
        ComponentContext context = this.newContext();
        GridContainerImpl grid = new GridContainerImpl(context);

        grid.withLinkLocator("my-grid");
        grid.withUnlimitedColumns();

        Node node = grid.asRenderer()
                        .render(mock(RenderingProcessor.class), Location.of("root"), Optional.empty());

        GridContainerNode gridNode = (GridContainerNode) node;
        assertEquals("my-grid", gridNode.getLocator());
        assertTrue(gridNode.isUnlimitedColumns());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        GridContainerImpl grid = new GridContainerImpl(context);
        grid.addRowContent(mock(UIComponent.class));
        grid.withLinkLocator("my-grid");

        GridContainerImpl templated = (GridContainerImpl) grid.asTemplateProvider()
                                                              .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), Location.of("root"), Optional.empty());

        GridContainerNode gridNode = (GridContainerNode) node;
        assertEquals(1, gridNode.getRows()
                                .size());
        assertEquals("my-grid", gridNode.getLocator());
    }
}
