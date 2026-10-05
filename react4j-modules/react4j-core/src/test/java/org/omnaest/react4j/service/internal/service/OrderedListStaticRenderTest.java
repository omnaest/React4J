package org.omnaest.react4j.service.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.ReactUIServiceImpl;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.internal.UIComponentFactoryServiceImpl;
import org.omnaest.utils.MapUtils;

/**
 * plan-283 S1 static-render fidelity test for {@code OrderedList}: through the REAL {@link NodeHierarchyStaticRenderer} pipeline, an ordered list renders as
 * an {@code <ol>} with one {@code <li>} per entry, and carries a {@code start} attribute only for a start number other than 1.
 * <p>
 * The static render of a single component is the whole output, so the assertions compare the complete markup (with the template's indentation removed)
 * rather than search it for a fragment.
 *
 * @see org.omnaest.react4j.service.internal.component.OrderedListImpl#asRenderer()
 */
public class OrderedListStaticRenderTest
{
    private ReactUIServiceImpl newUiService()
    {
        return new ReactUIServiceImpl() {
            {
                this.eventHandlerRegistry = Mockito.mock(EventHandlerRegistry.class);
                this.nodeHierarchyStaticRenderer = new NodeHierarchyStaticRenderer();
                this.uiComponentFactoryService = new UIComponentFactoryServiceImpl() {
                    {
                        this.textResolver = (text, location) -> new I18nTextValue(MapUtils.builder()
                                                                                          .put(LocalizedTextResolverService.DEFAULT_LOCALE_KEY,
                                                                                               text.getDefaultText())
                                                                                          .build());
                    }
                };
            }
        };
    }

    @Test
    public void testOrderedListRendersAnOlWithOneLiPerEntryAndNoStartAttributeByDefault() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newOrderedList()
                                                                                              .addText("one")
                                                                                              .addText("two")));

        assertEquals("<ol><li class=\"list-item\">one</li><li class=\"list-item\">two</li></ol>", this.render(uiService));
    }

    @Test
    public void testAStartNumberOfOneRendersNoStartAttribute() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newOrderedList()
                                                                                              .withStartNumber(1)
                                                                                              .addText("one")));

        assertEquals("<ol><li class=\"list-item\">one</li></ol>", this.render(uiService));
    }

    @Test
    public void testAStartNumberOtherThanOneRendersTheStartAttribute() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newOrderedList()
                                                                                              .withStartNumber(3)
                                                                                              .addText("three")
                                                                                              .addText("four")));

        assertEquals("<ol start=\"3\"><li class=\"list-item\">three</li><li class=\"list-item\">four</li></ol>", this.render(uiService));
    }

    @Test
    public void testAListNestedIntoAnEntryRendersInsideTheLiOfThatEntry() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newOrderedList()
                                                                                              .addEntry(this.itemWithANestedList(factory))
                                                                                              .addText("c")));

        // the outer div is the page body, itself a composite: it is wrapped as soon as the tree holds a composite, which registers the renderer for that node type
        assertEquals("<div><ol><li class=\"list-item\"><div>a<ol><li class=\"list-item\">b</li></ol></div></li><li class=\"list-item\">c</li></ol></div>",
                     this.render(uiService));
    }

    private UIComponent<?> itemWithANestedList(UIComponentFactory factory)
    {
        List<UIComponent<?>> entry = new ArrayList<>();
        entry.add(factory.newText()
                         .addText("a"));
        entry.add(factory.newOrderedList()
                         .addText("b"));
        return factory.newComposite()
                      .addComponents(entry);
    }

    private String render(ReactUIServiceImpl uiService)
    {
        // the template indents its markup with tabs and line breaks, which carry no meaning
        return uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML)
                        .replaceAll(">\\s+<", "><");
    }
}
