package org.omnaest.react4j.service.internal.service;

import java.util.List;
import java.util.Map;

import org.omnaest.react4j.domain.context.data.source.DataSource;
import org.omnaest.react4j.service.internal.handler.domain.Target;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

public interface DataService
{
    public void registerDataSource(Target target, DataSource dataSource);

    public DataPage getDataPage(DataPageQuery query);

    @Data
    public static class DataPageQuery
    {
        @JsonProperty
        private Target target;

        @JsonProperty
        private int pageIndex;
    }

    @lombok.Data
    @Builder
    public static class DataPage
    {
        @JsonProperty
        private final List<ElementData> elements;
    }

    @lombok.Data
    @AllArgsConstructor(staticName = "of")
    public static class ElementData
    {
        @JsonProperty
        private Map<String, Object> fieldToValue;
    }

}
