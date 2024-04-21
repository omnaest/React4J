package org.omnaest.react4j.component.listview;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.document.Document.Field;

public interface ListView extends UIComponent<ListView>
{
    public ListView withSource(DataSource source);

    public static interface DataSource extends BiConsumer<Integer, PageContent>
    {

    }

    public static interface PageContent
    {

        public PageElementBuilder addNewElementAndGet(String key);

        public PageContent addNewElement(String key, Consumer<PageElementBuilder> element);

    }

    public static interface PageElementBuilder
    {

        public PageElementBuilder add(Field field, Object value);

        public PageElementBuilder add(String field, Object value);

    }

    public ListView withElement(Consumer<ListViewElement> element);

    public static interface ListViewElement extends UIComponent<ListViewElement>
    {
        public ListViewElement withContent(UIComponent<?> uiComponent);
    }

}