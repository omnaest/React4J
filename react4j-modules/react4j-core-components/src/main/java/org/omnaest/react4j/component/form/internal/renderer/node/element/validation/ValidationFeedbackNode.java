package org.omnaest.react4j.component.form.internal.renderer.node.element.validation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationFeedbackNode
{
    @JsonProperty("valid")
    private boolean valid;

    @JsonProperty
    private List<ValidationMessageNode> messages;
}