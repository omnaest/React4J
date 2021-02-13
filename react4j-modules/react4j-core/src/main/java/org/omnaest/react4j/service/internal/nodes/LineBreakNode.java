package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LineBreakNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "LINEBREAK";

    @Override
    public String getType()
    {
        return this.type;
    }

}
