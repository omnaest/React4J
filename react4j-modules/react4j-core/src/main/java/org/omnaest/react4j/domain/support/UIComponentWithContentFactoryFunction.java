package org.omnaest.react4j.domain.support;

import java.util.function.Function;

import org.omnaest.react4j.domain.UIComponentFactory;

public interface UIComponentWithContentFactoryFunction extends Function<UIComponentFactory, UIComponentWithContent<?>>
{

}
