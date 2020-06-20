package org.omnaest.react.internal.nodes.service;

import org.omnaest.react.internal.nodes.NodeHierarchy;

/**
 * Resolves a node
 * 
 * @author omnaest
 */
public interface RootNodeResolverService
{
    public NodeHierarchy resolveNodeHierarchy(String contextPath);

    public NodeHierarchy resolveDefaultNodeHierarchy();
}
