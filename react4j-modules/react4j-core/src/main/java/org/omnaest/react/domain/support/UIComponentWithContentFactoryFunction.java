package org.omnaest.react.domain.support;

import java.util.function.Function;

import org.omnaest.react.domain.UIComponentFactory;

public interface UIComponentWithContentFactoryFunction extends Function<UIComponentFactory, UIComponentWithContent<?>>
{

}
