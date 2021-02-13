package org.omnaest.react4j.domain;

import java.util.List;

public interface UnsortedList extends UIComponent<UnsortedList>
{
    public UnsortedList addText(String text);

    public UnsortedList addText(Icon.StandardIcon icon, String text);

    public UnsortedList addEntry(UIComponent<?> component);

    public UnsortedList addEntries(List<UIComponent<?>> components);

}