package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Icon;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UnsortedList;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.UnsortedListNode;

public class UnsortedListImpl extends AbstractUIComponent<UnsortedList> implements UnsortedList
{
    private List<UIComponent<?>> elements = new ArrayList<>();

    public UnsortedListImpl(ComponentContext context)
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
                Location location = Location.of(parentLocation, UnsortedListImpl.this.getId());
                return new UnsortedListNode().setElements(UnsortedListImpl.this.elements.stream()
                                                                                        .map(component -> component.asRenderer()
                                                                                                                   .render(location))
                                                                                        .collect(Collectors.toList()));
            }
        };
    }

    @Override
    public UnsortedList addText(Icon.StandardIcon icon, String text)
    {
        this.elements.add(this.getUiComponentFactory()
                              .newComposite()
                              .addComponents(Arrays.asList(this.getUiComponentFactory()
                                                               .newIcon()
                                                               .from(icon),
                                                           this.getUiComponentFactory()
                                                               .newText()
                                                               .addText(text))));
        return this;
    }

    @Override
    public UnsortedList addText(String text)
    {
        return this.addText(null, text);
    }

    @Override
    public UnsortedList addEntry(UIComponent<?> component)
    {
        this.elements.add(component);
        return this;
    }

    @Override
    public UnsortedList addEntries(List<UIComponent<?>> components)
    {
        Optional.ofNullable(components)
                .orElse(Collections.emptyList())
                .forEach(this::addEntry);
        return this;
    }

}