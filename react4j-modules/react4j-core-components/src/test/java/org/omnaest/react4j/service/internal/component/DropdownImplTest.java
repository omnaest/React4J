package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import org.omnaest.react4j.domain.Dropdown.Drop;
import org.omnaest.react4j.domain.Dropdown.Presentation;
import org.omnaest.react4j.domain.Dropdown.Style;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.nodes.DropdownItemNode;
import org.omnaest.react4j.service.internal.nodes.DropdownNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see DropdownImpl
 * @see DropdownItemImpl
 * @author omnaest
 */
public class DropdownImplTest
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
    public void testDefaultFieldValues()
    {
        ComponentContext context = this.newContext();
        DropdownImpl dropdown = new DropdownImpl(context);

        UIComponentRenderer renderer = dropdown.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        DropdownNode node = (DropdownNode) renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("BUTTON", node.getPresentation());
        assertNull(node.getStyle());
        assertNull(node.getDrop());
        assertTrue(node.getItems()
                       .isEmpty());
    }

    @Test
    public void testBuilderSettersAppearOnNode()
    {
        ComponentContext context = this.newContext();
        DropdownImpl dropdown = new DropdownImpl(context);

        dropdown.withPresentation(Presentation.NAV)
                .withStyle(Style.SUCCESS)
                .withDrop(Drop.END);

        UIComponentRenderer renderer = dropdown.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        DropdownNode node = (DropdownNode) renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("NAV", node.getPresentation());
        assertEquals("success", node.getStyle());
        assertEquals("end", node.getDrop());
    }

    @Test
    public void testItemDividerAndHeaderRenderInOrderWithCorrectKinds()
    {
        ComponentContext context = this.newContext();
        DropdownImpl dropdown = new DropdownImpl(context);

        dropdown.addItem(item -> item.withText("Action"));
        dropdown.addDivider();
        dropdown.addHeader("Section");

        UIComponentRenderer renderer = dropdown.asRenderer();
        RenderingProcessor renderingProcessor = this.selfRenderingProcessor();
        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root"));

        DropdownNode node = (DropdownNode) renderer.render(renderingProcessor, location, Optional.empty());

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
        DropdownImpl dropdown = new DropdownImpl(context);
        dropdown.withPresentation(Presentation.NAV)
                .withStyle(Style.DANGER)
                .withDrop(Drop.UP);
        dropdown.addItem(item -> item.withActiveState(true));
        dropdown.addDivider();

        DropdownImpl templated = (DropdownImpl) dropdown.asTemplateProvider()
                                                        .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = this.selfRenderingProcessor();
        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root"));

        DropdownNode node = (DropdownNode) renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("NAV", node.getPresentation());
        assertEquals("danger", node.getStyle());
        assertEquals("up", node.getDrop());
        List<DropdownItemNode> items = node.getItems();
        assertEquals(2, items.size());
        assertTrue(items.get(0)
                        .isActive());
        assertEquals("DIVIDER", items.get(1)
                                     .getKind());
    }

    @Test
    public void testItemWithOnClickRendersNonNullServerHandlerAndWithoutIsNull()
    {
        ComponentContext context = this.newContext();

        DropdownItemImpl itemWithHandler = new DropdownItemImpl(context, 0, DropdownItemImpl.Kind.LINK);
        itemWithHandler.onClick(mock(EventHandler.class));

        Location location0 = mock(Location.class);
        when(location0.get()).thenReturn(Arrays.asList("root", itemWithHandler.getId()));

        DropdownItemNode node0 = (DropdownItemNode) itemWithHandler.asRenderer()
                                                                   .render(mock(RenderingProcessor.class), location0, Optional.empty());
        assertNotNull(node0.getOnClick());
        assertTrue(node0.getOnClick() instanceof ServerHandler);

        DropdownItemImpl itemWithoutHandler = new DropdownItemImpl(context, 1, DropdownItemImpl.Kind.DIVIDER);
        Location location1 = mock(Location.class);
        when(location1.get()).thenReturn(Arrays.asList("root", itemWithoutHandler.getId()));

        DropdownItemNode node1 = (DropdownItemNode) itemWithoutHandler.asRenderer()
                                                                      .render(mock(RenderingProcessor.class), location1, Optional.empty());
        assertNull(node1.getOnClick());
    }

    @Test
    public void testManageEventHandlerRegistersOnlyWhenOnClickSet()
    {
        ComponentContext context = this.newContext();

        DropdownItemImpl itemWithHandler = new DropdownItemImpl(context, 0, DropdownItemImpl.Kind.LINK);
        EventHandler handler = mock(EventHandler.class);
        itemWithHandler.onClick(handler);

        EventHandlerRegistrationSupport supportWithHandler = mock(EventHandlerRegistrationSupport.class);
        itemWithHandler.asRenderer()
                       .manageEventHandler(supportWithHandler);
        verify(supportWithHandler, times(1)).register(handler);

        DropdownItemImpl itemWithoutHandler = new DropdownItemImpl(context, 1, DropdownItemImpl.Kind.HEADER);
        EventHandlerRegistrationSupport supportWithoutHandler = mock(EventHandlerRegistrationSupport.class);
        itemWithoutHandler.asRenderer()
                          .manageEventHandler(supportWithoutHandler);
        verify(supportWithoutHandler, never()).register(any(EventHandler.class));
    }

    @Test
    public void testTwoItemsWithDistinctHandlersProduceDistinctTargets()
    {
        ComponentContext context = this.newContext();

        DropdownItemImpl item0 = new DropdownItemImpl(context, 0, DropdownItemImpl.Kind.LINK);
        item0.onClick(mock(EventHandler.class));
        DropdownItemImpl item1 = new DropdownItemImpl(context, 1, DropdownItemImpl.Kind.LINK);
        item1.onClick(mock(EventHandler.class));

        assertNotEquals(item0.getId(), item1.getId());

        Location location0 = mock(Location.class);
        when(location0.get()).thenReturn(Arrays.asList("root", item0.getId()));
        Location location1 = mock(Location.class);
        when(location1.get()).thenReturn(Arrays.asList("root", item1.getId()));

        DropdownItemNode node0 = (DropdownItemNode) item0.asRenderer()
                                                         .render(mock(RenderingProcessor.class), location0, Optional.empty());
        DropdownItemNode node1 = (DropdownItemNode) item1.asRenderer()
                                                         .render(mock(RenderingProcessor.class), location1, Optional.empty());

        ServerHandler handler0 = (ServerHandler) node0.getOnClick();
        ServerHandler handler1 = (ServerHandler) node1.getOnClick();

        assertNotEquals(handler0.getTarget(), handler1.getTarget());
    }

    @Test
    public void testEnumOfRoundTrips()
    {
        assertEquals(Style.SUCCESS, Style.of("SUCCESS")
                                         .get());
        assertFalse(Style.of("NOT_A_STYLE")
                         .isPresent());
        assertEquals(Presentation.NAV, Presentation.of("NAV")
                                                   .get());
        assertFalse(Presentation.of("INVALID")
                                .isPresent());
        assertEquals(Drop.END, Drop.of("END")
                                   .get());
        assertFalse(Drop.of("INVALID")
                        .isPresent());
    }
}
