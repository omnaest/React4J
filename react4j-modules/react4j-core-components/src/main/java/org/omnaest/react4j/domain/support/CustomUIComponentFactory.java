package org.omnaest.react4j.domain.support;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.service.internal.component.ComponentContext;

public interface CustomUIComponentFactory<UC extends UIComponent<UC>>
{
    public Class<UC> getType();

    public UC newInstance(ComponentContext context);
}
