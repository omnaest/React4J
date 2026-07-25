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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.omnaest.react4j.domain.Heading.Level;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.HeadingNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link HeadingImpl} builder API maps faithfully onto the produced
 * {@link HeadingNode} node hierarchy - heading level, i18n text resolution, and the fact that a {@link org.omnaest.react4j.domain.Heading}
 * is a leaf (no sub components, no event handler wiring). Asserts on the produced node model only, never on a browser
 * or static HTML (see {@code HeadingStaticRenderTest} for the Goal-4 static-render fidelity check).
 *
 * @see HeadingImpl
 * @author omnaest
 */
public class HeadingImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultLevelAndNullTextAreRendered()
    {
        ComponentContext context = this.newContext();
        HeadingImpl heading = new HeadingImpl(context);

        UIComponentRenderer renderer = heading.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals(1, ((HeadingNode) node).getLevel());
        assertNull(((HeadingNode) node).getText());
    }

    @Test
    public void testWithTextResolvesTranslatableI18nTextViaTextResolver()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        I18nTextValue resolvedText = new I18nTextValue(java.util.Map.of("DEFAULT", "Hi"));
        Location location = mock(Location.class);
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedText);

        HeadingImpl heading = new HeadingImpl(context);
        heading.withText("Hi")
               .withLevel(3);

        UIComponentRenderer renderer = heading.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals(3, ((HeadingNode) node).getLevel());
        assertSame(resolvedText, ((HeadingNode) node).getText());

        ArgumentCaptor<I18nText> textCaptor = ArgumentCaptor.forClass(I18nText.class);
        verify(textResolver).apply(textCaptor.capture(), eq(location));
        assertEquals("Hi", textCaptor.getValue()
                                     .getDefaultText());
        assertFalse(textCaptor.getValue()
                              .isNonTranslatable());
    }

    @Test
    public void testWithNonTranslatedTextMarksI18nTextAsNonTranslatable()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);

        HeadingImpl heading = new HeadingImpl(context);
        heading.withNonTranslatedText("Raw Title");

        UIComponentRenderer renderer = heading.asRenderer();
        renderer.render(mock(RenderingProcessor.class), location, Optional.empty());

        ArgumentCaptor<I18nText> textCaptor = ArgumentCaptor.forClass(I18nText.class);
        verify(textResolver).apply(textCaptor.capture(), eq(location));
        assertEquals("Raw Title", textCaptor.getValue()
                                            .getDefaultText());
        assertTrue(textCaptor.getValue()
                             .isNonTranslatable());
    }

    @Test
    public void testWithLevelEnumMapsToOneBasedLevel()
    {
        ComponentContext context = this.newContext();
        HeadingImpl heading = new HeadingImpl(context);

        heading.withLevel(Level.H4);

        Node node = heading.asRenderer()
                           .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(4, ((HeadingNode) node).getLevel());
    }

    @Test
    public void testHeadingIsALeafWithNoSubComponentsAndNoEventHandler()
    {
        ComponentContext context = this.newContext();
        HeadingImpl heading = new HeadingImpl(context);
        heading.withText("Hi");

        UIComponentRenderer renderer = heading.asRenderer();

        assertEquals(0, renderer.getSubComponents(mock(Location.class))
                                .count());

        EventHandlerRegistrationSupport support = mock(EventHandlerRegistrationSupport.class);
        renderer.manageEventHandler(support);
        verify(support, never()).register(any(org.omnaest.react4j.service.internal.handler.domain.EventHandler.class));
        verify(support, never()).registerAsRerenderingNode();
    }

    @Test
    public void testLocationIsDerivedFromComponentId()
    {
        ComponentContext context = this.newContext();
        HeadingImpl heading = new HeadingImpl(context);

        LocationSupport locationSupport = mock(LocationSupport.class);
        Location expectedLocation = mock(Location.class);
        when(locationSupport.createLocation(heading.getId())).thenReturn(expectedLocation);

        Location location = heading.asRenderer()
                                   .getLocation(locationSupport);

        assertSame(expectedLocation, location);
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        I18nTextValue resolvedText = new I18nTextValue(java.util.Map.of("DEFAULT", "Templated"));
        Location location = mock(Location.class);
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedText);

        HeadingImpl heading = new HeadingImpl(context);
        heading.withText("Templated")
               .withLevel(5);

        HeadingImpl templated = (HeadingImpl) heading.asTemplateProvider()
                                                     .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertEquals(5, ((HeadingNode) node).getLevel());
        assertSame(resolvedText, ((HeadingNode) node).getText());
    }
}
