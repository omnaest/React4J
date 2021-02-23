package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Text;
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
import org.omnaest.react4j.service.internal.nodes.TextNode;

public class TextImpl extends AbstractUIComponent<Text> implements Text
{
    private List<I18nText> texts = new ArrayList<>();

    public TextImpl(ComponentContext context)
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
                return locationSupport.createLocation(TextImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new TextNode().setTexts(TextImpl.this.texts.stream()
                                                                  .map(text -> TextImpl.this.getTextResolver()
                                                                                            .apply(text, location))
                                                                  .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                NodeRenderer<TextNode> nodeRenderer = new NodeRenderer<TextNode>()
                {
                    @Override
                    public String render(TextNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return node.getTexts()
                                   .stream()
                                   .map(text -> nodeRenderingProcessor.render(text))
                                   .collect(Collectors.joining(" "));
                    }

                };
                registry.register(TextNode.class, NodeRenderType.HTML, nodeRenderer);
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.empty();
            }

        };
    }

    @Override
    public Text addText(String text)
    {
        this.addText(this.toI18nText(text));
        return this;
    }

    @Override
    public Text addText(I18nText text)
    {
        this.texts.add(text);
        return this;
    }

}