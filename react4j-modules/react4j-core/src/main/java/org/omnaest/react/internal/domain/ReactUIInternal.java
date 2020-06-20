package org.omnaest.react.internal.domain;

import org.omnaest.react.domain.ReactUI;
import org.omnaest.react.internal.nodes.NodeHierarchy;

public interface ReactUIInternal extends ReactUI
{
    public NodeHierarchy asNodeHierarchy();

}