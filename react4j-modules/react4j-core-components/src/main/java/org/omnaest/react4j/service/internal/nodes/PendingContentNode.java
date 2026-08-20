package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire form of {@code PendingContent}. The content is rendered by the server as usual; whether it is DISPLAYED is
 * decided by the client, which is the only side that knows a round trip is outstanding.
 */
public class PendingContentNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "PENDINGCONTENT";

    @JsonProperty
    private Node   content;

    @JsonProperty
    private int    appearAfterMillis;

    @Override
    public String getType()
    {
        return this.type;
    }

    public Node getContent()
    {
        return this.content;
    }

    public PendingContentNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public int getAppearAfterMillis()
    {
        return this.appearAfterMillis;
    }

    public PendingContentNode setAppearAfterMillis(int appearAfterMillis)
    {
        this.appearAfterMillis = appearAfterMillis;
        return this;
    }
}
