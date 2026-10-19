package org.omnaest.react4j.service.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.ReactUIServiceImpl;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.internal.UIComponentFactoryServiceImpl;
import org.omnaest.utils.MapUtils;

/**
 * plan-320 S13 (kanban 3277cf97) static-render parity test for {@code Jumbotron}: through the REAL {@link NodeHierarchyStaticRenderer} pipeline, a
 * jumbotron that is not full width renders exactly as it always did (plan-261 policy criterion 2a, pinned BEFORE the renderer read the flag), and a full
 * width one carries the {@code jumbotron-fluid} class {@code JumboTron.tsx} adds.
 *
 * @see org.omnaest.react4j.service.internal.component.JumbotronImpl#asRenderer()
 */
public class JumbotronStaticRenderTest
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
     * Pinned BEFORE the static renderer read the full width flag: the complete output of a jumbotron that is not full width (policy criterion 2a).
     */
    @Test
    public void testJumbotronThatIsNotFullWidthRendersAsItAlwaysDid() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newJumboTron()
                                                                                              .withTitle("Title")
                                                                                              .withSubTitle("Sub")
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        assertEquals("<div class=\"jumbotron\">\n\t<h2 class=\"display-4\">Title</h2>\n\t<p class=\"lead\">Sub</p>\n\t) <br></br> Body\n</div>", this.render(uiService));
    }

    /**
     * JumboTron.tsx line 29: full width adds {@code jumbotron-fluid} to the {@code jumbotron} class.
     */
    @Test
    public void testFullWidthAddsTheFluidClass() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newJumboTron()
                                                                                              .withTitle("Title")
                                                                                              .withSubTitle("Sub")
                                                                                              .withFullWidth()
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.startsWith("<div class=\"jumbotron jumbotron-fluid\">"), () -> "expected the fluid jumbotron in: " + html);
    }

    /**
     * Switching full width off again is the unset state: no fluid class.
     */
    @Test
    public void testFullWidthSwitchedOffAgainRendersWithoutTheFluidClass() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newJumboTron()
                                                                                              .withFullWidth()
                                                                                              .withFullWidth(false)
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertFalse(html.contains("jumbotron-fluid"), () -> "expected no fluid class in: " + html);
    }
}
