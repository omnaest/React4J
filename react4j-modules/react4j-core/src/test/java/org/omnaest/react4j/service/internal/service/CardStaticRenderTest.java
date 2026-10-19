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
     * The complete output of a card with content only, and with it the DELIBERATE CONTRACT CHANGE of plan-320 S16 (user decision 6 of 2026-10-19, kanban
     * 5f4d3f62): this test replaces the S13 pin {@code testCardWithContentOnlyRendersAsItAlwaysDid}, which held the old default (an {@code h5.card-header}
     * on EVERY card, carrying the locator id even when it was empty). The static default now follows {@code Card.tsx} (the reference, unchanged): no
     * header bar unless a header is set, no {@code id} attribute without a locator, the title (if any) as {@code h4.card-title} in the body.
     * The root, the body and the inner body carry their {@code class} attributes as {@code Card.tsx} does (plan-320 S19, kanban 45da782b and 21da417b):
     * {@code card}, {@code card-body} and {@code card-inner-body}, where the template used to write the JSX name {@code className=} (inert in HTML) on the
     * root and body and no class on the inner body unless adjusted.
     */
    @Test
    public void testCardWithContentOnlyRendersNoHeaderBarAndNoIdAsCardTsxDoes() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        assertEquals("<div class=\"card\">\n\t<div class=\"card-body\">\n\t\t<div class=\"card-inner-body\">Body</div>\n\t</div>\n</div>", this.render(uiService));
    }

    /**
     * Card.tsx line 61: the title (what {@code withTitle} sets on a card WITH an image) is an {@code h4.card-title} in the body, after the image and before
     * the content; there is no header bar at all unless a header is set.
     */
    @Test
    public void testTitleOfAnImageCardRendersAsH4InTheBodyAfterTheImage() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withImage(image -> image.withName("Alt")
                                                                                                                       .withImage("pic.png"))
                                                                                              .withTitle("T")
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.contains("<h4 class=\"card-title\">T</h4>"), () -> "expected the title as h4.card-title in: " + html);
        assertTrue(html.indexOf("<img") < html.indexOf("<h4") && html.indexOf("<h4") < html.indexOf("Body"),
                   () -> "expected the image, then the title, then the body content in: " + html);
        assertFalse(html.contains("card-header"), () -> "expected no header bar without a header in: " + html);
        assertFalse(html.contains("<h5"), () -> "expected no h5 element in: " + html);
    }

    /**
     * A title that renders empty emits no {@code h4} (the featured title precedent); the card is then the same as one without a title.
     */
    @Test
    public void testAnEmptyTitleOfAnImageCardRendersNoH4() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withImage(image -> image.withName("Alt")
                                                                                                                       .withImage("pic.png"))
                                                                                              .withTitle("")
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertFalse(html.contains("<h4"), () -> "expected no h4 for an empty title in: " + html);
        assertTrue(html.contains("<img"), () -> "expected the image to be rendered in: " + html);
    }

    /**
     * Card.tsx line 54: the locator is the {@code id} of the card ROOT ({@code BSCard id=...}), after the class attribute and before the region attributes,
     * and it is the only {@code id} in the output.
     */
    @Test
    public void testLocatorIsTheIdOfTheCardRoot() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withLinkLocator("loc")
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.startsWith("<div class=\"card\" id=\"loc\">"), () -> "expected the locator as id of the root div in: " + html);
        assertEquals(1, html.split("id=", -1).length - 1, () -> "expected exactly one id attribute in: " + html);
    }

    /**
     * Without a locator there is no {@code id} attribute anywhere (the old template wrote {@code id=""} on the header bar of every card); an EMPTY locator
     * is no locator either, because an empty id is invalid HTML.
     */
    @Test
    public void testNoLocatorOrAnEmptyLocatorRendersNoIdAttribute() throws Exception
    {
        ReactUIServiceImpl unsetLocator = this.newUiService();
        unsetLocator.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                                 .withContent(factory.newText()
                                                                                                                     .addText("Body"))));
        ReactUIServiceImpl emptyLocator = this.newUiService();
        emptyLocator.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                                 .withLinkLocator("")
                                                                                                 .withContent(factory.newText()
                                                                                                                     .addText("Body"))));

        String withoutLocator = this.render(unsetLocator);
        String withEmptyLocator = this.render(emptyLocator);

        assertFalse(withoutLocator.contains("id="), () -> "expected no id attribute without a locator in: " + withoutLocator);
        assertFalse(withEmptyLocator.contains("id="), () -> "expected no id attribute for an empty locator in: " + withEmptyLocator);
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
     * Card.tsx line 54: full height adds Bootstrap's {@code h-100} to the card itself, and the body keeps its {@code card-body} class.
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

        assertEquals("<div class=\"card h-100\">\n\t<div class=\"card-body\">\n\t\t<div class=\"card-inner-body\">Body</div>\n\t</div>\n</div>", html);
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

        assertTrue(html.startsWith("<div class=\"card\" role=\"region\" aria-label=\"Panel\">"), () -> "expected the region card in: " + html);
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

        assertEquals("<div class=\"card h-100\" id=\"loc\" role=\"region\" aria-label=\"Panel\">\n" //
                     + "\t<div class=\"card-header\">HEAD</div>\n" //
                     + "\t<div class=\"card-body\">\n" //
                     + "\t\t<div class=\"card-title h5\">Featured</div>\n" //
                     + "\t\t<h6 class=\"card-subtitle mb-2 text-muted\">Sub</h6>\n" //
                     + "\t\t<div class=\"card-inner-body width-max-content\">Body</div>\n" //
                     + "\t</div>\n" //
                     + "\t<div class=\"card-footer\">FOOT</div>\n" //
                     + "</div>", this.render(uiService));
    }

    /**
     * An image AND a title (plus header, sub title, locator and footer) at once, to pin the order image, title, sub title (Card.tsx lines 59-62), the title
     * as {@code h4.card-title}, the locator on the root and the header as the only header bar.
     */
    @Test
    public void testImageTitleSubTitleHeaderLocatorAndFooterTogetherRenderInTheOrderOfTheClientComponent() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                              .withLinkLocator("loc")
                                                                                              .withImage(image -> image.withName("Alt")
                                                                                                                       .withImage("pic.png"))
                                                                                              .withTitle("T")
                                                                                              .withSubTitle("Sub")
                                                                                              .withHeader(factory.newText()
                                                                                                                 .addText("HEAD"))
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))
                                                                                              .withFooter(factory.newText()
                                                                                                                 .addText("FOOT"))));

        assertEquals("<div class=\"card\" id=\"loc\">\n" //
                     + "\t<div class=\"card-header\">HEAD</div>\n" //
                     + "\t<div class=\"card-body\">\n" //
                     + "\t\t<img class=\"card-img-top\" src=\"/images/pic.png\" alt=\"Alt\" width=\"100%\" />\n" //
                     + "\t\t<h4 class=\"card-title\">T</h4>\n" //
                     + "\t\t<h6 class=\"card-subtitle mb-2 text-muted\">Sub</h6>\n" //
                     + "\t\t<div class=\"card-inner-body\">Body</div>\n" //
                     + "\t</div>\n" //
                     + "\t<div class=\"card-footer\">FOOT</div>\n" //
                     + "</div>", this.render(uiService));
    }

    /**
     * Every text or attribute value the card writes is HTML-escaped: the title, the sub title, the aria label, the locator, the image name and the image file
     * name. A value with {@code <}, {@code "} and {@code &} must not break out of its element or attribute.
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
                                                                                              .withLinkLocator(hostile)
                                                                                              .withImage(image -> image.withName(hostile)
                                                                                                                       .withImage("a\"b<c&d.png"))
                                                                                              .withTitle(hostile)
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Body"))));

        String html = this.render(uiService);

        assertTrue(html.contains("<h4 class=\"card-title\">" + escaped + "</h4>"), () -> "expected the escaped title in: " + html);
        assertTrue(html.contains(" id=\"" + escaped + "\""), () -> "expected the escaped locator in: " + html);
        assertTrue(html.contains("<h6 class=\"card-subtitle mb-2 text-muted\">" + escaped + "</h6>"), () -> "expected the escaped sub title in: " + html);
        assertTrue(html.contains(" aria-label=\"" + escaped + "\""), () -> "expected the escaped aria label in: " + html);
        assertTrue(html.contains(" alt=\"" + escaped + "\""), () -> "expected the escaped image name in: " + html);
        assertTrue(html.contains(" src=\"/images/a&quot;b&lt;c&amp;d.png\""), () -> "expected the escaped image file name in: " + html);
        assertFalse(html.contains("<b>"), () -> "expected no unescaped markup from a value in: " + html);
    }

    /**
     * plan-320 S19 (kanban 45da782b, 21da417b): {@code className} is a JSX attribute name, not an HTML one, and an HTML parser keeps it as an inert attribute
     * that styles nothing. No static Card output may contain it in any spelling, whichever of the class branches is taken: the default card, a full-height
     * one, an adjusted one and one with every optional field set.
     */
    @Test
    public void testNoRenderedCardContainsClassNameInAnySpelling() throws Exception
    {
        ReactUIServiceImpl defaultCard = this.newUiService();
        defaultCard.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                                .withContent(factory.newText()
                                                                                                                    .addText("Body"))));
        ReactUIServiceImpl fullHeightCard = this.newUiService();
        fullHeightCard.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                                   .withFullHeight(true)
                                                                                                   .withContent(factory.newText()
                                                                                                                       .addText("Body"))));
        ReactUIServiceImpl adjustedCard = this.newUiService();
        adjustedCard.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                                 .withAdjustment(true)
                                                                                                 .withContent(factory.newText()
                                                                                                                     .addText("Body"))));
        ReactUIServiceImpl allFieldsCard = this.newUiService();
        allFieldsCard.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newCard()
                                                                                                  .withLinkLocator("loc")
                                                                                                  .withImage(image -> image.withName("Alt")
                                                                                                                           .withImage("pic.png"))
                                                                                                  .withTitle("T")
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

        for (String html : new String[] {this.render(defaultCard), this.render(fullHeightCard), this.render(adjustedCard), this.render(allFieldsCard)})
        {
            assertFalse(html.toLowerCase()
                            .contains("classname"),
                        () -> "expected no className attribute in any spelling in: " + html);
            assertTrue(html.startsWith("<div class=\"card") && html.contains("<div class=\"card-body\">") && html.contains("<div class=\"card-inner-body"),
                       () -> "expected the root, body and inner body class attributes in: " + html);
        }
    }
}
