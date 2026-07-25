package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Spinner.Style;
import org.omnaest.react4j.domain.Spinner.Type;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.SpinnerNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see SpinnerImpl
 * @author omnaest
 */
public class SpinnerImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testStyleAndTypeDefaults()
    {
        ComponentContext context = this.newContext();
        SpinnerImpl spinner = new SpinnerImpl(context);

        UIComponentRenderer renderer = spinner.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("primary", ((SpinnerNode) node).getStyle());
        assertEquals("border", ((SpinnerNode) node).getSpinnerType());
    }

    @Test
    public void testStyleAndTypeSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        SpinnerImpl spinner = new SpinnerImpl(context);

        spinner.withStyle(Style.SUCCESS);
        spinner.withType(Type.GROW);

        UIComponentRenderer renderer = spinner.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("success", ((SpinnerNode) node).getStyle());
        assertEquals("grow", ((SpinnerNode) node).getSpinnerType());
    }

    @Test
    public void testStyleAndTypeSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        SpinnerImpl spinner = new SpinnerImpl(context);
        spinner.withStyle(Style.INFO);
        spinner.withType(Type.GROW);

        SpinnerImpl templated = (SpinnerImpl) spinner.asTemplateProvider()
                                                     .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("info", ((SpinnerNode) node).getStyle());
        assertEquals("grow", ((SpinnerNode) node).getSpinnerType());
    }

    @Test
    public void testStyleAndTypeOfRoundTrip()
    {
        assertEquals(Style.SUCCESS, Style.of("SUCCESS")
                                         .get());
        assertEquals(Optional.empty(), Style.of("bogus"));
        assertEquals(Type.GROW, Type.of("GROW")
                                    .get());
        assertEquals(Optional.empty(), Type.of("bogus"));
    }
}
