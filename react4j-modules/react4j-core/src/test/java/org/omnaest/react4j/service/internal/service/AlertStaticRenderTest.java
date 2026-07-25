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
import org.omnaest.react4j.domain.Alert.Style;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.ReactUIServiceImpl;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.internal.UIComponentFactoryServiceImpl;
import org.omnaest.utils.MapUtils;

/**
 * Goal-4 static-render fidelity test (plan-74 F2 mechanical burn-down): asserts that an {@code Alert}'s registered
 * HTML {@code NodeRenderer} emits a semantic Bootstrap alert {@code <div role="alert">} carrying its configured style
 * and, when dismissible, a dismiss control, through the REAL {@link NodeHierarchyStaticRenderer} pipeline.
 *
 * @see org.omnaest.react4j.service.internal.component.AlertImpl#asRenderer()
 */
public class AlertStaticRenderTest
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
    public void testDismissibleAlertRendersRoleAndDismissButton() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newAlert()
                                                                                              .withStyle(Style.WARNING)
                                                                                              .withDismissible(true)
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("Careful"))));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertFalse(html.isEmpty());
        assertTrue(html.contains("role=\"alert\""), () -> "expected the alert ARIA role in: " + html);
        assertTrue(html.contains("alert-warning"), () -> "expected the configured style class in: " + html);
        assertTrue(html.contains("alert-dismissible"), () -> "expected the dismissible modifier class in: " + html);
        assertTrue(html.contains("btn-close"), () -> "expected a dismiss control in: " + html);
        assertTrue(html.contains("Careful"), () -> "expected the composed content in: " + html);
    }

    @Test
    public void testNonDismissibleAlertOmitsDismissButton() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newAlert()
                                                                                              .withStyle(Style.INFO)
                                                                                              .withContent(factory.newText()
                                                                                                                  .addText("FYI"))));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertFalse(html.contains("btn-close"), () -> "did not expect a dismiss control in: " + html);
        assertFalse(html.contains("alert-dismissible"), () -> "did not expect the dismissible modifier class in: " + html);
    }
}
