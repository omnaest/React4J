package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.CollapseNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see CollapseImpl
 * @author omnaest
 */
public class CollapseImplTest
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
        CollapseImpl collapse = new CollapseImpl(context);

        UIComponentRenderer renderer = collapse.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertFalse(((CollapseNode) node).isInitiallyOpen());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        CollapseImpl collapse = new CollapseImpl(context);

        collapse.withInitiallyOpen(true);

        UIComponentRenderer renderer = collapse.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((CollapseNode) node).isInitiallyOpen());
    }

    @Test
    public void testContentPresence()
    {
        ComponentContext context = this.newContext();
        CollapseImpl collapse = new CollapseImpl(context);

        UIComponent<?> content = mock(UIComponent.class);
        collapse.withContent(content);

        UIComponentRenderer renderer = collapse.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(eq(content), eq(location))).thenReturn(contentNode);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertNotNull(((CollapseNode) node).getContent());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        CollapseImpl collapse = new CollapseImpl(context);
        UIComponent<?> content = mock(UIComponent.class);
        collapse.withContent(content);
        collapse.withInitiallyOpen(true);

        CollapseImpl templated = (CollapseImpl) collapse.asTemplateProvider()
                                                        .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(eq(content), eq(location))).thenReturn(contentNode);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((CollapseNode) node).isInitiallyOpen());
        assertNotNull(((CollapseNode) node).getContent());
    }
}
