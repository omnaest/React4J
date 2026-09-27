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
package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.nodes.ClipboardCopyButtonNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Component-level unit test for {@link ClipboardCopyButtonImpl} - the Java half of the seam S5-AC-R4 requires be
 * proven end to end in a browser (see {@code ClipboardCopyButtonRoundTripIT} in react4j-ui-test). This test proves
 * only what a Java-only test CAN prove: the emitted {@link ClipboardCopyButtonNode} carries exactly the fields the
 * caller supplied, and - S5-AC-R1 - that {@link ClipboardCopyButtonImpl#manageEventHandler} never registers a server
 * handler, because this component is deliberately client-only.
 *
 * @see ClipboardCopyButtonImpl
 */
public class ClipboardCopyButtonImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        LocalizedTextResolverService textResolver = mock(LocalizedTextResolverService.class);
        when(textResolver.apply(org.mockito.ArgumentMatchers.any(I18nText.class), org.mockito.ArgumentMatchers.any(Location.class)))
                                                                                                                                    .thenAnswer(invocation -> new I18nTextValue(Collections.singletonMap("DEFAULT",
                                                                                                                                                                                                         "Copy")));
        when(context.getTextResolver()).thenReturn(textResolver);
        return context;
    }

    private Location newLocation(ClipboardCopyButtonImpl component)
    {
        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root", component.getId()));
        return location;
    }

    @Test
    public void testRenderedNodeCarriesExactlyTextLabelAndClearAfterDuration()
    {
        ComponentContext context = this.newContext();

        ClipboardCopyButtonImpl component = new ClipboardCopyButtonImpl(context);
        component.withText("s3cr3t-value")
                 .withLabel("Copy")
                 .withClearAfterDuration(5, TimeUnit.SECONDS);

        ClipboardCopyButtonNode node = (ClipboardCopyButtonNode) component.asRenderer()
                                                                          .render(mock(RenderingProcessor.class), this.newLocation(component), Optional.empty());

        assertEquals("s3cr3t-value", node.getText());
        assertEquals(5000, node.getClearAfterDurationMillis());
        assertEquals("CLIPBOARDCOPYBUTTON", node.getType());
    }

    @Test
    public void testDefaultClearAfterDurationIsThirtySecondsWhenNeverSet()
    {
        ComponentContext context = this.newContext();

        ClipboardCopyButtonImpl component = new ClipboardCopyButtonImpl(context);
        component.withText("value")
                 .withLabel("Copy");

        ClipboardCopyButtonNode node = (ClipboardCopyButtonNode) component.asRenderer()
                                                                          .render(mock(RenderingProcessor.class), this.newLocation(component), Optional.empty());

        assertEquals(30000, node.getClearAfterDurationMillis());
    }

    /**
     * S5-AC-R1 / the design's frozen property: this is a purely client-side interaction, so rendering never produces
     * a server {@link EventHandler} and {@link ClipboardCopyButtonImpl#manageEventHandler} never registers one - the
     * opposite of {@code ButtonImpl}, which registers an {@code onClick} handler whenever one is set.
     */
    @Test
    public void testManageEventHandlerNeverRegistersAnything()
    {
        ComponentContext context = this.newContext();

        ClipboardCopyButtonImpl component = new ClipboardCopyButtonImpl(context);
        component.withText("value")
                 .withLabel("Copy");

        EventHandlerRegistrationSupport support = mock(EventHandlerRegistrationSupport.class);
        UIComponentRenderer renderer = component.asRenderer();
        renderer.manageEventHandler(support);

        verify(support, never()).register(org.mockito.ArgumentMatchers.any(EventHandler.class));
    }

    @Test
    public void testAsTemplateProviderPreservesTextLabelAndClearAfterDuration()
    {
        ComponentContext context = this.newContext();

        ClipboardCopyButtonImpl component = new ClipboardCopyButtonImpl(context);
        component.withText("templated-value")
                 .withLabel("Copy")
                 .withClearAfterDuration(2, TimeUnit.MINUTES);

        ClipboardCopyButtonImpl templated = (ClipboardCopyButtonImpl) component.asTemplateProvider()
                                                                               .get();

        ClipboardCopyButtonNode node = (ClipboardCopyButtonNode) templated.asRenderer()
                                                                          .render(mock(RenderingProcessor.class), this.newLocation(templated), Optional.empty());

        assertEquals("templated-value", node.getText());
        assertEquals(120000, node.getClearAfterDurationMillis());
    }

    @Test
    public void testTextIsNullWhenNeverSet()
    {
        ComponentContext context = this.newContext();

        ClipboardCopyButtonImpl component = new ClipboardCopyButtonImpl(context);
        component.withLabel("Copy");

        ClipboardCopyButtonNode node = (ClipboardCopyButtonNode) component.asRenderer()
                                                                          .render(mock(RenderingProcessor.class), this.newLocation(component), Optional.empty());

        assertNull(node.getText());
    }
}
