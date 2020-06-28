package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NodeHierarchy extends AbstractJSONSerializable
{
    @JsonProperty
    private Node root;

    public NodeHierarchy(Node root)
    {
        super();
        this.root = root;
    }

    protected NodeHierarchy()
    {
        super();
    }

    public Node getRoot()
    {
        return this.root;
    }

}
