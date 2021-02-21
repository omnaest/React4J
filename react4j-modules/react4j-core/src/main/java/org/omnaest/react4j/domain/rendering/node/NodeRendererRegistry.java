package org.omnaest.react4j.domain.rendering.node;

import java.util.List;
import java.util.function.Function;

import org.omnaest.react4j.domain.raw.Node;

public interface NodeRendererRegistry
{
    public <N extends Node> NodeRendererRegistry register(Class<N> nodeType, NodeRenderType renderType, NodeRenderer<N> nodeRenderer);

    @Deprecated
    public <N extends Node> NodeRendererRegistry registerChildMapper(Class<N> nodeType, NodeToChildMapper<N> childMapper);

    @Deprecated
    public <N extends Node> NodeRendererRegistry registerChildrenMapper(Class<N> nodeType, NodeToChildrenMapper<N> childrenMapper);

    public static interface NodeToChildMapper<N extends Node> extends Function<N, Node>
    {
    }

    public static interface NodeToChildrenMapper<N extends Node> extends Function<N, List<? extends Node>>
    {
    }
}