package org.omnaest.react4j.domain;

import org.omnaest.react4j.domain.support.UIComponentWithContent;

public interface Toaster extends UIComponentWithContent<Toaster>
{
    public Toaster withTitle(String title);
}