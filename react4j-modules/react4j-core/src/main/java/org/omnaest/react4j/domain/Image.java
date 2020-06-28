package org.omnaest.react4j.domain;

public interface Image extends UIComponent<Image>
{
    public Image withName(String name);

    public Image withImage(String imageName);
}