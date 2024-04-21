package org.omnaest.react4j.component.listview;

import java.util.function.Consumer;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.source.DataSource;

public interface ListView extends UIComponent<ListView>
{
    public ListView withDataSource(DataSource source);

    public ListView withElement(Consumer<ListViewElement> element);

    public static interface ListViewElement extends UIComponent<ListViewElement>
    {
        public ListViewElement withContent(UIComponent<?> uiComponent);
    }

}