package org.omnaest.react4j.component.treetable.internal.factory;

import org.omnaest.react4j.component.treetable.TreeTable;
import org.omnaest.react4j.component.treetable.internal.TreeTableImpl;
import org.omnaest.react4j.domain.support.CustomUIComponentFactory;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.springframework.stereotype.Component;

@Component
public class TreeTableFactory implements CustomUIComponentFactory<TreeTable>
{
    @Override
    public Class<TreeTable> getType()
    {
        return TreeTable.class;
    }

    @Override
    public TreeTable newInstance(ComponentContext context)
    {
        return new TreeTableImpl(context);
    }
}
