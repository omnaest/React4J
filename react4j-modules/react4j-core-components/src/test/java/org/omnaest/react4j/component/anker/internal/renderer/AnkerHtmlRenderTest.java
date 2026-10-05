package org.omnaest.react4j.component.anker.internal.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.anker.internal.renderer.node.AnkerNode;
import org.omnaest.react4j.component.anker.internal.renderer.node.AnkerNode.Page;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.AbstractNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

/**
 * The html rendering of an anker (plan-286 S1): the label is the text followed by the rendered children without any added whitespace, and an anker without
 * children renders exactly the anchor it always did.
 *
 * @see AnkerRenderer
 */
public class AnkerHtmlRenderTest
{
    private static final String LOCALE = "en";

    private static I18nTextValue text(String text)
    {
        return new I18nTextValue(Collections.singletonMap(LOCALE, text));
    }

    /**
     * Pinned BEFORE the children existed (green on the old code): the complete output of a text-only anker.
     */
    @Test
    public void testAnkerWithoutChildrenRendersTheAnchorItAlwaysDid()
    {
        String html = this.renderHtml(new AnkerNode().setLink("http://link.example")
                                                     .setPage(Page.BLANK)
                                                     .setText(text("Join us!")));

        assertEquals("<a href=\"http://link.example\" target=\"_blank\" rel=\"noopener noreferrer\">Join us!</a>", html.strip());
    }

    @Test
    public void testChildrenAreRenderedAfterTheTextWithoutAddedWhitespace()
    {
        Node bold = new FakeNode("<strong>b</strong>");
        Node code = new FakeNode("<code>c</code>");

        String html = this.renderHtml(new AnkerNode().setLink("http://link.example")
                                                     .setPage(Page.SELF)
                                                     .setText(text("lead "))
                                                     .setElements(List.of(bold, code)));

        assertEquals("<a href=\"http://link.example\" target=\"_self\" rel=\"noopener noreferrer\">lead <strong>b</strong><code>c</code></a>", html.strip());
    }

    @Test
    public void testChildrenWithoutATextRenderAsTheWholeLabel()
    {
        String html = this.renderHtml(new AnkerNode().setLink("http://link.example")
                                                     .setPage(Page.BLANK)
                                                     .setText(text(""))
                                                     .setElements(List.of(new FakeNode("<strong>only</strong>"))));

        assertEquals("<a href=\"http://link.example\" target=\"_blank\" rel=\"noopener noreferrer\"><strong>only</strong></a>", html.strip());
    }

    private String renderHtml(AnkerNode node)
    {
        AnkerRenderer renderer = new AnkerRenderer(null, null, null, null);
        CapturingNodeRendererRegistry registry = new CapturingNodeRendererRegistry();
        renderer.manageNodeRenderers(registry);
        assertEquals(NodeRenderType.HTML, registry.renderType);
        return registry.nodeRenderer.render(node, new NodeRenderingProcessor() {
            @Override
            public String render(Node node)
            {
                return ((FakeNode) node).html;
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

    /**
     * A node that renders as the given markup, so the test depends on the anker's template only and not on the renderer of any child kind.
     */
    private static class FakeNode extends AbstractNode
    {
        private final String html;

        private FakeNode(String html)
        {
            this.html = html;
        }

        @Override
        public String getType()
        {
            return "FAKE";
        }
    }

    private static class CapturingNodeRendererRegistry implements NodeRendererRegistry
    {
        private NodeRenderer<AnkerNode> nodeRenderer;
        private NodeRenderType          renderType;

        @SuppressWarnings("unchecked")
        @Override
        public <N extends Node> NodeRendererRegistry register(Class<N> nodeType, NodeRenderType renderType, NodeRenderer<N> nodeRenderer)
        {
            this.nodeRenderer = (NodeRenderer<AnkerNode>) nodeRenderer;
            this.renderType = renderType;
            return this;
        }
    }
}
