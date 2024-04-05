package org.omnaest.react4j.component.table.internal.factory;

import org.omnaest.react4j.component.table.Table;
import org.omnaest.react4j.component.table.internal.TableImpl;
import org.omnaest.react4j.domain.support.CustomUIComponentFactory;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.springframework.stereotype.Component;

@Component
public class TableFactory implements CustomUIComponentFactory<Table>
{
    @Override
    public Class<Table> getType()
    {
        return Table.class;
    }

    @Override
    public Table newInstance(ComponentContext context)
    {
        return new TableImpl(context);
    }
}
