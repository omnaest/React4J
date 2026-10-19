package org.omnaest.react4j.service.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.ReactUIServiceImpl;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.internal.UIComponentFactoryServiceImpl;
import org.omnaest.utils.MapUtils;

/**
 * plan-320 S13 (kanban 3277cf97) static-render parity test for {@code GridContainer}: through the REAL {@link NodeHierarchyStaticRenderer} pipeline, a
 * grid without unlimited columns renders exactly as it always did (plan-261 policy criterion 2a, pinned BEFORE the renderer read the flag), a grid with
 * unlimited columns carries the {@code unlimited-columns} class {@code Container.tsx} adds, and the cell column span is - by documented design, see
 * {@code GridContainer.Cell#withColumnSpan} - not rendered statically.
 *
 * @see org.omnaest.react4j.service.internal.component.GridContainerImpl#asRenderer()
 */
public class GridContainerStaticRenderTest
{
    private ReactUIServiceImpl newUiService()
    {
        return new ReactUIServiceImpl() {
            {
                this.eventHandlerRegistry = Mockito.mock(EventHandlerRegistry.class);
                this.nodeHierarchyStaticRenderer = new NodeHierarchyStaticRenderer();
                this.uiComponentFactoryService = new UIComponentFactoryServiceImpl() {
                    {
                        // like the real resolver, an unset (null) text is resolved into an empty one
                        this.textResolver = (text, location) -> new I18nTextValue(MapUtils.builder()
                                                                                          .put(LocalizedTextResolverService.DEFAULT_LOCALE_KEY,
                                                                                               text == null ? "" : text.getDefaultText())
                                                                                          .build());
                    }
                };
            }
        };
    }

    private String render(ReactUIServiceImpl uiService) throws Exception
    {
        return uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML)
                        .replace("\r\n", "\n");
    }

    /**
     * Pinned BEFORE the static renderer read the unlimited columns flag: the complete output of a grid with limited columns (policy criterion 2a).
     */
    @Test
    public void testGridWithLimitedColumnsRendersAsItAlwaysDid() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newGridContainer()
                                                                                              .withLinkLocator("loc")
                                                                                              .addRow(row -> row.addCellContent(factory.newText()
                                                                                                                                       .addText("A"))
                                                                                                                .addCellContent(factory.newText()
                                                                                                                                       .addText("B")))));

        assertEquals("<div id=\"loc\" class=\"container-fluidpage-content\">\n\t A B  \n</div>\n", this.render(uiService));
    }

    /**
     * Container.tsx line 26: unlimited columns add {@code unlimited-columns} to the container's class.
     */
    @Test
    public void testUnlimitedColumnsAddTheUnlimitedColumnsClass() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newGridContainer()
                                                                                              .withLinkLocator("loc")
                                                                                              .withUnlimitedColumns()
                                                                                              .addRow(row -> row.addCellContent(factory.newText()
                                                                                                                                       .addText("A"))
                                                                                                                .addCellContent(factory.newText()
                                                                                                                                       .addText("B")))));

        String html = this.render(uiService);

        assertEquals("<div id=\"loc\" class=\"container-fluidpage-content unlimited-columns\">\n\t A B  \n</div>\n", html);
    }

    /**
     * The documented structural divergence: the static grid has no row and cell wrappers, so a cell's column span (Cell.tsx line 22, {@code col-md-N})
     * changes nothing statically. Pinned so that the javadoc on {@code Cell#withColumnSpan} stays true.
     */
    @Test
    public void testCellColumnSpanDoesNotChangeTheStaticOutput() throws Exception
    {
        ReactUIServiceImpl spanned = this.newUiService();
        spanned.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newGridContainer()
                                                                                            .withLinkLocator("loc")
                                                                                            .addRow(row -> row.addCell(cell -> cell.withColumnSpan(3)
                                                                                                                                   .withContent(factory.newText()
                                                                                                                                                       .addText("A")))
                                                                                                              .addCell(cell -> cell.withColumnSpan(9)
                                                                                                                                   .withContent(factory.newText()
                                                                                                                                                       .addText("B"))))));
        ReactUIServiceImpl unspanned = this.newUiService();
        unspanned.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newGridContainer()
                                                                                              .withLinkLocator("loc")
                                                                                              .addRow(row -> row.addCellContent(factory.newText()
                                                                                                                                       .addText("A"))
                                                                                                                .addCellContent(factory.newText()
                                                                                                                                       .addText("B")))));

        assertEquals(this.render(unspanned), this.render(spanned));
    }
}
