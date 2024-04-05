package org.omnaest.react4j.component.form.internal.renderer.node.element.validation;

import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationMessageNode
{
    @JsonProperty
    private ValidationMessageType type;

    @JsonProperty
    private I18nTextValue text;

    public static enum ValidationMessageType
    {
        VALID, INVALID
    }
}