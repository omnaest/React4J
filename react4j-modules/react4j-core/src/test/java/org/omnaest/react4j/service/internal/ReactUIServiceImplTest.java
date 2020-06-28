package org.omnaest.react4j.service.internal;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.Mockito;
import org.omnaest.react4j.service.internal.ReactUIServiceImpl;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.nodes.NodeHierarchy;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.utils.MapUtils;

public class ReactUIServiceImplTest
{
    private ReactUIServiceImpl uiService = new ReactUIServiceImpl()
    {
        {
            this.textResolver = (text, location) -> new I18nTextValue(MapUtils.builder()
                                                                              .put(text.getDefaultLocale()
                                                                                       .asLanguageTag(),
                                                                                   text.getDefaultText())
                                                                              .build());
            this.eventHandlerRegistry = Mockito.mock(EventHandlerRegistry.class);
        }
    };

    @Test
    public void testCreateDefaultRoot() throws Exception
    {
        this.uiService.getOrCreateDefaultRoot(reactUI ->
        {
            reactUI.addNewComponent(factory -> factory.newParagraph()
                                                      .addText("I love you!"))
                   .addNewComponent(factory -> factory.newButton()
                                                      .withName("Click to love me back!"));
        });

        NodeHierarchy nodeHierarchy = this.uiService.resolveNodeHierarchy("/");
        assertNotNull(nodeHierarchy);
    }

}
