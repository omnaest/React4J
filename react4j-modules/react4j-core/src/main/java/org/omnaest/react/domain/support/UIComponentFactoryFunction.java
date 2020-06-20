package org.omnaest.react.domain.support;

import java.util.function.Function;

import org.omnaest.react.domain.UIComponent;
import org.omnaest.react.domain.UIComponentFactory;

public interface UIComponentFactoryFunction extends Function<UIComponentFactory, UIComponent<?>>
{

}
