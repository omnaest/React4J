package org.omnaest.react4j.service.internal.component;

import org.omnaest.react4j.domain.Heading;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.HeadingNode;

public class HeadingImpl extends AbstractUIComponent implements Heading
{
    private I18nText text;
    private int      level = 1;

    public HeadingImpl(ComponentContext context)
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
                return new HeadingNode().setText(HeadingImpl.this.getTextResolver()
                                                                 .apply(HeadingImpl.this.text, Location.of(parentLocation, HeadingImpl.this.getId())))
                                        .setLevel(HeadingImpl.this.level);
            }
        };
    }

    @Override
    public Heading withText(String text)
    {
        this.text = I18nText.of(this.getLocations(), text, this.getDefaultLocale());
        return this;
    }

    @Override
    public Heading withLevel(int level)
    {
        this.level = level;
        return this;
    }

    @Override
    public Heading withLevel(Level level)
    {
        return this.withLevel(level.level());
    }

}