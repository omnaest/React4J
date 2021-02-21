package org.omnaest.react4j.service.internal.component;

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Anker;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.AnkerNode;

public class AnkerImpl extends AbstractUIComponent<Anker> implements Anker
{
    private I18nText text;
    private String   link;

    public AnkerImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(AnkerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new AnkerNode().setText(AnkerImpl.this.getTextResolver()
                                                             .apply(AnkerImpl.this.text, location))
                                      .setLink(AnkerImpl.this.link);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                // TODO Auto-generated method stub
                NodeRenderer<AnkerNode> nodeRenderer = new NodeRenderer<AnkerNode>()
                {
                    @Override
                    public String render(AnkerNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        String text = Optional.ofNullable(node.getText())
                                              .map(nodeRenderingProcessor::render)
                                              .orElse("");
                        return "<a href=\"" + node.getLink() + "\">" + text + "</a>";
                    }

                };
                registry.register(AnkerNode.class, NodeRenderType.HTML, nodeRenderer);
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.empty();
            }

        };
    }

    @Override
    public Anker withText(String text)
    {
        this.text = I18nText.of(this.getLocations(), text, this.getDefaultLocale());
        return this;
    }

    @Override
    public Anker withLink(String link)
    {
        this.link = link;
        return this;
    }
}