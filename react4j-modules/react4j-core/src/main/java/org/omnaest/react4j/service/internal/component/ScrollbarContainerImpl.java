package org.omnaest.react4j.service.internal.component;

import java.util.Optional;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.ScrollbarContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.ScrollbarContainerNode;

public class ScrollbarContainerImpl extends AbstractUIComponentAndContentHolder<ScrollbarContainer> implements ScrollbarContainer
{
    private UIComponent<?>    content;
    private VerticalBoxMode   verticalBoxMode   = VerticalBoxMode.DEFAULT_HEIGHT;
    private HorizontalBoxMode horizontalBoxMode = HorizontalBoxMode.DEFAULT_WIDTH;

    public ScrollbarContainerImpl(ComponentContext context)
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
                return new ScrollbarContainerNode().setContent(Optional.ofNullable(ScrollbarContainerImpl.this.content)
                                                                       .map(content -> content.asRenderer()
                                                                                              .render(parentLocation))
                                                                       .orElse(null))
                                                   .setVerticalBoxMode(ScrollbarContainerImpl.this.verticalBoxMode.name()
                                                                                                                  .replaceAll("_", "-")
                                                                                                                  .toLowerCase())
                                                   .setHorizontalBoxMode(ScrollbarContainerImpl.this.horizontalBoxMode.name()
                                                                                                                      .replaceAll("_", "-")
                                                                                                                      .toLowerCase());
            }
        };
    }

    @Override
    public ScrollbarContainer withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public ScrollbarContainer withVerticalBox(VerticalBoxMode verticalBoxMode)
    {
        this.verticalBoxMode = verticalBoxMode;
        return this;
    }

    @Override
    public ScrollbarContainer withHorizontalBox(HorizontalBoxMode horizontalBoxMode)
    {
        this.horizontalBoxMode = horizontalBoxMode;
        return this;
    }

}