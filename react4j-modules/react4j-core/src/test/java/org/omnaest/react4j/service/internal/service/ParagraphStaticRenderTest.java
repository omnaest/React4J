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
 * plan-286 S1 static-render fidelity test for {@code Paragraph}: through the REAL {@link NodeHierarchyStaticRenderer} pipeline, a paragraph that is not bold
 * renders exactly as it always did, and a bold one renders the same body inside a {@code <p class="fw-bold">} (the static template used to ignore the flag
 * while the client class was a Bootstrap 4 one that no served sheet defines).
 *
 * @see org.omnaest.react4j.service.internal.component.ParagraphImpl#asRenderer()
 */
public class ParagraphStaticRenderTest
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
     * Pinned BEFORE the static template honoured the bold flag (plan-261 policy criterion 2a): the complete static render of a paragraph that is not bold.
     */
    @Test
    public void testParagraphThatIsNotBoldRendersAsItAlwaysDid() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newParagraph()
                                                                                              .addText("Hello")
                                                                                              .addText("A & B <x>")));

        assertEquals("<div><p>\n" + "\t <span><div>Hello</div></span>  <span><div>A &amp; B &lt;x&gt;</div></span> \n" + "</p>\n" + "</div>", this.render(uiService));
    }

    /**
     * {@code withBoldStyle(false)} is the default state, so the render is the pinned one.
     */
    @Test
    public void testBoldStyleOfFalseRendersAsTheParagraphThatIsNotBold() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newParagraph()
                                                                                              .addText("Hello")
                                                                                              .addText("A & B <x>")
                                                                                              .withBoldStyle()
                                                                                              .withBoldStyle(false)));

        assertEquals("<div><p>\n" + "\t <span><div>Hello</div></span>  <span><div>A &amp; B &lt;x&gt;</div></span> \n" + "</p>\n" + "</div>", this.render(uiService));
    }

    /**
     * A bold paragraph renders the same body inside a paragraph element carrying the Bootstrap 5 bold class, which every served stylesheet defines.
     */
    @Test
    public void testBoldParagraphRendersTheSameBodyInsideAParagraphWithTheBoldClass() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newParagraph()
                                                                                              .addText("Hello")
                                                                                              .addText("A & B <x>")
                                                                                              .withBoldStyle()));

        assertEquals("<div><p class=\"fw-bold\">\n" + "\t <span><div>Hello</div></span>  <span><div>A &amp; B &lt;x&gt;</div></span> \n" + "</p>\n" + "</div>", this.render(uiService));
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
