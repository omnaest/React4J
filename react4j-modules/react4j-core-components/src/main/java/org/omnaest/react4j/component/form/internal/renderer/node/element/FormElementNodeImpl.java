package org.omnaest.react4j.component.form.internal.renderer.node.element;

import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class FormElementNodeImpl implements FormElementNode
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
    private I18nTextValue placeholder;

    @JsonProperty
    private I18nTextValue description;

    @JsonProperty
    private I18nTextValue text;

    @JsonProperty
    private Handler onClick;

    @JsonProperty
    private boolean disabled;

    @JsonProperty
    private FormRangeNode range;

    @JsonProperty
    private FormDropDownNode dropDown;

    public String getType()
    {
        return this.type;
    }

    public FormRangeNode getRange()
    {
        return this.range;
    }

    public FormElementNodeImpl setRange(FormRangeNode range)
    {
        this.range = range;
        return this;
    }

    public FormElementNodeImpl setType(String type)
    {
        this.type = type;
        return this;
    }

    public I18nTextValue getLabel()
    {
        return this.label;
    }

    public FormElementNodeImpl setLabel(I18nTextValue label)
    {
        this.label = label;
        return this;
    }

    public I18nTextValue getPlaceholder()
    {
        return this.placeholder;
    }

    public FormElementNodeImpl setPlaceholder(I18nTextValue placeholder)
    {
        this.placeholder = placeholder;
        return this;
    }

    public String getField()
    {
        return this.field;
    }

    public FormElementNodeImpl setField(String field)
    {
        this.field = field;
        return this;
    }

    public I18nTextValue getDescription()
    {
        return this.description;
    }

    public FormElementNodeImpl setDescription(I18nTextValue description)
    {
        this.description = description;
        return this;
    }

    public FormElementNodeImpl setText(I18nTextValue text)
    {
        this.text = text;
        return this;
    }

    public I18nTextValue getText()
    {
        return this.text;
    }

    public Handler getOnClick()
    {
        return this.onClick;
    }

    public FormElementNodeImpl setOnClick(Handler onClick)
    {
        this.onClick = onClick;
        return this;
    }

    public String getContextId()
    {
        return this.contextId;
    }

    public FormElementNodeImpl setContextId(String contextId)
    {
        this.contextId = contextId;
        return this;
    }

    public boolean isDisabled()
    {
        return this.disabled;
    }

    public FormElementNodeImpl setDisabled(boolean disabled)
    {
        this.disabled = disabled;
        return this;
    }

}