package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.ParentLocationAndComponent;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Regression test for plan-30: {@link CompositeImpl} must disambiguate same-type siblings via a positional
 * ("component" + index) intermediate {@link Location} segment (shared {@link ChildLocationSupport}), applied
 * identically by both {@link UIComponentRenderer#render(RenderingProcessor, Location, Optional)} and
 * {@link UIComponentRenderer#getSubComponents(Location)} - the render/registration agreement whose violation caused
 * plan-29's Bug 1 regression (clicks resolving to {@code null}).
 *
 * @see CompositeImpl
 * @see ChildLocationSupport
 * @author omnaest
 */
public class CompositeImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    private List<List<String>> captureRenderedChildLocations(CompositeImpl composite, Location rootLocation)
    {
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        composite.asRenderer()
                 .render(renderingProcessor, rootLocation, Optional.empty());

        ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
        verify(renderingProcessor, times(2)).process(any(UIComponent.class), locationCaptor.capture());

        return locationCaptor.getAllValues()
                             .stream()
                             .map(Location::get)
                             .collect(Collectors.toList());
    }

    @Test
    public void testTwoSameTypeChildrenGetDistinctLocationsOnRender()
    {
        ComponentContext context = this.newContext();
        ButtonImpl child0 = new ButtonImpl(context);
        ButtonImpl child1 = new ButtonImpl(context);
        // both are plain Buttons -> identical base id; only positional disambiguation can tell them apart
        assertEquals(child0.getId(), child1.getId());

        CompositeImpl composite = new CompositeImpl(context, new ArrayList<>(Arrays.asList(child0, child1)));
        Location rootLocation = Location.of("root");

        List<List<String>> childLocations = this.captureRenderedChildLocations(composite, rootLocation);

        assertNotEquals(childLocations.get(0), childLocations.get(1));
        // CompositeImpl.render() passes the indexed PARENT location into renderingProcessor.process(); the
        // child's own id ("button") is appended later by the real RenderingProcessor, which this mock bypasses.
        assertEquals(Arrays.asList("root", "component0"), childLocations.get(0));
        assertEquals(Arrays.asList("root", "component1"), childLocations.get(1));
    }

    @Test
    public void testChildLocationsStableAcrossTwoSeparateRenderPasses()
    {
        ComponentContext context = this.newContext();
        ButtonImpl child0 = new ButtonImpl(context);
        ButtonImpl child1 = new ButtonImpl(context);

        CompositeImpl composite = new CompositeImpl(context, new ArrayList<>(Arrays.asList(child0, child1)));
        Location rootLocation = Location.of("root");

        List<List<String>> firstPass = this.captureRenderedChildLocations(composite, rootLocation);
        List<List<String>> secondPass = this.captureRenderedChildLocations(composite, rootLocation);

        assertEquals(firstPass, secondPass);
    }

    @Test
    public void testRenderAndGetSubComponentsAgreeOnChildLocation()
    {
        ComponentContext context = this.newContext();
        ButtonImpl child0 = new ButtonImpl(context);
        ButtonImpl child1 = new ButtonImpl(context);

        CompositeImpl composite = new CompositeImpl(context, new ArrayList<>(Arrays.asList(child0, child1)));
        Location rootLocation = Location.of("root");

        List<List<String>> renderLocations = this.captureRenderedChildLocations(composite, rootLocation);

        List<List<String>> subComponentLocations = composite.asRenderer()
                                                            .getSubComponents(rootLocation)
                                                            .map(ParentLocationAndComponent::getParentLocation)
                                                            .map(Location::get)
                                                            .collect(Collectors.toList());

        assertEquals(renderLocations, subComponentLocations);
    }
}
