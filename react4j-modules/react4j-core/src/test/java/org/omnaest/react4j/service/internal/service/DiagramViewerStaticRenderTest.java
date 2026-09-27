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
 * Seam test (plan-229 S1 AC-S1-1/AC-S1-2/AC-S1-3): asserts that a {@code DiagramViewer}'s registered HTML
 * {@code NodeRenderer} emits the configured SVG and typed sizing, through the REAL
 * {@link UIComponentFactoryServiceImpl}/{@code newDiagramViewer()} factory wiring and the REAL
 * {@link NodeHierarchyStaticRenderer} pipeline - proving the factory wiring is real, not merely the node's own
 * unit-level rendering (that is {@code DiagramViewerImplTest}'s job).
 *
 * @see org.omnaest.react4j.service.internal.component.DiagramViewerImpl#asRenderer()
 */
public class DiagramViewerStaticRenderTest
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
    public void testSvgAndSizingAreRenderedThroughTheRealFactory() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newDiagramViewer()
                                                                                              .withSvg("<svg viewBox=\"0 0 10 10\"><circle r=\"3\"/></svg>")
                                                                                              .withMaxHeight("450px")
                                                                                              .withWidth("90%")));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertFalse(html.isEmpty());
        assertTrue(html.contains("<circle r=\"3\"/>"), () -> "expected the composed svg content in: " + html);
        assertTrue(html.contains("max-height:450px"), () -> "expected the max-height sizing in: " + html);
        assertTrue(html.contains("width:90%"), () -> "expected the width sizing in: " + html);
    }

    /**
     * plan-261 S1 AC-S1-4 (set half): {@code withHeight} must reach the {@code NodeRenderType.HTML} renderer
     * too, not only {@code DiagramViewerImpl}'s node-level render() path (which {@code DiagramViewerImplTest}
     * already covers).
     */
    @Test
    public void testHeightIsRenderedInStaticHtmlWhenSet() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newDiagramViewer()
                                                                                              .withSvg("<svg viewBox=\"0 0 10 10\"><circle r=\"3\"/></svg>")
                                                                                              .withMaxHeight("450px")
                                                                                              .withWidth("90%")
                                                                                              .withHeight("600px")));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertTrue(html.contains("<div style=\"width:90%;max-height:450px;height:600px;overflow:hidden;\">"),
                   () -> "expected the height sizing in the exact style attribute of: " + html);
    }

    /**
     * plan-261 S1 AC-S1-4 (unset half, the trap): the emitted style already contains {@code max-height:},
     * itself containing the substring {@code height:} - so "does not contain height:" is unsatisfiable and
     * would make this criterion impossible rather than merely weak. Assert the WHOLE {@code style} attribute
     * value against an exact literal instead, so a spurious {@code height:} declaration cannot slip in
     * unnoticed while a naive substring check stays green.
     */
    @Test
    public void testHeightAbsentLeavesStaticHtmlByteForByteUnchanged() throws Exception
    {
        ReactUIServiceImpl uiService = this.newUiService();

        uiService.getOrCreateDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newDiagramViewer()
                                                                                              .withSvg("<svg viewBox=\"0 0 10 10\"><circle r=\"3\"/></svg>")
                                                                                              .withMaxHeight("450px")
                                                                                              .withWidth("90%")));

        String html = uiService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML);

        assertTrue(html.contains("<div style=\"width:90%;max-height:450px;overflow:hidden;\">"),
                   () -> "expected today's exact style attribute, unchanged, in: " + html);
    }
}
