package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
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
            public Node render(Location parentLocation)
            {
                Location location = Location.of(parentLocation, CompositeImpl.this.getId());
                return new CompositeNode().setElements(CompositeImpl.this.components.stream()
                                                                                    .map(component -> component.asRenderer()
                                                                                                               .render(location))
                                                                                    .collect(Collectors.toList()));
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