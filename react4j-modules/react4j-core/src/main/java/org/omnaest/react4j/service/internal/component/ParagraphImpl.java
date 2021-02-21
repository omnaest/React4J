package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Anker;
import org.omnaest.react4j.domain.AnkerButton;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.Icon.StandardIcon;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Paragraph;
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
import org.omnaest.react4j.service.internal.nodes.ParagraphNode;
import org.omnaest.utils.ClassUtils;
import org.omnaest.utils.ClassUtils.Resource;
import org.omnaest.utils.StringUtils;

public class ParagraphImpl extends AbstractUIComponent<Paragraph> implements Paragraph
{
    private List<UIComponent<?>> elements = new ArrayList<>();
    private boolean              bold     = false;

    public ParagraphImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public Paragraph addText(I18nText text)
    {
        return this.addText(null, text);
    }

    @Override
    public Paragraph addText(StandardIcon icon, I18nText text)
    {
        Composite composite = this.getUiComponentFactory()
                                  .newComposite();
        if (icon != null)
        {
            composite.addComponent(this.getUiComponentFactory()
                                       .newIcon()
                                       .from(icon));
        }
        if (text != null)
        {
            composite.addComponent(this.getUiComponentFactory()
                                       .newText()
                                       .addText(text));
        }
        this.elements.add(composite);
        return this;
    }

    @Override
    public Paragraph addHeading(String text, int level)
    {
        this.elements.add(this.getUiComponentFactory()
                              .newHeading()
                              .withLevel(level)
                              .withText(text));
        return this;
    }

    @Override
    public Paragraph addLineBreak()
    {
        this.elements.add(this.getUiComponentFactory()
                              .newLineBreak());
        return this;
    }

    @Override
    public Paragraph withBoldStyle()
    {
        return this.withBoldStyle(true);
    }

    @Override
    public Paragraph withBoldStyle(boolean bold)
    {
        this.bold = bold;
        return this;
    }

    @Override
    public Paragraph addTextsByClasspathResource(String resourcePath)
    {
        StringUtils.splitToStreamByLineSeparator(ClassUtils.loadResource(this, resourcePath)
                                                           .map(Resource::asString)
                                                           .orElseThrow(() -> new IllegalStateException("Resource could not be loaded: " + resourcePath)))
                   .forEach(this::addText);
        return this;
    }

    public Paragraph addTexts(String... texts)
    {
        return this.addTexts(Arrays.asList(texts));
    }

    public Paragraph addTexts(List<String> texts)
    {
        Optional.ofNullable(texts)
                .orElse(Collections.emptyList())
                .forEach(this::addText);
        return this;
    }

    @Override
    public Paragraph addText(String text)
    {
        return this.addText(this.toI18nText(text));
    }

    @Override
    public Paragraph addText(StandardIcon icon, String text)
    {
        return this.addText(icon, this.toI18nText(text));
    }

    @Override
    public Paragraph addLink(Consumer<Anker> ankerConsumer)
    {
        Anker anker = this.getUiComponentFactory()
                          .newAnker();
        ankerConsumer.accept(anker);
        this.elements.add(anker);
        return this;
    }

    @Override
    public Paragraph addLinkButton(Consumer<AnkerButton> ankerButtonConsumer)
    {
        AnkerButton anker = this.getUiComponentFactory()
                                .newAnkerButton();
        ankerButtonConsumer.accept(anker);
        this.elements.add(anker);
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(ParagraphImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new ParagraphNode().setId(ParagraphImpl.this.getId())
                                          .setBold(ParagraphImpl.this.bold)
                                          .setElements(ParagraphImpl.this.elements.stream()
                                                                                  .map(element -> renderingProcessor.process(element, location))
                                                                                  .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                NodeRenderer<ParagraphNode> nodeRenderer = new NodeRenderer<ParagraphNode>()
                {
                    @Override
                    public String render(ParagraphNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        String body = node.getElements()
                                          .stream()
                                          .map(nodeRenderingProcessor::render)
                                          .collect(Collectors.joining());
                        return "<p>" + body + "</p>";
                    }

                };
                registry.register(ParagraphNode.class, NodeRenderType.HTML, nodeRenderer);
                registry.registerChildrenMapper(ParagraphNode.class, ParagraphNode::getElements);
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return ParagraphImpl.this.elements.stream();
            }

        };
    }
}