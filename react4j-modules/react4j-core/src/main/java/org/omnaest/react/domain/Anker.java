package org.omnaest.react.domain;

public interface Anker extends UIComponent<Anker>
{
    public Anker withText(String text);

    public Anker withLink(String link);
}