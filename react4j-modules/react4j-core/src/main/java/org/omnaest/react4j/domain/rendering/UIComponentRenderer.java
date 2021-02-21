package org.omnaest.react4j.domain.rendering;

import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;

/**
 * Renderer for a particular {@link UIComponent} which creates a {@link Node} structure
 * 
 * @author omnaest
 */
public interface UIComponentRenderer
{
    /**
     * Provides the current {@link Location}
     * 
     * @see LocationSupport#createLocation(String)
     * @param locationSupport
     * @return
     */
    public Location getLocation(LocationSupport locationSupport);

    /**
     * Renders a {@link Node} based on the {@link UIComponent} data
     * 
     * @param renderingProcessor
     * @param location
     * @return
     */
    public Node render(RenderingProcessor renderingProcessor, Location location);

    /**
     * Allows to specify additional {@link NodeRenderer} instances which allow to process the generated {@link Node} to static content like
     * {@link NodeRenderType#HTML}
     * 
     * @param registry
     */
    public void manageNodeRenderers(NodeRendererRegistry registry);

    /**
     * Returns all contained sub {@link UIComponent}s
     * 
     * @return
     */
    public Stream<UIComponent<?>> getSubComponents();
}