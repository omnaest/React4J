package org.omnaest.react4j.component.form.internal.renderer.node.element;

import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FormButtonNode
{
    @JsonProperty
    private I18nTextValue text;

    @JsonProperty
    private boolean outline;

    @JsonProperty
    private String variant;

    @JsonProperty
    private String size;

    @JsonProperty
    private Handler onClick;

}