package org.omnaest.react4j.component.ankerbutton.internal.renderer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.ankerbutton.internal.renderer.node.AnkerButtonNode;
import org.omnaest.react4j.component.ankerbutton.internal.renderer.node.AnkerButtonNode.Page;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

/**
 * The html rendering of a link button has to match what the react frontend renders: the button style must not get lost and an anchor opening another page needs
 * the noopener relation.
 *
 * @author omnaest
 */
public class AnkerButtonHtmlRenderTest
{
    private static final String LOCALE = "en";

    private static I18nTextValue newI18nTextValue(String text)
    {
        return new I18nTextValue(Collections.singletonMap(LOCALE, text));
    }

    @Test
    public void testRenderHtmlWithStyleAndRelation() throws Exception
    {
        String html = this.renderHtml(new AnkerButtonNode().setLink("http://link.example")
                                                           .setStyle("success")
                                                           .setPage(Page.BLANK)
                                                           .setText(newI18nTextValue("Join us!")));
        assertTrue(html.contains("btn-success"), html);
        assertTrue(html.contains("rel=\"noopener noreferrer\""), html);
        assertTrue(html.contains("target=\"_blank\""), html);
        assertTrue(html.contains("Join us!"), html);
    }

    @Test
    public void testRenderHtmlOfSamePageAnker() throws Exception
    {
        String html = this.renderHtml(new AnkerButtonNode().setLink("#locator")
                                                           .setStyle("primary")
                                                           .setPage(Page.SELF)
                                                           .setText(newI18nTextValue("Jump")));
        assertTrue(html.contains("target=\"_self\""), html);
    }

    private String renderHtml(AnkerButtonNode node)
    {
        AnkerButtonRenderer renderer = new AnkerButtonRenderer(null, null, null);
        CapturingNodeRendererRegistry registry = new CapturingNodeRendererRegistry();
        renderer.manageNodeRenderers(registry);
        return registry.nodeRenderer.render(node, new NodeRenderingProcessor() {
            @Override
            public String render(org.omnaest.react4j.domain.raw.Node node)
            {
                return String.valueOf(node);
            }

            @Override
            public String render(I18nTextValue text)
            {
                return text.getLocaleToText()
                           .get(LOCALE);
            }

            @Override
            public String render(org.omnaest.react4j.component.value.node.ValueNode value)
            {
                return String.valueOf(value);
            }
        });
    }

    private static class CapturingNodeRendererRegistry implements NodeRendererRegistry
    {
        private NodeRenderer<AnkerButtonNode> nodeRenderer;

        @SuppressWarnings("unchecked")
        @Override
        public <N extends org.omnaest.react4j.domain.raw.Node> NodeRendererRegistry register(Class<N> nodeType, NodeRenderType renderType,
                                                                                             NodeRenderer<N> nodeRenderer)
        {
            this.nodeRenderer = (NodeRenderer<AnkerButtonNode>) nodeRenderer;
            return this;
        }
    }

}
