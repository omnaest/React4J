package org.omnaest.react4j.domain.rendering;

import org.omnaest.react4j.domain.UIComponent;

/**
 * Internal extension of the {@link UIComponent} which provides a {@link UIComponentRenderer} instance
 * 
 * @see #asRenderer()
 * @author omnaest
 * @param <UIC>
 */
public interface RenderableUIComponent<UIC extends UIComponent<?>> extends UIComponent<UIC>
{
    public UIComponentRenderer asRenderer();
}
