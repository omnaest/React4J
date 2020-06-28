package org.omnaest.react4j.domain;

public interface BlockQuote extends UIComponent
{
    public BlockQuote addText(String text);

    public BlockQuote withFooter(String footer);
}