package org.omnaest.react4j.domain.support;

import org.omnaest.react4j.domain.UIComponent;

public interface UIComponentWithContent<UIC extends UIComponent<?>> extends UIComponent<UIC>, UIContentHolder<UIC>
{
}
