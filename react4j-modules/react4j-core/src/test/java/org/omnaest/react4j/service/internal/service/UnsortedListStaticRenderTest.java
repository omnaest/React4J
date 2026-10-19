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
 * plan-320 S13 (kanban 3277cf97) static-render parity test for {@code UnsortedList}: through the REAL {@link NodeHierarchyStaticRenderer} pipeline, a list
 * without bullet points renders exactly as it always did (plan-261 policy criterion 2a, pinned BEFORE the renderer read the flag), and a list with bullet
 * points drops the {@code list-unstyled} class the way {@code UnorderedList.tsx} does.
 *
 * @see org.omnaest.react4j.service.internal.component.UnsortedListImpl#asRenderer()
 */
public class UnsortedListStaticRenderTest
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
     * Pinned BEFORE the static renderer read the bullet point flag: the complete output of a list without bullet points (policy criterion 2a).
     */
    @Test
    public void testListWithoutBulletPointsRendersAsItAlwaysDid() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newUnsortedList()
                                                                                              .addEntry(factory.newText().addText("one"))
                                                                                              .addEntry(factory.newText().addText("two"))));

        assertEquals("<ul class=\"list-unstyled\">\n\t\t<li class=\"list-item\">one</li>\n\t<li class=\"list-item\">two</li>\n</ul>", this.render(uiService));
    }

    /**
     * UnorderedList.tsx line 22: with bullet points the list carries no {@code list-unstyled} class (React emits an empty class attribute, which is the same
     * to a browser as none).
     */
    @Test
    public void testListWithBulletPointsHasNoUnstyledClass() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newUnsortedList()
                                                                                              .enableBulletPoints()
                                                                                              .addEntry(factory.newText().addText("one"))
                                                                                              .addEntry(factory.newText().addText("two"))));

        assertEquals("<ul>\n\t\t<li class=\"list-item\">one</li>\n\t<li class=\"list-item\">two</li>\n</ul>", this.render(uiService));
    }

    /**
     * Switching the bullet points off again is the unset state: the unstyled class is back.
     */
    @Test
    public void testBulletPointsSwitchedOffAgainRenderWithTheUnstyledClass() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newUnsortedList()
                                                                                              .enableBulletPoints()
                                                                                              .enableBulletPoints(false)
                                                                                              .addEntry(factory.newText().addText("one"))));

        assertEquals("<ul class=\"list-unstyled\">\n\t\t<li class=\"list-item\">one</li>\n</ul>", this.render(uiService));
    }
}
