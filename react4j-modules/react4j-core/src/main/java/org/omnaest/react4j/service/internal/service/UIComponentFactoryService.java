package org.omnaest.react4j.service.internal.service;

import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.i18n.UILocale;

public interface UIComponentFactoryService
{
    public UIComponentFactory newInstanceFor(UILocale locale);
}
