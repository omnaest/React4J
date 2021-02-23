package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.BlockQuote;
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
import org.omnaest.react4j.service.internal.nodes.BlockQuoteNode;
import org.omnaest.utils.template.TemplateUtils;

public class BlockQuoteImpl extends AbstractUIComponent implements BlockQuote
{
    private List<I18nText> texts = new ArrayList<>();
    private I18nText       footer;

    public BlockQuoteImpl(ComponentContext context)
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
                return locationSupport.createLocation(BlockQuoteImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new BlockQuoteNode().setTexts(BlockQuoteImpl.this.getTextResolver()
                                                                        .apply(BlockQuoteImpl.this.texts, location))
                                           .setFooter(BlockQuoteImpl.this.getTextResolver()
                                                                         .apply(BlockQuoteImpl.this.footer, location));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(BlockQuoteNode.class, NodeRenderType.HTML, new NodeRenderer<BlockQuoteNode>()
                {
                    @Override
                    public String render(BlockQuoteNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/blockquote.html")
                                            .add("footer", nodeRenderingProcessor.render(node.getFooter()))
                                            .add("texts", node.getTexts()
                                                              .stream()
                                                              .map(nodeRenderingProcessor::render)
                                                              .collect(Collectors.toList()))
                                            .build()
                                            .get();
                    }
                });
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.empty();
            }

        };
    }

    @Override
    public BlockQuote addText(String text)
    {
        this.texts.add(this.toI18nText(text));
        return this;
    }

    @Override
    public BlockQuote withFooter(String footer)
    {
        this.footer = this.toI18nText(footer);
        return this;
    }

}