package org.omnaest.react.domain;

import org.omnaest.react.domain.support.UIComponentWithContent;

public interface Card extends UIComponentWithContent<Card>
{
    public Card withTitle(String title);

    public Card withLinkLocator(String locator);

    public Card withAdjustment(boolean value);
}