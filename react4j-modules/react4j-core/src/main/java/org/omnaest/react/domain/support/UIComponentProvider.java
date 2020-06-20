package org.omnaest.react.domain.support;

import java.util.function.Supplier;

import org.omnaest.react.domain.UIComponent;

/**
 * @see UIComponent
 * @author omnaest
 */
public interface UIComponentProvider<UIC extends UIComponent<?>> extends Supplier<UIComponent<UIC>>
{
}
