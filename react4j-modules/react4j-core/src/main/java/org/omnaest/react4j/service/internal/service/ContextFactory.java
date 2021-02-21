package org.omnaest.react4j.service.internal.service;

import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.context.data.DefineableDataContext;
import org.omnaest.react4j.domain.context.ui.UIContext;

public interface ContextFactory
{
    public DefineableDataContext newDataContextInstance(Locations locations);

    public UIContext newUIContextInstance(Locations locations);
}
