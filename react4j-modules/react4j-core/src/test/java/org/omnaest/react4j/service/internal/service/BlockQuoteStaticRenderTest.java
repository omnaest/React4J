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
 * plan-284 S2 static-render fidelity test for {@code BlockQuote}: through the REAL {@link NodeHierarchyStaticRenderer} pipeline, a quote holding texts and a
 * footer renders exactly as it always did, a quote may additionally hold child components that render after the texts, and the footer element is only
 * emitted when a footer was set (an empty {@code .blockquote-footer} makes Bootstrap print a lone em dash).
 *
 * @see org.omnaest.react4j.service.internal.component.BlockQuoteImpl#asRenderer()
 */
public class BlockQuoteStaticRenderTest
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
     * Pinned BEFORE {@code BlockQuote} gained children (plan-284 policy criterion 2a): the complete static render of a quote with texts and a footer.
     */
    @Test
    public void testQuoteWithTextsAndAFooterRendersAsItAlwaysDid() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newBlockQuote()
                                                                                              .addText("Quote")
                                                                                              .addText("A & B <x>")
                                                                                              .withFooter("Author")));

        assertEquals("<blockquote class=\"blockquote\">\n" + "\t\t<p class=\"mb-01\">Quote</p>\n" + "\t<p class=\"mb-01\">A &amp; B &lt;x&gt;</p>\n"
                     + "\t<footer class=\"blockquote-footer\">\n" + "\t\t<cite>Author</cite>\n" + "\t</footer>\n" + "</blockquote>", this.render(uiService));
    }

    /**
     * Child components render after the texts and before the footer, through the same pipeline as everywhere else (their text is escaped there).
     */
    @Test
    public void testChildComponentsRenderAfterTheTextsAndBeforeTheFooter() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newBlockQuote()
                                                                                              .addText("Quote")
                                                                                              .addComponent(factory.newText()
                                                                                                                   .addText("child <x>"))
                                                                                              .addComponents(java.util.List.of(factory.newText()
                                                                                                                                      .addText("second"),
                                                                                                                               factory.newText()
                                                                                                                                      .addText("third")))
                                                                                              .withFooter("Author")));

        assertEquals("<blockquote class=\"blockquote\">\n" + "\t\t<p class=\"mb-01\">Quote</p>\n" + "\tchild &lt;x&gt;\n" + "\tsecond\n" + "\tthird\n"
                     + "\t<footer class=\"blockquote-footer\">\n" + "\t\t<cite>Author</cite>\n" + "\t</footer>\n" + "</blockquote>", this.render(uiService));
    }

    /**
     * An empty {@code .blockquote-footer} makes Bootstrap print a lone em dash, so without a footer the element is not emitted at all.
     */
    @Test
    public void testNoFooterElementIsEmittedWithoutAFooter() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newBlockQuote()
                                                                                              .addText("Quote")
                                                                                              .addComponent(factory.newText()
                                                                                                                   .addText("child"))));

        String html = this.render(uiService);

        assertEquals("<blockquote class=\"blockquote\">\n" + "\t\t<p class=\"mb-01\">Quote</p>\n" + "\tchild\n" + "</blockquote>", html);
    }

    @Test
    public void testAQuoteHoldingOnlyChildComponentsRendersJustThoseInsideTheBlockquote() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newBlockQuote()
                                                                                              .addComponent(factory.newText()
                                                                                                                   .addText("child"))));

        assertEquals("<blockquote class=\"blockquote\">\n" + "\t\tchild\n" + "</blockquote>", this.render(uiService));
    }

    /**
     * The template is checked in with LF endings and checked out with the platform's, so the pin compares with the endings normalised.
     */
    private String render(ReactUIServiceImpl uiService) throws Exception
    {
        return uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML)
                        .replace("\r\n", "\n");
    }
}
