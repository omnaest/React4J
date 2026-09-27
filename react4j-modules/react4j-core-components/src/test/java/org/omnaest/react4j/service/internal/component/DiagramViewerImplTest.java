package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.DiagramViewerNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see DiagramViewerImpl
 * @author omnaest
 */
public class DiagramViewerImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaults()
    {
        ComponentContext context = this.newContext();
        DiagramViewerImpl diagramViewer = new DiagramViewerImpl(context);

        UIComponentRenderer renderer = diagramViewer.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("DIAGRAMVIEWER", node.getType());
        assertNull(((DiagramViewerNode) node).getSvg());
        assertEquals(DiagramViewerImpl.DEFAULT_MAX_HEIGHT, ((DiagramViewerNode) node).getMaxHeight());
        assertEquals(DiagramViewerImpl.DEFAULT_WIDTH, ((DiagramViewerNode) node).getWidth());
        assertNull(((DiagramViewerNode) node).getHeight());
        assertTrue(((DiagramViewerNode) node).isInteractive());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        DiagramViewerImpl diagramViewer = new DiagramViewerImpl(context);

        diagramViewer.withSvg("<svg viewBox=\"0 0 10 10\"><circle r=\"1\"/></svg>");
        diagramViewer.withMaxHeight("500px");
        diagramViewer.withWidth("80%");
        diagramViewer.withHeight("640px");
        diagramViewer.withInteractive(false);

        UIComponentRenderer renderer = diagramViewer.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("DIAGRAMVIEWER", node.getType());
        assertEquals("<svg viewBox=\"0 0 10 10\"><circle r=\"1\"/></svg>", ((DiagramViewerNode) node).getSvg());
        assertEquals("500px", ((DiagramViewerNode) node).getMaxHeight());
        assertEquals("80%", ((DiagramViewerNode) node).getWidth());
        assertEquals("640px", ((DiagramViewerNode) node).getHeight());
        assertFalse(((DiagramViewerNode) node).isInteractive());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        DiagramViewerImpl diagramViewer = new DiagramViewerImpl(context);
        diagramViewer.withSvg("<svg viewBox=\"0 0 20 20\"><rect/></svg>");
        diagramViewer.withMaxHeight("300px");
        diagramViewer.withWidth("50%");
        diagramViewer.withHeight("720px");
        diagramViewer.withInteractive(false);

        DiagramViewerImpl templated = (DiagramViewerImpl) diagramViewer.asTemplateProvider()
                                                                       .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("<svg viewBox=\"0 0 20 20\"><rect/></svg>", ((DiagramViewerNode) node).getSvg());
        assertEquals("300px", ((DiagramViewerNode) node).getMaxHeight());
        assertEquals("50%", ((DiagramViewerNode) node).getWidth());
        assertEquals("720px", ((DiagramViewerNode) node).getHeight());
        assertFalse(((DiagramViewerNode) node).isInteractive());
    }

    @Test
    public void testNoRegisteredEventHandler()
    {
        ComponentContext context = this.newContext();
        DiagramViewerImpl diagramViewer = new DiagramViewerImpl(context);

        UIComponentRenderer.EventHandlerRegistrationSupport support = mock(UIComponentRenderer.EventHandlerRegistrationSupport.class);
        diagramViewer.asRenderer()
                     .manageEventHandler(support);

        org.mockito.Mockito.verifyNoInteractions(support);
    }

    @Test
    public void testNoSubComponents()
    {
        ComponentContext context = this.newContext();
        DiagramViewerImpl diagramViewer = new DiagramViewerImpl(context);

        assertEquals(0, diagramViewer.asRenderer()
                                     .getSubComponents(mock(Location.class))
                                     .count());
    }
}
