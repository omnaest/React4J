package org.omnaest.react4j.domain.rendering.components;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;

public interface RenderingProcessor
{
    public Node process(UIComponent<?> component, Location parentLocation);
}
