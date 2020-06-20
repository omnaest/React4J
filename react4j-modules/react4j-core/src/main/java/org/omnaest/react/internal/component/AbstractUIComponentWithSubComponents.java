package org.omnaest.react.internal.component;

import org.omnaest.react.domain.UIComponent;

public abstract class AbstractUIComponentWithSubComponents<UIC extends UIComponent<?>> extends AbstractUIComponent<UIC>
{
    public AbstractUIComponentWithSubComponents(ComponentContext context)
    {
        super(context);
    }
}
