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
 * Goal-4 static-render fidelity test (plan-74 F2 mechanical burn-down): asserts that a {@code Breadcrumb}'s
 * registered HTML {@code NodeRenderer} emits a semantic {@code <nav><ol class="breadcrumb">} with a linked entry and
 * an active (unlinked) entry, through the REAL {@link NodeHierarchyStaticRenderer} pipeline.
 *
 * @see org.omnaest.react4j.service.internal.component.BreadcrumbImpl#asRenderer()
 */
public class BreadcrumbStaticRenderTest
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

    @Test
    public void testLinkedAndActiveEntriesAreRenderedAsNavAndOl() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newBreadcrumb()
                                                                                              .addEntry(entry -> entry.withText("Home")
                                                                                                                      .withLink("/home"))
                                                                                              .addEntry(entry -> entry.withText("Current")
                                                                                                                      .withActiveState(true))));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertFalse(html.isEmpty());
        assertTrue(html.contains("<nav aria-label=\"breadcrumb\">"), () -> "expected the breadcrumb nav landmark in: " + html);
        assertTrue(html.contains("<ol class=\"breadcrumb\">"), () -> "expected the breadcrumb list in: " + html);
        assertTrue(html.contains("<a href=\"/home\">Home</a>"), () -> "expected the linked entry in: " + html);
        assertTrue(html.contains("breadcrumb-item active"), () -> "expected the active entry class in: " + html);
        assertTrue(html.contains("Current"), () -> "expected the active entry's resolved text in: " + html);
    }
}
