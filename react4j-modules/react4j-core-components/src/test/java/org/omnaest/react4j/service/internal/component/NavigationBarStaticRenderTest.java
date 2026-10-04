package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.NavigationBarNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * The static HTML render of a {@link NavigationBarImpl}: plain entries render exactly as before, a dropdown renders as
 * {@code li.nav-item.dropdown > a.nav-link.dropdown-toggle + ul.dropdown-menu > li > a.dropdown-item}, each item with the href a plain entry would get.
 *
 * @see NavigationBarImpl
 */
public class NavigationBarStaticRenderTest
{
    @Test
    public void testDropdownRendersAsToggleAndMenuAndPlainEntriesRenderAsBefore()
    {
        UIComponent<?> linkedComponent = mock(UIComponent.class);
        when(linkedComponent.getId()).thenReturn("section-id");

        NavigationBarImpl navigationBar = this.newNavigationBar();
        navigationBar.addEntry(entry -> entry.withText("First")
                                             .withLink("/first"));
        navigationBar.addDropdown(dropdown -> dropdown.withText("More")
                                                      .addEntry(item -> item.withText("Reports")
                                                                            .withLink("/reports"))
                                                      .addEntry(item -> item.withText("Section")
                                                                            .withLinked(linkedComponent)));
        navigationBar.addEntry(entry -> entry.withText("Last")
                                             .withLinkedLocator("last-id"));

        String html = this.renderStatic(navigationBar);

        assertTrue(html.contains("<li class=\"nav-item\"><a class=\"nav-link\" href=\"/first\" target=\"_self\">First</a></li>"),
                   () -> "a plain entry renders as before: " + html);
        assertTrue(html.contains("<li class=\"nav-item dropdown\"><a class=\"nav-link dropdown-toggle\" href=\"#\" role=\"button\" data-bs-toggle=\"dropdown\" aria-expanded=\"false\">More</a>"
                                 + "<ul class=\"dropdown-menu\">" + "<li><a class=\"dropdown-item\" href=\"/reports\" target=\"_self\">Reports</a></li>"
                                 + "<li><a class=\"dropdown-item\" href=\"#section-id\" target=\"_self\">Section</a></li>" + "</ul></li>"),
                   () -> "a dropdown renders as toggle plus menu, items with the href of a plain entry (a linked locator becomes #id): " + html);
        assertTrue(html.contains("<li class=\"nav-item\"><a class=\"nav-link\" href=\"#last-id\" target=\"_self\">Last</a></li>"),
                   () -> "a plain entry after the dropdown renders as before: " + html);
        assertTrue(html.indexOf("First") < html.indexOf("More") && html.indexOf("More") < html.indexOf("Last"), () -> "insertion order is kept: " + html);
    }

    @Test
    public void testAnEmptyDropdownRendersAToggleWithAnEmptyMenu()
    {
        NavigationBarImpl navigationBar = this.newNavigationBar();
        navigationBar.addDropdown(dropdown -> dropdown.withText("Empty"));

        String html = this.renderStatic(navigationBar);

        assertTrue(html.contains("class=\"nav-link dropdown-toggle\""), () -> html);
        assertTrue(html.contains("<ul class=\"dropdown-menu\"></ul>"), () -> html);
        assertFalse(html.contains("dropdown-item"), () -> html);
    }

    private NavigationBarImpl newNavigationBar()
    {
        ComponentContext context = mock(ComponentContext.class);
        LocalizedTextResolverService textResolver = mock(LocalizedTextResolverService.class);
        when(textResolver.apply(any(I18nText.class), any(Location.class))).thenAnswer(invocation -> new I18nTextValue(Map.of("DEFAULT", invocation.<I18nText>getArgument(0)
                                                                                                                                                  .getDefaultText())));
        when(context.getTextResolver()).thenReturn(textResolver);
        return new NavigationBarImpl(context);
    }

    /**
     * Renders through the component's own registered HTML {@link NodeRenderer}, then collapses whitespace so the assertions read the markup and not its
     * indentation
     */
    private String renderStatic(NavigationBarImpl navigationBar)
    {
        Node node = navigationBar.asRenderer()
                                 .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        CapturingNodeRendererRegistry registry = new CapturingNodeRendererRegistry();
        navigationBar.asRenderer()
                     .manageNodeRenderers(registry);
        assertEquals(NodeRenderType.HTML, registry.renderType);

        String html = registry.nodeRenderer.render((NavigationBarNode) node, new NodeRenderingProcessor() {
            @Override
            public String render(Node node)
            {
                return String.valueOf(node);
            }

            @Override
            public String render(I18nTextValue text)
            {
                return text.getLocaleToText()
                           .get("DEFAULT");
            }

            @Override
            public String render(org.omnaest.react4j.component.value.node.ValueNode value)
            {
                return String.valueOf(value);
            }
        });
        return html.replaceAll("\\s+", " ")
                   .replaceAll(">\\s+<", "><");
    }

    private static class CapturingNodeRendererRegistry implements NodeRendererRegistry
    {
        private NodeRenderer<NavigationBarNode> nodeRenderer;
        private NodeRenderType                  renderType;

        @SuppressWarnings("unchecked")
        @Override
        public <N extends Node> NodeRendererRegistry register(Class<N> nodeType, NodeRenderType renderType, NodeRenderer<N> nodeRenderer)
        {
            this.nodeRenderer = (NodeRenderer<NavigationBarNode>) nodeRenderer;
            this.renderType = renderType;
            return this;
        }
    }
}
