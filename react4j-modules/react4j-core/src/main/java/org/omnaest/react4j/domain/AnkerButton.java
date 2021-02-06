package org.omnaest.react4j.domain;

import org.omnaest.react4j.domain.Button.Style;

public interface AnkerButton extends UIComponent<AnkerButton>
{
    public AnkerButton withText(String text);

    public AnkerButton withLink(String link);

    public AnkerButton withStyle(Style style);
}