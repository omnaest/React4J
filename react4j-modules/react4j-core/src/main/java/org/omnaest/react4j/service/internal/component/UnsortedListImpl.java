package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UnsortedList;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.UnsortedListNode;
import org.omnaest.utils.element.bi.BiElement;

public class UnsortedListImpl extends AbstractUIComponent<UnsortedList> implements UnsortedList
{
    private List<BiElement<I18nText, Icon>> texts = new ArrayList<>();

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
                return new UnsortedListNode().setElements(UnsortedListImpl.this.texts.stream()
                                                                                     .map(bi -> new UnsortedListNode.ULElement().setText(UnsortedListImpl.this.getTextResolver()
                                                                                                                                                              .apply(bi.getFirst(),
                                                                                                                                                                     location))
                                                                                                                                .setIcon(Optional.ofNullable(bi.getSecond())
                                                                                                                                                 .map(icon -> icon.get())
                                                                                                                                                 .orElse(null)))
                                                                                     .collect(Collectors.toList()));
            }
        };
    }

    @Override
    public UnsortedList addText(Icon icon, String text)
    {
        this.texts.add(BiElement.of(I18nText.of(this.getLocations(), text, this.getDefaultLocale()), icon));
        return this;
    }

    @Override
    public UnsortedList addText(String text)
    {
        return this.addText(null, text);
    }

}