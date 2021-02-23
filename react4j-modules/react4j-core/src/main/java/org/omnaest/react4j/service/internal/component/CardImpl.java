package org.omnaest.react4j.service.internal.component;

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Card;
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
import org.omnaest.react4j.service.internal.nodes.CardNode;
import org.omnaest.utils.template.TemplateUtils;

public class CardImpl extends AbstractUIComponentAndContentHolder<Card> implements Card
{
    private I18nText       title;
    private String         locator;
    private UIComponent<?> content;
    private boolean        adjust = false;

    public CardImpl(ComponentContext context)
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
                return locationSupport.createLocation(CardImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new CardNode().setTitle(CardImpl.this.getTextResolver()
                                                            .apply(CardImpl.this.title, location))
                                     .setLocator(CardImpl.this.locator)
                                     .setAdjust(CardImpl.this.adjust)
                                     .setContent(Optional.ofNullable(CardImpl.this.content)
                                                         .map(content -> renderingProcessor.process(content, location))
                                                         .orElse(null));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(CardNode.class, NodeRenderType.HTML, new NodeRenderer<CardNode>()
                {
                    @Override
                    public String render(CardNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/card.html")
                                            .add("locator", node.getLocator())
                                            .add("title", nodeRenderingProcessor.render(node.getTitle()))
                                            .add("content", Optional.ofNullable(node.getContent())
                                                                    .map(nodeRenderingProcessor::render)
                                                                    .orElse(""))
                                            .build()
                                            .get();
                    }
                });
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.of(CardImpl.this.content);
            }

        };
    }

    @Override
    public Card withTitle(String title)
    {
        this.title = this.toI18nText(title);
        return this;
    }

    @Override
    public Card withLinkLocator(String locator)
    {
        this.locator = locator;
        return this;
    }

    @Override
    public Card withContent(UIComponent<?> component)
    {
        this.content = component;
        component.registerParent(this);
        return this;
    }

    @Override
    public Card withAdjustment(boolean value)
    {
        this.adjust = value;
        return this;
    }

}