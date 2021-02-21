package org.omnaest.react4j.domain.rendering.node;

import org.omnaest.react4j.domain.raw.Node;

public interface NodeRenderer<N extends Node>
{
    public String render(N node, NodeRenderingProcessor nodeRenderingProcessor);

}