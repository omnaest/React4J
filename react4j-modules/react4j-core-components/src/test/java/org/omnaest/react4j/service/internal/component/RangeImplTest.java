package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.RangeNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link RangeImpl} builder API ({@code withLabel}) and its numeric
 * defaults (min/max/step/disabled) map onto the produced {@link RangeNode}.
 *
 * @see RangeImpl
 * @author omnaest
 */
public class RangeImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsMapToNode()
    {
        ComponentContext context = this.newContext();
        RangeImpl range = new RangeImpl(context);

        Node node = range.asRenderer()
                         .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        RangeNode rangeNode = (RangeNode) node;
        assertNull(rangeNode.getLabel());
        assertEquals("0.0", rangeNode.getMin());
        assertEquals("100.0", rangeNode.getMax());
        assertEquals("1.0", rangeNode.getStep());
        assertFalse(rangeNode.isDisabled());
    }

    @Test
    public void testWithLabelResolvesTranslatableI18nText()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue resolvedLabel = new I18nTextValue(Map.of("DEFAULT", "Volume"));
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedLabel);

        RangeImpl range = new RangeImpl(context);
        range.withLabel("Volume");

        Node node = range.asRenderer()
                         .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertEquals(resolvedLabel, ((RangeNode) node).getLabel());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        RangeImpl range = new RangeImpl(context);
        range.withLabel("Volume");

        RangeImpl templated = (RangeImpl) range.asTemplateProvider()
                                               .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals("0.0", ((RangeNode) node).getMin());
    }
}
