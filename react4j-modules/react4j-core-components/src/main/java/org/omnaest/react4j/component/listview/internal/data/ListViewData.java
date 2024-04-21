package org.omnaest.react4j.component.listview.internal.data;

import org.omnaest.react4j.component.listview.ListView.ListViewElement;
import org.omnaest.react4j.domain.context.data.source.DataSource;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ListViewData
{
    private ListViewElement element;
    private DataSource      dataSource;
}