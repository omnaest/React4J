package org.omnaest.react4j.service.internal.component;

import org.omnaest.react4j.domain.AnkerButton;
import org.omnaest.react4j.domain.Button.Style;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.AnkerButtonNode;

public class AnkerButtonImpl extends AbstractUIComponent<AnkerButton> implements AnkerButton
{
    private I18nText text;
    private String   link;
    private Style    style;

    public AnkerButtonImpl(ComponentContext context)
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
                return new AnkerButtonNode().setText(AnkerButtonImpl.this.getTextResolver()
                                                                         .apply(AnkerButtonImpl.this.text,
                                                                                Location.of(parentLocation, AnkerButtonImpl.this.getId())))
                                            .setLink(AnkerButtonImpl.this.link)
                                            .setStyle(AnkerButtonImpl.this.style.name()
                                                                                .toLowerCase());
            }
        };
    }

    @Override
    public AnkerButton withText(String text)
    {
        this.text = I18nText.of(this.getLocations(), text, this.getDefaultLocale());
        return this;
    }

    @Override
    public AnkerButton withLink(String link)
    {
        this.link = link;
        return this;
    }

    @Override
    public AnkerButton withStyle(Style style)
    {
        this.style = style;
        return this;
    }
}