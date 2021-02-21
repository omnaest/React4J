package org.omnaest.react4j.domain.rendering.node;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

public interface NodeRenderingProcessor
{
    public String render(Node node);

    public String render(I18nTextValue text);
}