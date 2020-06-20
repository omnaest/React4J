package org.omnaest.react.internal.component;

import java.util.Optional;

import org.omnaest.react.domain.Card;
import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.UIComponent;
import org.omnaest.react.domain.i18n.I18nText;
import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.domain.raw.UIComponentRenderer;
import org.omnaest.react.internal.nodes.CardNode;

public class CardImpl extends AbstractUIComponentAndContentHolder<Card> implements Card
{
    private I18nText       title;
    private String         locator;
    private UIComponent<?> content;
    private boolean        adjust = false;

    public CardImpl(ComponentContext context)
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
                Location location = Location.of(parentLocation, CardImpl.this.getId());
                return new CardNode().setTitle(CardImpl.this.getTextResolver()
                                                            .apply(CardImpl.this.title, location))
                                     .setLocator(CardImpl.this.locator)
                                     .setAdjust(CardImpl.this.adjust)
                                     .setContent(Optional.ofNullable(CardImpl.this.content)
                                                         .map(content -> content.asRenderer()
                                                                                .render(location))
                                                         .orElse(null));
            }
        };
    }

    @Override
    public Card withTitle(String title)
    {
        this.title = this.toI18nText(title);
        return this;
    }

    @Override
    public Card withLinkLocator(String locator)
    {
        this.locator = locator;
        return this;
    }

    @Override
    public Card withContent(UIComponent<?> component)
    {
        this.content = component;
        component.registerParent(this);
        return this;
    }

    @Override
    public Card withAdjustment(boolean value)
    {
        this.adjust = value;
        return this;
    }

}