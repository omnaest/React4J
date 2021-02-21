package org.omnaest.react4j.domain.context;

import java.util.Optional;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.ui.UIContext;

/**
 * @see DataContext
 * @see UIContext
 * @author omnaest
 */
public interface Context
{
    public String getId(Location location);

    /**
     * Returns the current {@link Context} as {@link DataContext} if possible. Otherwise a {@link Optional#empty()} is returned.
     * 
     * @return
     */
    public Optional<DataContext> asDataContext();
}
