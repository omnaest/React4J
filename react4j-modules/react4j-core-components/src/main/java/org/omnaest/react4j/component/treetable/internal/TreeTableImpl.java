package org.omnaest.react4j.component.treetable.internal;

import java.util.Arrays;
import java.util.List;

import org.omnaest.react4j.component.treetable.TreeTable;
import org.omnaest.react4j.component.treetable.internal.data.TreeTableData;
import org.omnaest.react4j.component.treetable.internal.renderer.TreeTableRendererImpl;
import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.component.treetable.provider.TreeTableDataProvider;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.component.AbstractUIComponentWithSubComponents;
import org.omnaest.react4j.service.internal.component.ComponentContext;

public class TreeTableImpl extends AbstractUIComponentWithSubComponents<TreeTable> implements TreeTable
{
    private final TreeTableData.TreeTableDataBuilder data;

    public TreeTableImpl(ComponentContext context)
    {
        this(context, TreeTableData.builder());
    }

    public TreeTableImpl(ComponentContext context, TreeTableData.TreeTableDataBuilder data)
    {
        super(context);
        this.data = data;
    }

    @Override
    public TreeTable withColumns(TreeTableColumn... columns)
    {
        return this.withColumns(Arrays.asList(columns));
    }

    @Override
    public TreeTable withColumns(List<TreeTableColumn> columns)
    {
        this.data.columns(columns);
        return this;
    }

    @Override
    public TreeTable withDataProvider(TreeTableDataProvider dataProvider)
    {
        this.data.dataProvider(dataProvider);
        return this;
    }

    @Override
    public TreeTable withWindowSize(int windowSize)
    {
        this.data.windowSize(windowSize);
        return this;
    }

    @Override
    public TreeTable withFilterEnabled(boolean enabled)
    {
        this.data.filterEnabled(enabled);
        return this;
    }

    @Override
    public TreeTable withSortEnabled(boolean enabled)
    {
        this.data.sortEnabled(enabled);
        return this;
    }

    @Override
    public TreeTable withMultiColumnSortEnabled(boolean enabled)
    {
        this.data.multiColumnSortEnabled(enabled);
        return this;
    }

    @Override
    public TreeTable withFiltersInitiallyVisible(boolean visible)
    {
        this.data.filtersInitiallyVisible(visible);
        return this;
    }

    @Override
    public TreeTable withFlatModeToggleEnabled(boolean enabled)
    {
        this.data.flatModeToggleEnabled(enabled);
        return this;
    }

    @Override
    public TreeTable withInitiallyFlat(boolean flat)
    {
        this.data.initiallyFlat(flat);
        return this;
    }

    @Override
    public TreeTable refresh()
    {
        // See TreeTable.refresh() javadoc for the full contract + documented limits (plan-76 Slice 7, AC6): this
        // component never caches a fetch result, so TreeTableRendererImpl's RerenderingContainer content function
        // already re-invokes TreeTableDataProvider#fetch(...) fresh on every render pass through the SAME seam the
        // interactive re-query uses - there is no cache here for this call to invalidate, so it is a documented
        // no-op that returns this unchanged, kept for the named lifecycle hook and fluent-chain symmetry with the
        // other TreeTable operations.
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new TreeTableRendererImpl(this.data.build(), this.getUiComponentFactory(), this.context);
    }

    @Override
    public UIComponentProvider<TreeTable> asTemplateProvider()
    {
        return () -> new TreeTableImpl(this.context, this.data.build()
                                                              .toBuilder());
    }
}
