package org.omnaest.react4j.service.internal.component;

import org.omnaest.react4j.domain.LineBreak;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.LineBreakNode;

public class LineBreakImpl extends AbstractUIComponent<LineBreak> implements LineBreak
{

    public LineBreakImpl(ComponentContext context)
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
                return new LineBreakNode();
            }
        };
    }

}