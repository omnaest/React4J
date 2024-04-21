package org.omnaest.react4j.component.value;

import org.omnaest.react4j.component.value.node.ValueNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService.LocationAwareTextResolver;

public interface ValueSource
{
    public ValueNode asNode(Location location, LocationAwareTextResolver textResolver);
}