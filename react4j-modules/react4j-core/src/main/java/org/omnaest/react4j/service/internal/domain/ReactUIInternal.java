package org.omnaest.react4j.service.internal.domain;

import org.omnaest.react4j.domain.ReactUI;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.service.internal.nodes.NodeHierarchy;

public interface ReactUIInternal extends ReactUI
{
    public NodeHierarchy asNodeHierarchy();

    void collectNodeRenderers(NodeRendererRegistry registry);

}