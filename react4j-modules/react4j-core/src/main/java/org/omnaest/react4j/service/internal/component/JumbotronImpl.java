package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Jumbotron;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.JumbotronNode;

public class JumbotronImpl extends AbstractUIComponentWithSubComponents<Jumbotron> implements Jumbotron
{
    private I18nText             title;
    private List<UIComponent<?>> contentLeft  = new ArrayList<>();
    private List<UIComponent<?>> contentRight = new ArrayList<>();

    public JumbotronImpl(ComponentContext context)
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
                return locationSupport.createLocation(JumbotronImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new JumbotronNode().setTitle(JumbotronImpl.this.getTextResolver()
                                                                      .apply(JumbotronImpl.this.title, location))
                                          .setLeft(JumbotronImpl.this.contentLeft.stream()
                                                                                 .map(element -> renderingProcessor.process(element, location))
                                                                                 .collect(Collectors.toList()))
                                          .setRight(JumbotronImpl.this.contentRight.stream()
                                                                                   .map(element -> renderingProcessor.process(element, location))
                                                                                   .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.registerChildrenMapper(JumbotronNode.class, JumbotronNode::getLeft)
                        .registerChildrenMapper(JumbotronNode.class, JumbotronNode::getRight);
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.concat(JumbotronImpl.this.contentLeft.stream(), JumbotronImpl.this.contentRight.stream());
            }

        };
    }

    @Override
    public Jumbotron withTitle(String title)
    {
        this.title = this.toI18nText(title);
        return this;
    }

    @Override
    public Jumbotron addContentLeft(UIComponent<?> component)
    {
        this.contentLeft.add(component);
        component.registerParent(this);
        return this;
    }

    @Override
    public Jumbotron addContentLeft(UIComponentProvider<?> componentProvider)
    {
        return this.addContentLeft(componentProvider.get());
    }

    @Override
    public Jumbotron addContentLeft(UIComponentFactoryFunction factoryConsumer)
    {
        return this.addContentLeft(factoryConsumer.apply(this.getUiComponentFactory()));
    }

    @Override
    public Jumbotron addContentRight(UIComponent<?> component)
    {
        this.contentRight.add(component);
        component.registerParent(this);
        return this;
    }

    @Override
    public Jumbotron addContentRight(UIComponentProvider<?> componentProvider)
    {
        return this.addContentRight(componentProvider.get());
    }

    @Override
    public Jumbotron addContentRight(UIComponentFactoryFunction factoryConsumer)
    {
        return this.addContentRight(factoryConsumer.apply(this.getUiComponentFactory()));
    }

}