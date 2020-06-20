package org.omnaest.react.internal.nodes;

import java.util.List;

import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JumbotronNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "JUMBOTRON";

    @JsonProperty
    private I18nTextValue title;

    @JsonProperty
    private List<Node> left;

    @JsonProperty
    private List<Node> right;

    @Override
    public String getType()
    {
        return this.type;
    }

    public I18nTextValue getTitle()
    {
        return this.title;
    }

    public JumbotronNode setTitle(I18nTextValue title)
    {
        this.title = title;
        return this;
    }

    public List<Node> getLeft()
    {
        return this.left;
    }

    public JumbotronNode setLeft(List<Node> left)
    {
        this.left = left;
        return this;
    }

    public List<Node> getRight()
    {
        return this.right;
    }

    public JumbotronNode setRight(List<Node> right)
    {
        this.right = right;
        return this;
    }

}
