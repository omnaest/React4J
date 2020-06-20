package org.omnaest.react.internal.nodes;

import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.internal.nodes.handler.Handler;
import org.omnaest.react.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ButtonNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "BUTTON";

    @JsonProperty
    private I18nTextValue name;

    @JsonProperty
    private String style;

    @JsonProperty
    private Handler onClick;

    @Override
    public String getType()
    {
        return this.type;
    }

    public Handler getOnClick()
    {
        return this.onClick;
    }

    public ButtonNode setOnClick(Handler onClick)
    {
        this.onClick = onClick;
        return this;
    }

    public I18nTextValue getName()
    {
        return this.name;
    }

    public ButtonNode setName(I18nTextValue i18nTextValue)
    {
        this.name = i18nTextValue;
        return this;
    }

    public String getStyle()
    {
        return this.style;
    }

    public ButtonNode setStyle(String style)
    {
        this.style = style;
        return this;
    }

}
