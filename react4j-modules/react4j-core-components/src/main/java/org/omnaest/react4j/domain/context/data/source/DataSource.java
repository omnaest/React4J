package org.omnaest.react4j.domain.context.data.source;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.omnaest.react4j.domain.context.data.source.DataSource.PageContent;
import org.omnaest.react4j.domain.context.document.Document.Field;

public interface DataSource extends BiConsumer<Integer, PageContent>
{

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
}