package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.ParentLocationAndComponent;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.DraggableNode;
import org.omnaest.react4j.service.internal.nodes.ParagraphNode;

/**
 * @see DraggableImpl
 * @author omnaest
 */
public class DraggableImplTest
{
    private ComponentContext newContext()
    {
        return mock(ComponentContext.class);
    }

    @Test
    public void testBareRenderHasNoDragIdAndNoContent()
    {
        DraggableImpl draggable = new DraggableImpl(this.newContext());

        UIComponentRenderer renderer = draggable.asRenderer();
        Location location = mock(Location.class);
        Node node = renderer.render(mock(RenderingProcessor.class), location, Optional.empty());

        assertEquals("DRAGGABLE", node.getType());
        assertNull(((DraggableNode) node).getDragId());
        assertNull(((DraggableNode) node).getContent());
    }

    @Test
    public void testWithDragIdIsReflectedOnTheRenderedNode()
    {
        DraggableImpl draggable = new DraggableImpl(this.newContext());
        draggable.withDragId("card-42");

        UIComponentRenderer renderer = draggable.asRenderer();
        Node node = renderer.render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals("card-42", ((DraggableNode) node).getDragId());
    }

    @Test
    public void testWithContentIsProcessedAndAttachedAsTheRenderedNodesContent()
    {
        DraggableImpl draggable = new DraggableImpl(this.newContext());
        UIComponent<?> content = mock(UIComponent.class);
        draggable.withContent(content);

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        ParagraphNode contentNode = new ParagraphNode();
        org.mockito.Mockito.when(renderingProcessor.process(content, location))
                           .thenReturn(contentNode);

        UIComponentRenderer renderer = draggable.asRenderer();
        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals(contentNode, ((DraggableNode) node).getContent());
    }

    @Test
    public void testGetSubComponentsReturnsTheContentWhenPresentAndIsEmptyWhenAbsent()
    {
        DraggableImpl bareDraggable = new DraggableImpl(this.newContext());
        assertEquals(0, bareDraggable.asRenderer()
                                     .getSubComponents(mock(Location.class))
                                     .count());

        DraggableImpl draggableWithContent = new DraggableImpl(this.newContext());
        UIComponent<?> content = mock(UIComponent.class);
        draggableWithContent.withContent(content);
        List<ParentLocationAndComponent> subComponents = draggableWithContent.asRenderer()
                                                                             .getSubComponents(mock(Location.class))
                                                                             .toList();
        assertEquals(1, subComponents.size());
        assertEquals(content, subComponents.get(0)
                                           .getComponent());
    }

    @Test
    public void testManageEventHandlerRegistersNothingPurelyClientSideInteraction()
    {
        DraggableImpl draggable = new DraggableImpl(this.newContext());
        draggable.withDragId("card-1");

        java.util.concurrent.atomic.AtomicBoolean registered = new java.util.concurrent.atomic.AtomicBoolean(false);
        UIComponentRenderer.EventHandlerRegistrationSupport support = new UIComponentRenderer.EventHandlerRegistrationSupport() {
            @Override
            public UIComponentRenderer.EventHandlerRegistrationSupport register(org.omnaest.react4j.service.internal.handler.domain.EventHandler eventHandler)
            {
                registered.set(true);
                return this;
            }

            @Override
            public UIComponentRenderer.EventHandlerRegistrationSupport register(org.omnaest.react4j.service.internal.handler.domain.DataEventHandler eventHandler)
            {
                registered.set(true);
                return this;
            }

            @Override
            public UIComponentRenderer.EventHandlerRegistrationSupport registerAsRerenderingNode()
            {
                return this;
            }
        };

        draggable.asRenderer()
                 .manageEventHandler(support);

        assertTrue(!registered.get(), "Draggable has no server handler and must register nothing");
    }

    @Test
    public void testTemplatingSurvivesDragIdAndContent()
    {
        DraggableImpl draggable = new DraggableImpl(this.newContext());
        UIComponent<?> content = mock(UIComponent.class);
        draggable.withDragId("card-7")
                 .withContent(content);

        DraggableImpl templated = (DraggableImpl) draggable.asTemplateProvider()
                                                           .get();

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        ParagraphNode contentNode = new ParagraphNode();
        org.mockito.Mockito.when(renderingProcessor.process(content, location))
                           .thenReturn(contentNode);

        Node node = templated.asRenderer()
                             .render(renderingProcessor, location, Optional.empty());

        assertEquals("card-7", ((DraggableNode) node).getDragId());
        assertEquals(contentNode, ((DraggableNode) node).getContent());
    }
}
