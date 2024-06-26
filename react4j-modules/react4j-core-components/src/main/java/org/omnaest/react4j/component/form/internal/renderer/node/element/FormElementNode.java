package org.omnaest.react4j.component.form.internal.renderer.node.element;

import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class FormElementNode
{
    @JsonProperty
    private String field;

    @JsonProperty
    private String contextId;

    @JsonProperty
    private String type;

    @JsonProperty
    private I18nTextValue label;

    @JsonProperty
    private I18nTextValue description;

    @JsonProperty
    private boolean disabled;

    @JsonProperty
    private boolean readonly;

    @JsonProperty
    private String colspan;

    @JsonProperty
    private FormRangeNode range;

    @JsonProperty
    private FormDropDownNode dropDown;

    @JsonProperty
    private FormButtonNode button;

    @JsonProperty
    private FormCheckboxNode checkbox;

    @JsonProperty
    private FormInputNode input;

    @Data
    @Builder
    public static class FormCheckboxNode
    {
        @JsonProperty
        private String checkboxType;
    }

    @Data
    @Builder
    public static class FormInputNode
    {
        @JsonProperty
        private I18nTextValue placeholder;

        @JsonProperty
        private String type;
    }

    public String getType()
    {
        return this.type;
    }

    public FormRangeNode getRange()
    {
        return this.range;
    }

    public FormElementNode setRange(FormRangeNode range)
    {
        this.range = range;
        return this;
    }

    public FormElementNode setType(String type)
    {
        this.type = type;
        return this;
    }

    public I18nTextValue getLabel()
    {
        return this.label;
    }

    public FormElementNode setLabel(I18nTextValue label)
    {
        this.label = label;
        return this;
    }

    public String getField()
    {
        return this.field;
    }

    public FormElementNode setField(String field)
    {
        this.field = field;
        return this;
    }

    public I18nTextValue getDescription()
    {
        return this.description;
    }

    public FormElementNode setDescription(I18nTextValue description)
    {
        this.description = description;
        return this;
    }

    public String getContextId()
    {
        return this.contextId;
    }

    public FormElementNode setContextId(String contextId)
    {
        this.contextId = contextId;
        return this;
    }

    public boolean isDisabled()
    {
        return this.disabled;
    }

    public FormElementNode setDisabled(boolean disabled)
    {
        this.disabled = disabled;
        return this;
    }

}