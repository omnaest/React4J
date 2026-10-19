package org.omnaest.react4j.service.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omnaest.react4j.component.table.Table;
import org.omnaest.react4j.component.table.Table.TableSize;
import org.omnaest.react4j.component.table.internal.factory.TableFactory;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.ReactUIServiceImpl;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.internal.CustomUIComponentFactoryManager;
import org.omnaest.react4j.service.internal.service.internal.UIComponentFactoryServiceImpl;
import org.omnaest.utils.MapUtils;

/**
 * plan-320 S13 (kanban 3277cf97) static-render parity test for {@code Table}: through the REAL {@link NodeHierarchyStaticRenderer} pipeline, a table with
 * the default options renders exactly as it always did (plan-261 policy criterion 2a, pinned BEFORE the renderer read the options), and a table with a
 * size is wrapped in the {@code table-responsive-<size>} element {@code Table.tsx} wraps it in. The wrapper of the DEFAULT options (responsive, no size)
 * is not rendered statically, by documented design, see {@code Table#withResponsiveness}.
 *
 * @see org.omnaest.react4j.component.table.internal.renderer.TableRendererImpl#manageNodeRenderers(org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry)
 */
public class TableStaticRenderTest
{
    private ReactUIServiceImpl newUiService()
    {
        return new ReactUIServiceImpl() {
            {
                this.eventHandlerRegistry = Mockito.mock(EventHandlerRegistry.class);
                this.nodeHierarchyStaticRenderer = new NodeHierarchyStaticRenderer();
                this.uiComponentFactoryService = new UIComponentFactoryServiceImpl() {
                    {
                        // Table is a custom component, created by its factory: the manager is given exactly that one
                        this.customUIComponentFactoryManager = new CustomUIComponentFactoryManager() {
                            {
                                this.uiComponentFactories = List.of(new TableFactory());
                            }
                        };
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

    private ReactUIServiceImpl tableWith(UnaryOperator<Table> options)
    {
        ReactUIServiceImpl uiService = this.newUiService();
        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> options.apply(factory.newTable()
                                                                                                            .withColumnTitles("Col")
                                                                                                            .addRowTextContent(List.of("cell")))));
        return uiService;
    }

    /**
     * Pinned BEFORE the static renderer read the options: the complete output of a table with the default options (policy criterion 2a).
     */
    @Test
    public void testTableWithDefaultOptionsRendersAsItAlwaysDid() throws Exception
    {
        assertEquals("<table class=\"table\">\n\t<thead>\n\t\t<tr>\n\t\t\t\t\t\t<th scope=\"col\">Col</th>\n\t\t</tr>\n\t</thead>\n\t<tbody>\n\t\t<tr>\n\t\t\t<td>cell</td>\n\t\t</tr>\n\t</tbody>\n</table>",
                     this.render(this.tableWith(table -> table)));
    }

    /**
     * Table.tsx lines 32-34: a table with a size (and the default responsiveness) is wrapped in {@code div.table-responsive-<size>}.
     */
    @Test
    public void testSizedResponsiveTableIsWrappedInTheSizedResponsiveElement() throws Exception
    {
        String html = this.render(this.tableWith(table -> table.withSize(TableSize.SMALL)));
        String plain = this.render(this.tableWith(table -> table));

        assertEquals("<div class=\"table-responsive-sm\">" + plain + "</div>", html);
    }

    /**
     * Every size maps to its own suffix, the regular size (empty identifier) to none.
     */
    @Test
    public void testEverySizeUsesItsOwnSuffix() throws Exception
    {
        assertTrue(this.render(this.tableWith(table -> table.withSize(TableSize.MEDIUM)))
                       .startsWith("<div class=\"table-responsive-md\"><table"));
        assertTrue(this.render(this.tableWith(table -> table.withSize(TableSize.LARGE)))
                       .startsWith("<div class=\"table-responsive-lg\"><table"));
        assertTrue(this.render(this.tableWith(table -> table.withSize(TableSize.EXTRA_LARGE)))
                       .startsWith("<div class=\"table-responsive-xl\"><table"));
        assertTrue(this.render(this.tableWith(table -> table.withSize(TableSize.EXTRA_EXTRA_LARGE)))
                       .startsWith("<div class=\"table-responsive-xxl\"><table"));
        assertTrue(this.render(this.tableWith(table -> table.withSize(TableSize.REGULAR)))
                       .startsWith("<table"));
    }

    /**
     * Table.tsx line 33: without responsiveness there is no wrapper, whatever the size.
     */
    @Test
    public void testASizedTableWithoutResponsivenessIsNotWrapped() throws Exception
    {
        String html = this.render(this.tableWith(table -> table.withSize(TableSize.SMALL)
                                                               .withDisabledResponsiveness()));

        assertTrue(html.startsWith("<table"), () -> "expected the table without a wrapper in: " + html);
        assertFalse(html.contains("table-responsive"), () -> "expected no responsive class in: " + html);
    }

    /**
     * The documented structural divergence: the default options are responsive with no size, which {@code Table.tsx} wraps in {@code div.table-responsive};
     * the static render has never emitted that wrapper and keeps not emitting it (policy criterion 2a), so an explicit {@code withResponsiveness(true)}
     * renders like the default. Pinned so that the javadoc on {@code Table#withResponsiveness} stays true.
     */
    @Test
    public void testExplicitResponsivenessWithoutASizeRendersLikeTheDefault() throws Exception
    {
        assertEquals(this.render(this.tableWith(table -> table)), this.render(this.tableWith(table -> table.withResponsiveness(true))));
    }
}
