package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.Value;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.ParentLocationAndComponent;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;

/**
 * @see RerenderingContainerImpl
 * @author omnaest
 */
public class RerenderingContainerImplTest
{
    /**
     * {@link RerenderingContainerImpl#withContent(UIComponentFactoryFunction)} must stay lazy like its sibling
     * content-setter overloads: the factory function has to be re-invoked on every render, not materialized once at
     * configuration time.
     */
    @Test
    public void testWithContentFactoryFunctionIsReinvokedOnEachRender()
    {
        ComponentContext context = mock(ComponentContext.class);
        org.mockito.Mockito.when(context.getUiComponentFactory())
                           .thenReturn(mock(UIComponentFactory.class));

        RerenderingContainerImpl container = new RerenderingContainerImpl(context);

        AtomicInteger invocationCount = new AtomicInteger();
        UIComponent<?> renderedComponent = mock(UIComponent.class);
        UIComponentFactoryFunction factoryFunction = factory ->
        {
            invocationCount.incrementAndGet();
            return renderedComponent;
        };

        container.withContent(factoryFunction);

        UIComponentRenderer renderer = container.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        assertEquals(0, invocationCount.get());

        renderer.render(renderingProcessor, location, Optional.empty());
        assertEquals(1, invocationCount.get());

        renderer.render(renderingProcessor, location, Optional.empty());
        assertEquals(2, invocationCount.get());
    }

    /**
     * plan-77 Slice 1 / Cliff F2: the {@code Data}-aware 2-arg {@code getSubComponents} overload must apply the
     * REAL submitted {@link Data} (not the hardcoded empty {@link Data} the 1-arg overload always uses) so a
     * {@code withDataDrivenContent} subtree's REVEALED subcomponent is discoverable by the event-handler
     * registration walk (previously only the render() traversal saw it).
     */
    @Test
    public void testDataAwareGetSubComponentsEnumeratesTheRevealedSubcomponentUnderTheSubmittedDataOnly()
    {
        ComponentContext context = mock(ComponentContext.class);
        org.mockito.Mockito.when(context.getUiComponentFactory())
                           .thenReturn(mock(UIComponentFactory.class));

        RerenderingContainerImpl container = new RerenderingContainerImpl(context);

        UIComponent<?> revealedComponent = mock(UIComponent.class);
        UIComponent<?> defaultComponent = mock(UIComponent.class);
        String revealFieldKey = "reveal";
        container.withDataDrivenContent(data -> data.getFieldValue(revealFieldKey)
                                                    .map(Value::asBoolean)
                                                    .orElse(false) ? revealedComponent : defaultComponent);

        UIComponentRenderer renderer = container.asRenderer();
        Location location = mock(Location.class);
        Data revealingData = Data.newInstance()
                                 .setFieldValue(revealFieldKey, true);

        List<UIComponent<?>> underRevealingData = renderer.getSubComponents(location, Optional.of(revealingData))
                                                          .map(ParentLocationAndComponent::getComponent)
                                                          .collect(Collectors.toList());
        assertEquals(1, underRevealingData.size());
        assertSame(revealedComponent, underRevealingData.get(0), "getSubComponents(location, data) must apply the REAL submitted Data");

        List<UIComponent<?>> underEmptyOptionalData = renderer.getSubComponents(location, Optional.empty())
                                                              .map(ParentLocationAndComponent::getComponent)
                                                              .collect(Collectors.toList());
        assertSame(defaultComponent, underEmptyOptionalData.get(0), "Optional.empty() must behave like empty Data, never revealing the control");

        List<UIComponent<?>> under1ArgOverload = renderer.getSubComponents(location)
                                                         .map(ParentLocationAndComponent::getComponent)
                                                         .collect(Collectors.toList());
        assertSame(defaultComponent, under1ArgOverload.get(0), "the pre-existing 1-arg overload must stay unaffected - always empty Data");

        assertTrue(underRevealingData.get(0) != underEmptyOptionalData.get(0), "revealed content must differ from the empty-Data default");
    }
}
