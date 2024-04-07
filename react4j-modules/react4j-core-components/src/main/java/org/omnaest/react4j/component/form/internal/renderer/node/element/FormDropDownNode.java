package org.omnaest.react4j.component.form.internal.renderer.node.element;

import java.util.List;

import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class FormDropDownNode
{
    @JsonProperty
    @Singular("addOption")
    private List<DropDownOptionNode> options;

    @JsonProperty
    private boolean multiselect;

    @Data
    @Builder
    public static class DropDownOptionNode
    {
        @JsonProperty
        private String key;

        @JsonProperty
        private I18nTextValue label;

        @JsonProperty
        private boolean disabled;
    }

}