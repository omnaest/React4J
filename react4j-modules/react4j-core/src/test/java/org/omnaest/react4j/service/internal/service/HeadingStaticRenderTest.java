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
package org.omnaest.react4j.service.internal.service;

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
 * Goal-4 static-render fidelity test (plan-74 G4-Heading): asserts that a {@code Heading}'s registered HTML
 * {@code NodeRenderer} produces semantic {@code <hN>...</hN>} static markup through the REAL
 * {@link NodeHierarchyStaticRenderer} pipeline - real {@link ReactUIServiceImpl}, real node hierarchy, real registry
 * collection (mirrors {@code ReactUIServiceImplTest}'s wiring; no mocks of the rendering pipeline itself).
 *
 * @see org.omnaest.react4j.service.internal.component.HeadingImpl#asRenderer()
 */
public class HeadingStaticRenderTest
{
    private ReactUIServiceImpl newUiService()
    {
        return new ReactUIServiceImpl() {
            {
                this.eventHandlerRegistry = Mockito.mock(EventHandlerRegistry.class);
                this.nodeHierarchyStaticRenderer = new NodeHierarchyStaticRenderer();
                this.uiComponentFactoryService = new UIComponentFactoryServiceImpl() {
                    {
                        // keyed by DEFAULT_LOCALE_KEY (not a language tag) - this is the key
                        // NodeHierarchyRenderingProcessorImpl actually looks up when rendering static HTML
                        this.textResolver = (text, location) -> new I18nTextValue(MapUtils.builder()
                                                                                          .put(LocalizedTextResolverService.DEFAULT_LOCALE_KEY,
                                                                                               text.getDefaultText())
                                                                                          .build());
                    }
                };
            }
        };
    }

    @Test
    public void testHeadingRendersSemanticHeadingTagWithResolvedText() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newHeading()
                                                                                              .withText("Hi")
                                                                                              .withLevel(3)));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertFalse(html.isEmpty());
        assertTrue(html.contains("<h3>Hi</h3>"), () -> "expected a semantic <h3> heading in: " + html);
    }

    @Test
    public void testHeadingLevelChangesTheEmittedTag() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newHeading()
                                                                                              .withText("Section")
                                                                                              .withLevel(2)));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertTrue(html.contains("<h2>Section</h2>"), () -> "expected a semantic <h2> heading in: " + html);
        assertFalse(html.contains("<h3>"), () -> "did not expect an <h3> tag in: " + html);
    }
}
