package org.omnaest.react.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.omnaest.react.domain.Composite;
import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.UIComponent;
import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.domain.raw.UIComponentRenderer;
import org.omnaest.react.domain.support.UIComponentFactoryFunction;
import org.omnaest.react.domain.support.UIComponentProvider;
import org.omnaest.react.internal.nodes.CompositeNode;

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

}