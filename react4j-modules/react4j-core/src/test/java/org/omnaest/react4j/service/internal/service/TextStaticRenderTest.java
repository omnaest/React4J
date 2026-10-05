package org.omnaest.react4j.service.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omnaest.react4j.domain.Text.Style;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.ReactUIServiceImpl;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.internal.UIComponentFactoryServiceImpl;
import org.omnaest.utils.MapUtils;

/**
 * plan-283 S1 static-render fidelity test for {@code Text}: through the REAL {@link NodeHierarchyStaticRenderer} pipeline, a {@code Text} without a style
 * renders exactly as it always did (its escaped texts joined by a blank, no element of its own), and a styled one wraps the same texts in one element.
 *
 * @see org.omnaest.react4j.service.internal.component.TextImpl#asRenderer()
 */
public class TextStaticRenderTest
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

    /**
     * Pinned BEFORE {@code Text} gained a style: the static render of an unstyled text.
     */
    @Test
    public void testUnstyledTextRendersItsEscapedTextsJoinedByABlankWithoutAnyElement() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newText()
                                                                                              .addText("Hello")
                                                                                              .addText("A & B <x>")));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertEquals("Hello A &amp; B &lt;x&gt;", html);
    }

    /**
     * {@code withStyle(null)} is the unset state, not a style of its own: the render is the pinned unstyled one.
     */
    @Test
    public void testStyleOfNullRendersAsTheUnstyledText() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newText()
                                                                                              .addText("Hello")
                                                                                              .addText("A & B <x>")
                                                                                              .withStyle(Style.MUTED)
                                                                                              .withStyle(null)));

        assertEquals("Hello A &amp; B &lt;x&gt;", uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML));
    }

    /**
     * A muted text wraps ALL its texts in exactly one span carrying the mapped theme class, and the texts are escaped exactly as without a style.
     */
    @Test
    public void testMutedTextWrapsTheEscapedTextsInOneSpanWithTheMappedClass() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newText()
                                                                                              .addText("Hello")
                                                                                              .addText("A & B <x>")
                                                                                              .withStyle(Style.MUTED)));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertEquals("<span class=\"" + Style.MUTED.toCssClass() + "\">Hello A &amp; B &lt;x&gt;</span>", html);
    }
}
