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

        // plan-320 S3a deliberate contract change: an external link used to render target="_self", it now opens in a new tab like NavigationBar.tsx
        assertTrue(html.contains("<li class=\"nav-item\"><a class=\"nav-link\" href=\"/first\" target=\"_blank\">First</a></li>"),
                   () -> "a plain entry renders as before, except that its external link opens in a new tab: " + html);
        assertTrue(html.contains("<li class=\"nav-item dropdown\"><a class=\"nav-link dropdown-toggle\" href=\"#\" role=\"button\" data-bs-toggle=\"dropdown\" aria-expanded=\"false\">More</a>"
                                 + "<ul class=\"dropdown-menu\">" + "<li><a class=\"dropdown-item\" href=\"/reports\" target=\"_blank\">Reports</a></li>"
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

    /**
     * Property 1 (plan-320 S3a): the static render carries the {@code active} and {@code disabled} classes exactly where {@code NavigationBar.tsx} puts them,
     * on a plain entry, on a dropdown toggle and on a dropdown item, and puts none where the state is not set.
     */
    @Test
    public void testActiveAndDisabledStateRenderAsClassesOnPlainEntriesTogglesAndItems()
    {
        NavigationBarImpl navigationBar = this.newNavigationBar();
        navigationBar.addEntry(entry -> entry.withText("PlainActive")
                                             .withLinkedLocator("pa")
                                             .withActiveState(true));
        navigationBar.addEntry(entry -> entry.withText("PlainDisabled")
                                             .withLinkedLocator("pd")
                                             .withDisabledState(true));
        navigationBar.addEntry(entry -> entry.withText("PlainBoth")
                                             .withLinkedLocator("pb")
                                             .withActiveState(true)
                                             .withDisabledState(true));
        navigationBar.addEntry(entry -> entry.withText("PlainNeutral")
                                             .withLinkedLocator("pn"));
        navigationBar.addDropdown(dropdown -> dropdown.withText("ToggleActive")
                                                      .withActiveState(true)
                                                      .addEntry(item -> item.withText("ItemActive")
                                                                            .withLinkedLocator("ia")
                                                                            .withActiveState(true))
                                                      .addEntry(item -> item.withText("ItemDisabled")
                                                                            .withLinkedLocator("id")
                                                                            .withDisabledState(true))
                                                      .addEntry(item -> item.withText("ItemBoth")
                                                                            .withLinkedLocator("ib")
                                                                            .withActiveState(true)
                                                                            .withDisabledState(true))
                                                      .addEntry(item -> item.withText("ItemNeutral")
                                                                            .withLinkedLocator("in")));
        navigationBar.addDropdown(dropdown -> dropdown.withText("ToggleDisabled")
                                                      .withDisabledState(true));
        navigationBar.addDropdown(dropdown -> dropdown.withText("ToggleBoth")
                                                      .withActiveState(true)
                                                      .withDisabledState(true));
        navigationBar.addDropdown(dropdown -> dropdown.withText("ToggleNeutral"));

        String html = this.renderStatic(navigationBar);

        this.assertContains(html, "<a class=\"nav-link active\" href=\"#pa\" target=\"_self\">PlainActive</a>");
        this.assertContains(html, "<a class=\"nav-link disabled\" href=\"#pd\" target=\"_self\">PlainDisabled</a>");
        this.assertContains(html, "<a class=\"nav-link active disabled\" href=\"#pb\" target=\"_self\">PlainBoth</a>");
        this.assertContains(html, "<a class=\"nav-link\" href=\"#pn\" target=\"_self\">PlainNeutral</a>");
        this.assertContains(html, "<a class=\"nav-link dropdown-toggle active\" href=\"#\" role=\"button\" data-bs-toggle=\"dropdown\" aria-expanded=\"false\">ToggleActive</a>");
        this.assertContains(html, "<a class=\"nav-link dropdown-toggle disabled\" href=\"#\" role=\"button\" data-bs-toggle=\"dropdown\" aria-expanded=\"false\">ToggleDisabled</a>");
        this.assertContains(html, "<a class=\"nav-link dropdown-toggle active disabled\" href=\"#\" role=\"button\" data-bs-toggle=\"dropdown\" aria-expanded=\"false\">ToggleBoth</a>");
        this.assertContains(html, "<a class=\"nav-link dropdown-toggle\" href=\"#\" role=\"button\" data-bs-toggle=\"dropdown\" aria-expanded=\"false\">ToggleNeutral</a>");
        this.assertContains(html, "<a class=\"dropdown-item active\" href=\"#ia\" target=\"_self\">ItemActive</a>");
        this.assertContains(html, "<a class=\"dropdown-item disabled\" href=\"#id\" target=\"_self\">ItemDisabled</a>");
        this.assertContains(html, "<a class=\"dropdown-item active disabled\" href=\"#ib\" target=\"_self\">ItemBoth</a>");
        this.assertContains(html, "<a class=\"dropdown-item\" href=\"#in\" target=\"_self\">ItemNeutral</a>");
    }

    /**
     * Property 2 (plan-320 S3a), first facet: an entry holding only a linked locator links to the in-page anchor and stays in the page.
     */
    @Test
    public void testAnEntryWithOnlyALinkedLocatorLinksToTheAnchorAndTargetsSelf()
    {
        NavigationBarImpl navigationBar = this.newNavigationBar();
        navigationBar.addEntry(entry -> entry.withText("Anchored")
                                             .withLinkedLocator("section-a"));
        navigationBar.addDropdown(dropdown -> dropdown.withText("More")
                                                      .addEntry(item -> item.withText("AnchoredItem")
                                                                            .withLinkedLocator("section-b")));

        String html = this.renderStatic(navigationBar);

        this.assertContains(html, "<a class=\"nav-link\" href=\"#section-a\" target=\"_self\">Anchored</a>");
        this.assertContains(html, "<a class=\"dropdown-item\" href=\"#section-b\" target=\"_self\">AnchoredItem</a>");
    }

    /**
     * Property 2, second facet: an entry holding only an external link opens it in a new tab.
     */
    @Test
    public void testAnEntryWithOnlyALinkOpensItInANewTab()
    {
        NavigationBarImpl navigationBar = this.newNavigationBar();
        navigationBar.addEntry(entry -> entry.withText("External")
                                             .withLink("https://example.org/"));
        navigationBar.addDropdown(dropdown -> dropdown.withText("More")
                                                      .addEntry(item -> item.withText("ExternalItem")
                                                                            .withLink("https://example.org/docs")));

        String html = this.renderStatic(navigationBar);

        this.assertContains(html, "<a class=\"nav-link\" href=\"https://example.org/\" target=\"_blank\">External</a>");
        this.assertContains(html, "<a class=\"dropdown-item\" href=\"https://example.org/docs\" target=\"_blank\">ExternalItem</a>");
    }

    /**
     * Property 2, third facet: with both set the linked locator wins the href, and because the resolved href is then not the external link the target is
     * {@code _self} (an in-page anchor must not open a new tab).
     */
    @Test
    public void testAnEntryWithBothTheLinkedLocatorWinsTheHrefAndTargetsSelf()
    {
        NavigationBarImpl navigationBar = this.newNavigationBar();
        navigationBar.addEntry(entry -> entry.withText("Both")
                                             .withLink("https://example.org/")
                                             .withLinkedLocator("section-a"));
        navigationBar.addDropdown(dropdown -> dropdown.withText("More")
                                                      .addEntry(item -> item.withText("BothItem")
                                                                            .withLinkedLocator("section-b")
                                                                            .withLink("https://example.org/docs")));

        String html = this.renderStatic(navigationBar);

        this.assertContains(html, "<a class=\"nav-link\" href=\"#section-a\" target=\"_self\">Both</a>");
        this.assertContains(html, "<a class=\"dropdown-item\" href=\"#section-b\" target=\"_self\">BothItem</a>");
        assertFalse(html.contains("target=\"_blank\""), () -> "no entry resolved to its external link, so none may open a new tab: " + html);
    }

    /**
     * Property 3 (plan-320 S3a): an entry with neither link nor linked locator renders without error and its anchor carries no {@code href} attribute at all
     * (not {@code href="null"}, not {@code href=""}), exactly as {@code NavigationBar.tsx} passes {@code href={undefined}}.
     */
    @Test
    public void testAnEntryWithNeitherLinkNorLinkedLocatorRendersNoHrefAttribute()
    {
        NavigationBarImpl navigationBar = this.newNavigationBar();
        navigationBar.addEntry(entry -> entry.withText("Nowhere"));
        navigationBar.addDropdown(dropdown -> dropdown.withText("More")
                                                      .addEntry(item -> item.withText("NowhereItem")));

        String html = this.renderStatic(navigationBar);

        this.assertContains(html, "<a class=\"nav-link\" target=\"_self\">Nowhere</a>");
        this.assertContains(html, "<a class=\"dropdown-item\" target=\"_self\">NowhereItem</a>");
        assertFalse(html.contains("href=\"null\""), () -> html);
        assertFalse(html.contains("href=\"\""), () -> html);
    }

    private void assertContains(String html, String expectedFragment)
    {
        assertTrue(html.contains(expectedFragment), () -> "expected <" + expectedFragment + "> in: " + html);
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
