package org.omnaest.react.domain.support;

import org.omnaest.react.domain.UIComponent;

public interface UIComponentWithContent<UIC extends UIComponent<?>> extends UIComponent<UIC>, UIContentHolder<UIC>
{
}
