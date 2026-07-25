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
 * Goal-4 static-render fidelity test (plan-74 F2 mechanical burn-down): asserts that a {@code Pagination}'s
 * registered HTML {@code NodeRenderer} emits a semantic {@code <nav><ul class="pagination">} whose items are
 * themselves rendered via a registered {@code PAGINATION_ITEM} {@code NodeRenderer} (child composition), through the
 * REAL {@link NodeHierarchyStaticRenderer} pipeline.
 *
 * @see org.omnaest.react4j.service.internal.component.PaginationImpl#asRenderer()
 */
public class PaginationStaticRenderTest
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
    public void testActiveAndDisabledItemsAreComposedInsideNavAndUl() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newPagination()
                                                                                              .addItem(item -> item.withLabel("1")
                                                                                                                   .withActiveState(true))
                                                                                              .addItem(item -> item.withLabel("2")
                                                                                                                   .withDisabledState(true))));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertFalse(html.isEmpty());
        assertTrue(html.contains("<nav aria-label=\"pagination\">"), () -> "expected the pagination nav landmark in: " + html);
        assertTrue(html.contains("<ul class=\"pagination\">"), () -> "expected the pagination list in: " + html);
        assertTrue(html.contains("page-item active"), () -> "expected the active item class in: " + html);
        assertTrue(html.contains("page-item disabled"), () -> "expected the disabled item class in: " + html);
        assertTrue(html.contains("page-link"), () -> "expected the page-link element in: " + html);
        assertTrue(html.contains(">1<"), () -> "expected the first item's label in: " + html);
        assertTrue(html.contains(">2<"), () -> "expected the second item's label in: " + html);
    }
}
