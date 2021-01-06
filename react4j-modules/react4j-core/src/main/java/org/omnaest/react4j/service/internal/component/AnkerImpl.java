package org.omnaest.react4j.service.internal.component;

import org.omnaest.react4j.domain.Anker;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.AnkerNode;

public class AnkerImpl extends AbstractUIComponent<Anker> implements Anker
{
    private I18nText text;
    private String   link;

    public AnkerImpl(ComponentContext context)
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
                return new AnkerNode().setText(AnkerImpl.this.getTextResolver()
                                                             .apply(AnkerImpl.this.text, Location.of(parentLocation, AnkerImpl.this.getId())))
                                      .setLink(AnkerImpl.this.link);
            }
        };
    }

    @Override
    public Anker withText(String text)
    {
        this.text = I18nText.of(this.getLocations(), text, this.getDefaultLocale());
        return this;
    }

    @Override
    public Anker withLink(String link)
    {
        this.link = link;
        return this;
    }
}