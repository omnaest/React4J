package org.omnaest.react.internal;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.Mockito;
import org.omnaest.react.domain.ReactUI;
import org.omnaest.react.internal.handler.EventHandlerRegistry;
import org.omnaest.react.internal.nodes.NodeHierarchy;
import org.omnaest.react.internal.nodes.i18n.I18nTextValue;
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
        ReactUI reactUI = this.uiService.getOrCreateDefaultRoot();

        reactUI.addNewComponent(factory -> factory.newParagraph()
                                                  .addText("I love you!"))
               .addNewComponent(factory -> factory.newButton()
                                                  .withName("Click to love me back!"));

        NodeHierarchy nodeHierarchy = this.uiService.resolveNodeHierarchy("/");
        assertNotNull(nodeHierarchy);
    }

}
