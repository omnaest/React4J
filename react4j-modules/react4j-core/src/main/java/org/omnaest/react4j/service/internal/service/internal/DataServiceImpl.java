package org.omnaest.react4j.service.internal.service.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.omnaest.react4j.domain.context.data.source.DataSource;
import org.omnaest.react4j.domain.context.data.source.DataSource.PageContent;
import org.omnaest.react4j.domain.context.data.source.DataSource.PageElementBuilder;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.service.DataService;
import org.springframework.stereotype.Service;

@Service
public class DataServiceImpl implements DataService
{
    private Map<Target, DataSource> targetToDataSource = new ConcurrentHashMap<>();

    @Override
    public void registerDataSource(Target target, DataSource dataSource)
    {
        if (target != null && dataSource != null)
        {
            this.targetToDataSource.put(target, dataSource);
        }
    }

    @Override
    public DataPage getDataPage(DataPageQuery query)
    {
        PageContentImpl pageContent = new PageContentImpl();
        Optional.ofNullable(query)
                .map(DataPageQuery::getTarget)
                .map(this.targetToDataSource::get)
                .ifPresent(dataSource -> dataSource.accept(query.getPageIndex(), pageContent));
        return pageContent.build();
    }

    private static class PageContentImpl implements PageContent
    {
        private final List<PageElementBuilderImpl> elementBuilders = new ArrayList<>();

        @Override
        public PageElementBuilder addNewElementAndGet(String key)
        {
            PageElementBuilderImpl builder = new PageElementBuilderImpl();
            this.elementBuilders.add(builder);
            return builder;
        }

        public DataPage build()
        {
            return DataPage.builder()
                           .elements(this.elementBuilders.stream()
                                                         .map(PageElementBuilderImpl::build)
                                                         .toList())
                           .build();
        }

        @Override
        public PageContent addNewElement(String key, Consumer<PageElementBuilder> elementBuilderConsumer)
        {
            if (elementBuilderConsumer != null)
            {
                elementBuilderConsumer.accept(this.addNewElementAndGet(key));
            }
            return this;
        }

        private static class PageElementBuilderImpl implements PageElementBuilder
        {
            private Map<String, Object> fieldToValue = new LinkedHashMap<>();

            @Override
            public PageElementBuilder add(String field, Object value)
            {
                if (field != null)
                {
                    this.fieldToValue.put(field, value);
                }
                return this;
            }

            public ElementData build()
            {
                return ElementData.of(this.fieldToValue);
            }

            @Override
            public PageElementBuilder add(Field field, Object value)
            {
                return this.add(field.getFieldName(), value);
            }
        }
    }

}
