package org.omnaest.react4j.domain;

public interface Heading extends UIComponent
{
    public Heading withText(String text);

    public Heading withLevel(int level);

    public Heading withLevel(Level level);

    public static enum Level
    {
        H1, H2, H3, H4, H5, H6;

        public int level()
        {
            return this.ordinal() + 1;
        }
    }
}