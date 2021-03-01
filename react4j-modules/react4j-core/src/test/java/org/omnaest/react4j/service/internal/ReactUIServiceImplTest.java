/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
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
