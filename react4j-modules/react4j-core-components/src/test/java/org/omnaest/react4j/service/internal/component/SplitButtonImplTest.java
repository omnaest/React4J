package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Dropdown.Style;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.nodes.DropdownItemNode;
import org.omnaest.react4j.service.internal.nodes.SplitButtonNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see SplitButtonImpl
 * @author omnaest
 */
public class SplitButtonImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    private RenderingProcessor selfRenderingProcessor()
    {
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        when(renderingProcessor.process(any(UIComponent.class), any(Location.class))).thenAnswer(invocation ->
        {
            UIComponent<?> component = invocation.getArgument(0);
            Location location = invocation.getArgument(1);
            RenderableUIComponent<?> renderable = (RenderableUIComponent<?>) component;
            return renderable.asRenderer()
                             .render(this.selfRenderingProcessor(), location, Optional.empty());
        });
        return renderingProcessor;
    }

    @Test
    public void testDefaultOnClickIsNull()
    {
        ComponentContext context = this.newContext();
        SplitButtonImpl splitButton = new SplitButtonImpl(context);

        UIComponentRenderer renderer = splitButton.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        SplitButtonNode node = (SplitButtonNode) renderer.render(renderingProcessor, location, Optional.empty());

        assertNull(node.getOnClick());
        assertNull(node.getStyle());
        assertTrue(node.getItems()
                       .isEmpty());
    }

    @Test
    public void testStyleSetterAppearsOnNode()
    {
        ComponentContext context = this.newContext();
        SplitButtonImpl splitButton = new SplitButtonImpl(context);
        splitButton.withStyle(Style.DANGER);

        UIComponentRenderer renderer = splitButton.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        SplitButtonNode node = (SplitButtonNode) renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("danger", node.getStyle());
    }

    @Test
    public void testPrimaryOnClickRendersNonNullServerHandlerWhenSet()
    {
        ComponentContext context = this.newContext();
        SplitButtonImpl splitButton = new SplitButtonImpl(context);
        splitButton.onClick(mock(EventHandler.class));

        UIComponentRenderer renderer = splitButton.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root"));

        SplitButtonNode node = (SplitButtonNode) renderer.render(renderingProcessor, location, Optional.empty());

        assertNotNull(node.getOnClick());
        assertTrue(node.getOnClick() instanceof ServerHandler);
    }

    @Test
    public void testManageEventHandlerRegistersOnlyWhenPrimaryOnClickSet()
    {
        ComponentContext context = this.newContext();

        SplitButtonImpl withHandler = new SplitButtonImpl(context);
        EventHandler handler = mock(EventHandler.class);
        withHandler.onClick(handler);

        EventHandlerRegistrationSupport supportWithHandler = mock(EventHandlerRegistrationSupport.class);
        withHandler.asRenderer()
                   .manageEventHandler(supportWithHandler);
        verify(supportWithHandler, times(1)).register(handler);

        SplitButtonImpl withoutHandler = new SplitButtonImpl(context);
        EventHandlerRegistrationSupport supportWithoutHandler = mock(EventHandlerRegistrationSupport.class);
        withoutHandler.asRenderer()
                      .manageEventHandler(supportWithoutHandler);
        verify(supportWithoutHandler, never()).register(any(EventHandler.class));
    }

    @Test
    public void testAddItemAddDividerAddHeaderReuseDropdownItemImpl()
    {
        ComponentContext context = this.newContext();
        SplitButtonImpl splitButton = new SplitButtonImpl(context);

        splitButton.addItem(item -> item.withText("Action"));
        splitButton.addDivider();
        splitButton.addHeader("Section");

        UIComponentRenderer renderer = splitButton.asRenderer();
        RenderingProcessor renderingProcessor = this.selfRenderingProcessor();
        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root"));

        SplitButtonNode node = (SplitButtonNode) renderer.render(renderingProcessor, location, Optional.empty());

        List<DropdownItemNode> items = node.getItems();
        assertEquals(3, items.size());
        assertEquals("LINK", items.get(0)
                                  .getKind());
        assertEquals("DIVIDER", items.get(1)
                                     .getKind());
        assertEquals("HEADER", items.get(2)
                                    .getKind());
    }

    @Test
    public void testFieldsAndItemsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        SplitButtonImpl splitButton = new SplitButtonImpl(context);
        splitButton.withStyle(Style.SUCCESS);
        splitButton.onClick(mock(EventHandler.class));
        splitButton.addItem(item -> item.withActiveState(true));

        SplitButtonImpl templated = (SplitButtonImpl) splitButton.asTemplateProvider()
                                                                 .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = this.selfRenderingProcessor();
        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root"));

        SplitButtonNode node = (SplitButtonNode) renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("success", node.getStyle());
        assertNotNull(node.getOnClick());
        List<DropdownItemNode> items = node.getItems();
        assertEquals(1, items.size());
        assertTrue(items.get(0)
                        .isActive());
    }
}
