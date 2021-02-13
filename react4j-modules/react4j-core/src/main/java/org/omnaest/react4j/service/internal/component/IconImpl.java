package org.omnaest.react4j.service.internal.component;

import java.util.Optional;

import org.omnaest.react4j.domain.Icon;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.IconNode;

public class IconImpl extends AbstractUIComponent<Icon> implements Icon
{
    private StandardIcon value;

    public IconImpl(ComponentContext context)
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
                return new IconNode().setIcon(Optional.ofNullable(IconImpl.this.value)
                                                      .map(StandardIcon::get)
                                                      .orElse(null));
            }
        };
    }

    @Override
    public Icon from(StandardIcon value)
    {
        this.value = value;
        return this;
    }

}