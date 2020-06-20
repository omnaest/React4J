package org.omnaest.react.domain;

import org.omnaest.react.internal.handler.domain.EventHandler;

public interface Button extends UIComponent<Button>
{
    public Button withName(String name);

    public Button withStyle(Style style);

    public Button onClick(EventHandler eventHandler);

    public static enum Style
    {
        PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO, LIGHT, DARK, LINK
    }
}