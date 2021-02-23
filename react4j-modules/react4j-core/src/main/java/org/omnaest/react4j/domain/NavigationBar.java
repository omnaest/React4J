package org.omnaest.react4j.domain;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface NavigationBar extends UIComponent<NavigationBar>
{
    public NavigationBar addEntry(Consumer<NavigationBarEntry> navigationEntryConsumer);

    public static interface NavigationBarEntry
    {
        public NavigationBarEntry withText(String text);

        public NavigationBarEntry withLink(String link);

        public NavigationBarEntry withLinkedLocator(String id);

        public NavigationBarEntry withLinked(UIComponent component);

        public NavigationBarEntry withActiveState(boolean active);

        public NavigationBarEntry withDisabledState(boolean disabled);
    }

    public static interface NavigationBarProvider extends Supplier<NavigationBar>
    {
    }

    public static interface NavigationBarConsumer extends Consumer<NavigationBar>
    {
    }
}