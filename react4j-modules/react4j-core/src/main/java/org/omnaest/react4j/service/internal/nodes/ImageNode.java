package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "IMAGE";

    @JsonProperty
    private I18nTextValue name;

    @JsonProperty
    private String image;

    @Override
    public String getType()
    {
        return this.type;
    }

    public I18nTextValue getName()
    {
        return this.name;
    }

    public ImageNode setName(I18nTextValue name)
    {
        this.name = name;
        return this;
    }

    public String getImage()
    {
        return this.image;
    }

    public ImageNode setImage(String image)
    {
        this.image = image;
        return this;
    }

}
