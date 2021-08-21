package org.omnaest.react4j.service.internal.rerenderer;

import java.util.Optional;

import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.handler.domain.TargetNode;
import org.omnaest.react4j.service.internal.rerenderer.RerenderingNodeProviderRegistry.RerenderedNodeProvider;

/**
 * @see RerenderingNodeProviderRegistry
 * @author omnaest
 */
public interface RerenderingService
{
    /**
     * Rerenders a {@link Target} node if it registered a {@link RerenderedNodeProvider} at the {@link RerenderingNodeProviderRegistry}
     * 
     * @param target
     * @param data
     * @return
     */
    public Optional<TargetNode> rerenderTargetNode(Target target, Optional<Data> data);
}
