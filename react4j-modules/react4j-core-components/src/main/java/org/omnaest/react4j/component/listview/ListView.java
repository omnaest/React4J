package org.omnaest.react4j.component.listview;

import java.util.function.Consumer;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.source.DataSource;

/**
 * A list that shows its {@link #withElement(Consumer) element} once for each row its {@link #withDataSource(DataSource)
 * data source} delivers.
 * <p>
 * The rows come from the data source at the time the page is shown: the interactive renderer fetches them from the
 * server and repeats the element for each one. The static HTML render ({@code NodeRenderType.HTML}) does not fetch
 * anything, so it renders the element exactly once, whatever the data source holds, and a list view with an empty data
 * source is still rendered with one element there. This is a structural difference of the static render, not a missing
 * field: do not use static output to show the rows of a list view.
 */
public interface ListView extends UIComponent<ListView>
{
    public ListView withDataSource(DataSource source);

    public ListView withElement(Consumer<ListViewElement> element);

    public static interface ListViewElement extends UIComponent<ListViewElement>
    {
        public ListViewElement withContent(UIComponent<?> uiComponent);
    }

}
