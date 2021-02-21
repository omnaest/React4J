package org.omnaest.react4j.service.internal.component;

import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.PaddingContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.service.internal.nodes.PaddingContainerNode;

public class PaddingContainerImpl extends AbstractUIComponentAndContentHolder<PaddingContainer> implements PaddingContainer
{
    private boolean        verticalPadding   = true;
    private boolean        horizontalPadding = true;
    private UIComponent<?> content;

    public PaddingContainerImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(PaddingContainerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new PaddingContainerNode().setHorizontal(PaddingContainerImpl.this.horizontalPadding)
                                                 .setVertical(PaddingContainerImpl.this.verticalPadding)
                                                 .setContent(renderingProcessor.process(PaddingContainerImpl.this.content, location));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                // TODO Auto-generated method stub
                registry.registerChildMapper(PaddingContainerNode.class, PaddingContainerNode::getContent);
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.of(PaddingContainerImpl.this.content);
            }

        };
    }

    @Override
    public PaddingContainer withVerticalPadding(boolean verticalPadding)
    {
        this.verticalPadding = verticalPadding;
        return this;
    }

    @Override
    public PaddingContainer withHorizontalPadding(boolean horizontalPadding)
    {
        this.horizontalPadding = horizontalPadding;
        return this;
    }

    @Override
    public PaddingContainer withNoHorizontalPadding()
    {
        return this.withHorizontalPadding(false);
    }

    @Override
    public PaddingContainer withNoVerticalPadding()
    {
        return this.withVerticalPadding(false);
    }

    @Override
    public PaddingContainer withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

}