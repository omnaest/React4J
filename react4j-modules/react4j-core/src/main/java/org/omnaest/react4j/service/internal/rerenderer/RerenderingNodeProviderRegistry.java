package org.omnaest.react4j.service.internal.rerenderer;

import java.util.Optional;
import java.util.function.Function;

import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.handler.domain.Target;

/**
 * Allows to register {@link RerenderedNodeProvider} instances
 * 
 * @author omnaest
 */
public interface RerenderingNodeProviderRegistry
{
    public void register(Target target, RerenderedNodeProvider rerenderedNodeProvider);

    public static interface RerenderedNodeProvider extends Function<Optional<Data>, Node>
    {
    }
}
