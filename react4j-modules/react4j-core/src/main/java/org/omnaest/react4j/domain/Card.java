package org.omnaest.react4j.domain;

import org.omnaest.react4j.domain.support.UIComponentWithContent;

public interface Card extends UIComponentWithContent<Card>
{
    public Card withTitle(String title);

    public Card withLinkLocator(String locator);

    public Card withAdjustment(boolean value);
}