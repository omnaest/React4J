package org.omnaest.react4j.component.value.internal.node;

import org.omnaest.react4j.component.value.node.ValueNode;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TextFieldNode implements ValueNode
{
    @JsonProperty
    private final String type = "TEXTFIELD";

    @JsonProperty
    private String contextId;

    @JsonProperty
    private String fieldName;
}
