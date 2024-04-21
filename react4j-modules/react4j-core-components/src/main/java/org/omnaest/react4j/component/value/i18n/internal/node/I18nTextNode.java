package org.omnaest.react4j.component.value.i18n.internal.node;

import org.omnaest.react4j.component.value.node.ValueNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class I18nTextNode implements ValueNode
{
    @JsonProperty
    private final String type = "I18NTEXT";

    @JsonProperty
    private I18nTextValue value;
}
