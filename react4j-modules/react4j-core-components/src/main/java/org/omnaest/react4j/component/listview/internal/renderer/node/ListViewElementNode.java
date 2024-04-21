package org.omnaest.react4j.component.listview.internal.renderer.node;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.AbstractNode;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ListViewElementNode extends AbstractNode implements Node
{
    @JsonProperty
    private final String type = "LISTVIEWELEMENT";

    @JsonProperty
    private Node content;
}