package org.omnaest.react4j.service.internal.component;

import org.omnaest.react4j.domain.UIComponent;

public abstract class AbstractUIComponentWithSubComponents<UIC extends UIComponent<?>> extends AbstractUIComponent<UIC>
{
    public AbstractUIComponentWithSubComponents(ComponentContext context)
    {
        super(context);
    }
}
