package org.omnaest.react.domain;

public interface BlockQuote extends UIComponent
{
    public BlockQuote addText(String text);

    public BlockQuote withFooter(String footer);
}