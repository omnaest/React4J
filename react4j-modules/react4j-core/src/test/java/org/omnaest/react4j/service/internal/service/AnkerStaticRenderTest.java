package org.omnaest.react4j.service.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omnaest.react4j.component.anker.internal.factory.AnkerFactory;
import org.omnaest.react4j.domain.Text.Emphasis;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.ReactUIServiceImpl;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.internal.CustomUIComponentFactoryManager;
import org.omnaest.react4j.service.internal.service.internal.UIComponentFactoryServiceImpl;
import org.omnaest.utils.MapUtils;

/**
 * plan-286 S1 static-render fidelity test for {@code Anker}: through the REAL {@link NodeHierarchyStaticRenderer} pipeline, an anker with a text and no
 * children renders exactly as it always did, and children are rendered after the text inside the same anchor, without any added whitespace.
 *
 * @see org.omnaest.react4j.component.anker.internal.renderer.AnkerRenderer#manageNodeRenderers(org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry)
 */
public class AnkerStaticRenderTest
{
    private ReactUIServiceImpl newUiService()
    {
        return new ReactUIServiceImpl() {
            {
                this.eventHandlerRegistry = Mockito.mock(EventHandlerRegistry.class);
                this.nodeHierarchyStaticRenderer = new NodeHierarchyStaticRenderer();
                this.uiComponentFactoryService = new UIComponentFactoryServiceImpl() {
                    {
                        // Anker is a custom component, created by its factory: the manager is given exactly that one
                        this.customUIComponentFactoryManager = new CustomUIComponentFactoryManager() {
                            {
                                this.uiComponentFactories = List.of(new AnkerFactory());
                            }
                        };
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

    /**
     * Pinned BEFORE {@code Anker} gained children (plan-261 policy criterion 2a): the complete static render of a text-only anker, escaped as ever.
     */
    @Test
    public void testAnkerWithTextAndWithoutChildrenRendersAsItAlwaysDid() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newAnker()
                                                                                              .withLink("https://example.org/a?x=1&y=2")
                                                                                              .withText("A & B <x>")));

        assertEquals("<a href=\"https://example.org/a?x=1&y=2\" target=\"_blank\" rel=\"noopener noreferrer\">A &amp; B &lt;x&gt;</a>\n",
                     this.render(uiService));
    }

    /**
     * The same anker opening on the same page keeps its target.
     */
    @Test
    public void testAnkerOpeningOnTheSamePageRendersWithTheSelfTarget() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newAnker()
                                                                                              .withLocator("section")
                                                                                              .withText("Jump")));

        assertEquals("<a href=\"#section\" target=\"_self\" rel=\"noopener noreferrer\">Jump</a>\n", this.render(uiService));
    }

    /**
     * Children are rendered through the same pipeline as everywhere else (their text is escaped there), after the text, inside the same anchor and without
     * any added whitespace.
     */
    @Test
    public void testChildrenRenderAfterTheTextInsideTheSameAnchorWithoutAddedWhitespace() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newAnker()
                                                                                              .withLink("https://example.org/c")
                                                                                              .withText("lead ")
                                                                                              .addComponent(factory.newText()
                                                                                                                   .addText("b <x>")
                                                                                                                   .withEmphasis(Emphasis.BOLD))
                                                                                              .addComponents(List.of(factory.newText()
                                                                                                                            .addNonTranslatedText("c")))));

        assertEquals("<a href=\"https://example.org/c\" target=\"_blank\" rel=\"noopener noreferrer\">lead <strong>b &lt;x&gt;</strong>c</a>\n",
                     this.render(uiService));
    }

    /**
     * A label that is nothing but children (a markdown link label of emphasis only) renders those and nothing else.
     */
    @Test
    public void testAnAnkerWithoutATextRendersItsChildrenAsTheWholeLabel() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newAnker()
                                                                                              .withLink("https://example.org/c")
                                                                                              .addComponent(factory.newText()
                                                                                                                   .addText("only")
                                                                                                                   .withEmphasis(Emphasis.ITALIC))));

        assertEquals("<a href=\"https://example.org/c\" target=\"_blank\" rel=\"noopener noreferrer\"><em>only</em></a>\n", this.render(uiService));
    }

    private String render(ReactUIServiceImpl uiService) throws Exception
    {
        return uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML)
                        .replace("\r\n", "\n");
    }
}
