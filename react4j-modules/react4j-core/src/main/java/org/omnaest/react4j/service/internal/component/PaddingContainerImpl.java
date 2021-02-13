package org.omnaest.react4j.service.internal.component;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.PaddingContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
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
            public Node render(Location parentLocation)
            {
                Location location = Location.of(parentLocation, PaddingContainerImpl.this.getId());
                return new PaddingContainerNode().setHorizontal(PaddingContainerImpl.this.horizontalPadding)
                                                 .setVertical(PaddingContainerImpl.this.verticalPadding)
                                                 .setContent(PaddingContainerImpl.this.content.asRenderer()
                                                                                              .render(location));
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