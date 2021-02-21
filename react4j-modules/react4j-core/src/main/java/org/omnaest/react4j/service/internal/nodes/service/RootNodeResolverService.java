package org.omnaest.react4j.service.internal.nodes.service;

import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.nodes.NodeHierarchy;

/**
 * Resolves a node
 * 
 * @author omnaest
 */
public interface RootNodeResolverService
{
    public NodeHierarchy resolveNodeHierarchy(String contextPath);

    public NodeHierarchy resolveDefaultNodeHierarchy();

    public String renderDefaultNodeHierarchyAsStatic(NodeRenderType nodeRenderType);
}
