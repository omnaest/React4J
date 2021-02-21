package org.omnaest.react4j.service.internal.component;

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Heading;
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
import org.omnaest.react4j.service.internal.nodes.HeadingNode;

public class HeadingImpl extends AbstractUIComponent implements Heading
{
    private I18nText text;
    private int      level = 1;

    public HeadingImpl(ComponentContext context)
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
                return locationSupport.createLocation(HeadingImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new HeadingNode().setText(HeadingImpl.this.getTextResolver()
                                                                 .apply(HeadingImpl.this.text, location))
                                        .setLevel(HeadingImpl.this.level);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                // TODO Auto-generated method stub
                NodeRenderer<HeadingNode> nodeRenderer = new NodeRenderer<HeadingNode>()
                {
                    @Override
                    public String render(HeadingNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        int level = node.getLevel();
                        String body = Optional.ofNullable(node.getText())
                                              .map(text -> nodeRenderingProcessor.render(text))
                                              .orElse("");
                        return "<h" + level + ">" + body + "</h" + level + ">";
                    }

                };
                registry.register(HeadingNode.class, NodeRenderType.HTML, nodeRenderer);
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.empty();
            }

        };
    }

    @Override
    public Heading withText(String text)
    {
        this.text = I18nText.of(this.getLocations(), text, this.getDefaultLocale());
        return this;
    }

    @Override
    public Heading withLevel(int level)
    {
        this.level = level;
        return this;
    }

    @Override
    public Heading withLevel(Level level)
    {
        return this.withLevel(level.level());
    }

}