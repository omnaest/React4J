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
 * plan-320 S13 (kanban 3277cf97) static-render parity test for {@code Card}: through the REAL {@link NodeHierarchyStaticRenderer} pipeline, a card whose
 * optional fields are all unset renders exactly as it always did (plan-261 policy criterion 2a, pinned BEFORE the static renderer started to read the
 * other fields), and every field {@code Card.tsx} honours reaches the static HTML as the markup {@code Card.tsx} emits for it.
 *
 * @see org.omnaest.react4j.service.internal.component.CardImpl#asRenderer()
 */
public class CardStaticRenderTest
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
     * Pinned BEFORE the static renderer read any optional field: the complete output of a card with content only (policy criterion 2a, the default case).
     */
    @Test
    public void testCardWithContentOnlyRendersAsItAlwaysDid() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        assertEquals("<div className=\"card\">\n\t<h5 id=\"\" className=\"card-header\"></h5>\n\t<div className=\"card-body\">\n\t\t<div>Body</div>\n\t</div>\n</div>", this.render(uiService));
    }

    /**
     * Card.tsx line 60: the featured title (what {@code withTitle} sets on a card without an image) is a {@code BSCard.Title} in the body.
     */
    @Test
    public void testFeaturedTitleRendersAsTheCardTitleInTheBody() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withTitle("Featured")
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.contains("<div class=\"card-title h5\">Featured</div>"), () -> "expected the featured title as card-title in: " + html);
    }

    /**
     * Card.tsx lines 49 and 62: the sub title is an {@code h6.card-subtitle.mb-2.text-muted} in the body.
     */
    @Test
    public void testSubTitleRendersAsAMutedHeadingInTheBody() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withSubTitle("Sub")
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.contains("<h6 class=\"card-subtitle mb-2 text-muted\">Sub</h6>"), () -> "expected the sub title heading in: " + html);
    }

    /**
     * Card.tsx lines 57 and 67: header and footer are {@code .card-header} / {@code .card-footer} elements, header before and footer after the body.
     */
    @Test
    public void testHeaderAndFooterRenderBeforeAndAfterTheBody() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withHeader(factory.newText()
                                                                                                                 .addText("HEAD"))
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))
                                                                                              .withFooter(factory.newText()
                                                                                                                 .addText("FOOT"))));

        String html = this.render(uiService);

        assertTrue(html.contains("<div class=\"card-header\">HEAD</div>"), () -> "expected the header in: " + html);
        assertTrue(html.contains("<div class=\"card-footer\">FOOT</div>"), () -> "expected the footer in: " + html);
        assertTrue(html.indexOf("HEAD") < html.indexOf("Body") && html.indexOf("Body") < html.indexOf("FOOT"),
                   () -> "expected header, body and footer in this order in: " + html);
    }

    /**
     * Card.tsx lines 34-39: the image is a {@code card-img-top} at the top of the body, from {@code /images/<image>}, with the name as alternative text.
     */
    @Test
    public void testImageRendersAsCardImgTopAtTheStartOfTheBody() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withImage(image -> image.withName("Alt")
                                                                                                                       .withImage("pic.png"))
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.contains("<img class=\"card-img-top\" src=\"/images/pic.png\" alt=\"Alt\" width=\"100%\" />"), () -> "expected the card image in: " + html);
        assertTrue(html.indexOf("<img") < html.indexOf("Body"), () -> "expected the image before the body content in: " + html);
    }

    /**
     * Card.tsx line 63: the adjustment adds {@code width-max-content} to the inner body that holds the content.
     */
    @Test
    public void testAdjustmentAddsWidthMaxContentToTheInnerBody() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withAdjustment(true)
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.contains("<div class=\"card-inner-body width-max-content\">Body</div>"), () -> "expected the adjusted inner body in: " + html);
    }

    /**
     * Card.tsx line 54: full height adds Bootstrap's {@code h-100} to the card itself.
     */
    @Test
    public void testFullHeightAddsH100ToTheCard() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withFullHeight(true)
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.startsWith("<div class=\"card h-100\">"), () -> "expected the card with h-100 in: " + html);
    }

    /**
     * Card.tsx lines 55-56: an aria label promotes the card to a landmark region with that accessible name.
     */
    @Test
    public void testAriaLabelPromotesTheCardToARegion() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withAriaLabel("Panel")
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.startsWith("<div className=\"card\" role=\"region\" aria-label=\"Panel\">"), () -> "expected the region card in: " + html);
    }

    /**
     * Every optional field at once, to pin the order the elements are emitted in (Card.tsx lines 54-68).
     */
    @Test
    public void testAllOptionalFieldsTogetherRenderInTheOrderOfTheClientComponent() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withLinkLocator("loc")
                                                                                              .withTitle("Featured")
                                                                                              .withSubTitle("Sub")
                                                                                              .withAdjustment(true)
                                                                                              .withFullHeight(true)
                                                                                              .withAriaLabel("Panel")
                                                                                              .withHeader(factory.newText()
                                                                                                                 .addText("HEAD"))
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))
                                                                                              .withFooter(factory.newText()
                                                                                                                 .addText("FOOT"))));

        assertEquals("<div class=\"card h-100\" role=\"region\" aria-label=\"Panel\">\n" //
                     + "\t<div class=\"card-header\">HEAD</div>\n" //
                     + "\t<h5 id=\"loc\" className=\"card-header\"></h5>\n" //
                     + "\t<div className=\"card-body\">\n" //
                     + "\t\t<div class=\"card-title h5\">Featured</div>\n" //
                     + "\t\t<h6 class=\"card-subtitle mb-2 text-muted\">Sub</h6>\n" //
                     + "\t\t<div class=\"card-inner-body width-max-content\">Body</div>\n" //
                     + "\t</div>\n" //
                     + "\t<div class=\"card-footer\">FOOT</div>\n" //
                     + "</div>", this.render(uiService));
    }

    /**
     * Every text or attribute value the card writes is HTML-escaped: the sub title, the aria label, the image name and the image file name. A value with
     * {@code <}, {@code "} and {@code &} must not break out of its element or attribute.
     */
    @Test
    public void testHostileValuesAreEscapedInTextAndInAttributes() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();
        String hostile = "<b>\"x\" & y";
        String escaped = "&lt;b&gt;&quot;x&quot; &amp; y";

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withSubTitle(hostile)
                                                                                              .withAriaLabel(hostile)
                                                                                              .withImage(image -> image.withName(hostile)
                                                                                                                       .withImage("a\"b<c&d.png"))
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.contains("<h6 class=\"card-subtitle mb-2 text-muted\">" + escaped + "</h6>"), () -> "expected the escaped sub title in: " + html);
        assertTrue(html.contains(" aria-label=\"" + escaped + "\""), () -> "expected the escaped aria label in: " + html);
        assertTrue(html.contains(" alt=\"" + escaped + "\""), () -> "expected the escaped image name in: " + html);
        assertTrue(html.contains(" src=\"/images/a&quot;b&lt;c&amp;d.png\""), () -> "expected the escaped image file name in: " + html);
        assertFalse(html.contains("<b>"), () -> "expected no unescaped markup from a value in: " + html);
    }
}
