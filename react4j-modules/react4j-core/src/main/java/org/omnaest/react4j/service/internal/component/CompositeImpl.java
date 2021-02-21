package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.CompositeNode;

public class CompositeImpl extends AbstractUIComponentWithSubComponents<Composite> implements Composite
{
    private List<UIComponent<?>> components = new ArrayList<>();

    public CompositeImpl(ComponentContext context)
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
                return locationSupport.createLocation(CompositeImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new CompositeNode().setElements(CompositeImpl.this.components.stream()
                                                                                    .map(component -> renderingProcessor.process(component, location))
                                                                                    .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.registerChildrenMapper(CompositeNode.class, CompositeNode::getElements);
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return CompositeImpl.this.components.stream();
            }

        };
    }

    @Override
    public Composite addNewComponent(UIComponentFactoryFunction factoryConsumer)
    {
        return this.addComponent(factoryConsumer.apply(this.getUiComponentFactory()));
    }

    @Override
    public Composite addComponent(UIComponent<?> component)
    {
        this.components.add(component);
        return this;
    }

    @Override
    public Composite addComponent(UIComponentProvider<?> componentProvider)
    {
        return this.addComponent(componentProvider.get());
    }

    @Override
    public Composite addComponents(List<UIComponent<?>> components)
    {
        Optional.ofNullable(components)
                .ifPresent(consumer -> consumer.forEach(this::addComponent));
        return this;
    }
}