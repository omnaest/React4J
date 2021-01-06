package org.omnaest.react4j.domain;

import java.util.function.Consumer;

import org.omnaest.react4j.domain.support.UIContentHolder;

public interface VerticalContentSwitcher extends UIComponent<VerticalContentSwitcher>
{
    public VerticalContentSwitcher addContentEntry(Consumer<VerticalContent> contentConsumer);

    public static interface VerticalContent extends UIContentHolder<VerticalContent>
    {
        public VerticalContent withTitle(String title);

        public VerticalContent withState(State state);

        public static enum State
        {
            DEFAULT, DISABLED, ACTIVE
        }
    }
}